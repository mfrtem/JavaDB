package database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * разбирает и выполняет запросы: SELECT, INSERT, UPDATE, DELETE,
 * CREATE TABLE, DROP TABLE, CREATE INDEX, SHOW TABLES и DESCRIBE
 * если INSERT, UPDATE или DELETE падает на середине, сделанные изменения отменяются
 */
public class QueryParser {
    private final Database database;
    private List<Token> tokens;
    private int position;
    private String lastSearch = "";
    /**
     * создаёт обработчик запросов
     *
     * @param database база, с которой работаем
     */
    public QueryParser(Database database) {
        this.database = database;
    }

    /**
     * выполняет один запрос
     *
     * @param query текст запроса
     * @return результат запроса
     * @throws SqlException если в запросе ошибка или его нельзя выполнить
     */
    public QueryResult execute(String query) throws SqlException {
        tokens = Tokenizer.tokenize(query);
        position = 0;
        if (tokens.isEmpty()) {
            throw new SqlException("Пустой запрос");
        }
        try {
            String command = nextWord();
            switch (command) {
                case "SELECT":
                    return select();
                case "INSERT":
                    return insert();
                case "UPDATE":
                    return update();
                case "DELETE":
                    return delete();
                case "CREATE":
                    return create();
                case "DROP":
                    return drop();
                case "SHOW":
                    expect("TABLES");
                    expectEnd();
                    return showTables();
                case "DESCRIBE":
                    Table table = getTable(nextName());
                    expectEnd();
                    return describe(table);
                default:
                    throw new SqlException("Неизвестная команда: " + command);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new SqlException(e.getMessage());
        }
    }

    private QueryResult select() throws SqlException {
        List<String> names = null;
        if (!accept("*")) {
            names = new ArrayList<>();
            do {
                names.add(nextName());
            } while (accept(","));
        }
        expect("FROM");
        Table table = getTable(nextName());
        Condition where = parseWhere(table);
        expectEnd();

        List<Column> columns = table.getColumns();
        List<Integer> positions = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        if (names == null) {
            for (int i = 0; i < columns.size(); i++) {
                positions.add(i);
                headers.add(columns.get(i).getName());
            }
        } else {
            for (String name : names) {
                positions.add(columnIndex(table, name));
                headers.add(name);
            }
        }
        List<List<Object>> result = new ArrayList<>();
        for (Row row : findRows(table, where)) {
            List<Object> values = new ArrayList<>();
            for (int index : positions) {
                values.add(row.getValue(index));
            }
            result.add(values);
        }
        return new QueryResult(headers, result, "Поиск: " + lastSearch);
    }

    private QueryResult insert() throws SqlException {
        expect("INTO");
        Table table = getTable(nextName());
        List<Column> columns = table.getColumns();
        List<Integer> positions = new ArrayList<>();
        if (accept("(")) {
            do {
                int index = columnIndex(table, nextName());
                if (positions.contains(index)) {
                    throw new SqlException("Столбец " + columns.get(index).getName() + " указан дважды");
                }
                positions.add(index);
            } while (accept(","));
            expect(")");
        } else {
            for (int i = 0; i < columns.size(); i++) {
                positions.add(i);
            }
        }
        expect("VALUES");
        List<List<String>> literalRows = new ArrayList<>();
        do {
            expect("(");
            List<String> literals = new ArrayList<>();
            do {
                literals.add(nextValue());
            } while (accept(","));
            expect(")");
            literalRows.add(literals);
        } while (accept(","));
        expectEnd();

        List<Row> newRows = new ArrayList<>();
        for (List<String> literals : literalRows) {
            if (literals.size() != positions.size()) {
                throw new SqlException("Ожидалось значений: " + positions.size()
                        + ", а указано: " + literals.size());
            }
            List<Object> values = new ArrayList<>(Collections.nCopies(columns.size(), (Object) null));
            for (int i = 0; i < positions.size(); i++) {
                int index = positions.get(i);
                values.set(index, ValueConverter.convert(literals.get(i), columns.get(index)));
            }
            newRows.add(new Row(values));
        }

        List<Row> added = new ArrayList<>();
        try {
            for (Row row : newRows) {
                database.addRow(table.getName(), row);
                added.add(row);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            for (Row row : added) {
                database.deleteRow(table.getName(), row);
            }
            throw new SqlException("Запись " + describeRow(newRows.get(added.size()))
                    + " не добавлена: " + e.getMessage());
        }
        return new QueryResult("Добавлено записей: " + added.size(), true);
    }

    private QueryResult update() throws SqlException {
        Table table = getTable(nextName());
        expect("SET");
        Map<Integer, Object> assignments = new LinkedHashMap<>();
        do {
            String name = nextName();
            int index = columnIndex(table, name);
            if (table.isPrimaryKeyColumn(name)) {
                throw new SqlException("Первичный ключ " + name + " изменять нельзя");
            }
            expect("=");
            assignments.put(index, ValueConverter.convert(nextValue(), table.getColumns().get(index)));
        } while (accept(","));
        Condition where = parseWhere(table);
        expectEnd();

        List<Row> oldRows = findRows(table, where);
        List<Row> newRows = new ArrayList<>();
        try {
            for (Row oldRow : oldRows) {
                List<Object> values = new ArrayList<>();
                for (int i = 0; i < oldRow.size(); i++) {
                    values.add(oldRow.getValue(i));
                }
                for (Map.Entry<Integer, Object> entry : assignments.entrySet()) {
                    values.set(entry.getKey(), entry.getValue());
                }
                Row newRow = new Row(values);
                database.updateRow(table.getName(), oldRow, newRow);
                newRows.add(newRow);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            for (int i = newRows.size() - 1; i >= 0; i--) {
                database.updateRow(table.getName(), newRows.get(i), oldRows.get(i));
            }
            throw new SqlException("Изменения отменены: " + e.getMessage());
        }
        return new QueryResult("Изменено записей: " + newRows.size(), !newRows.isEmpty());
    }

    private QueryResult delete() throws SqlException {
        expect("FROM");
        Table table = getTable(nextName());
        Condition where = parseWhere(table);
        expectEnd();

        List<Row> rows = findRows(table, where);
        List<Row> deleted = new ArrayList<>();
        try {
            for (Row row : rows) {
                database.deleteRow(table.getName(), row);
                deleted.add(row);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            for (Row row : deleted) {
                database.addRow(table.getName(), row);
            }
            throw new SqlException("Удаление отменено: " + e.getMessage());
        }
        return new QueryResult("Удалено записей: " + deleted.size(), !deleted.isEmpty());
    }

    private QueryResult create() throws SqlException {
        if (accept("INDEX")) {
            String indexName = nextName();
            expect("ON");
            String tableName = nextName();
            expect("(");
            String columnName = nextName();
            expect(")");
            expectEnd();
            columnIndex(getTable(tableName), columnName);
            database.createIndex(indexName, tableName, columnName);
            return new QueryResult("Индекс " + indexName + " создан", true);
        }
        expect("TABLE");
        String name = nextName();
        Table table = new Table(name);
        String primaryKey = null;
        List<ForeignKey> foreignKeys = new ArrayList<>();
        expect("(");
        do {
            String columnName = nextName();
            String type = ValueConverter.normalizeType(nextWord());
            if (accept("(")) {
                next();
                expect(")");
            }
            while (true) {
                if (accept("PRIMARY")) {
                    expect("KEY");
                    if (primaryKey != null) {
                        throw new SqlException("В таблице может быть только один первичный ключ");
                    }
                    primaryKey = columnName;
                } else if (accept("REFERENCES")) {
                    String refTable = nextName();
                    expect("(");
                    String refColumn = nextName();
                    expect(")");
                    foreignKeys.add(new ForeignKey(columnName, refTable, refColumn));
                } else {
                    break;
                }
            }
            table.addColumn(new Column(columnName, type));
        } while (accept(","));
        expect(")");
        expectEnd();

        table.setPrimaryKey(primaryKey != null ? primaryKey : table.getColumns().get(0).getName());
        database.addTable(table);
        try {
            for (ForeignKey foreignKey : foreignKeys) {
                database.addForeignKey(name, foreignKey);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            database.removeTable(name);
            throw new SqlException("Таблица не создана: " + e.getMessage());
        }
        return new QueryResult("Таблица " + name + " создана", true);
    }

    private QueryResult drop() throws SqlException {
        expect("TABLE");
        String name = nextName();
        expectEnd();
        database.removeTable(name);
        return new QueryResult("Таблица " + name + " удалена", true);
    }

    private QueryResult showTables() {
        List<List<Object>> rows = new ArrayList<>();
        for (Table table : database.getTables()) {
            List<Object> values = new ArrayList<>();
            values.add(table.getName());
            values.add(table.getColumns().size());
            values.add(table.getRows().size());
            rows.add(values);
        }
        List<String> headers = new ArrayList<>();
        headers.add("table");
        headers.add("columns");
        headers.add("rows");
        return new QueryResult(headers, rows, null);
    }

    private QueryResult describe(Table table) {
        List<List<Object>> rows = new ArrayList<>();
        for (Column column : table.getColumns()) {
            String constraints = table.isPrimaryKeyColumn(column.getName()) ? "PRIMARY KEY" : "";
            for (ForeignKey foreignKey : table.getForeignKeys()) {
                if (foreignKey.getColumnName().equals(column.getName())) {
                    constraints += (constraints.isEmpty() ? "" : " ") + "REFERENCES "
                            + foreignKey.getReferencedTableName() + "(" + foreignKey.getReferencedColumnName() + ")";
                }
            }
            List<Object> values = new ArrayList<>();
            values.add(column.getName());
            values.add(column.getType());
            values.add(constraints);
            rows.add(values);
        }
        List<String> headers = new ArrayList<>();
        headers.add("column");
        headers.add("type");
        headers.add("constraints");
        return new QueryResult(headers, rows, null);
    }

    private List<Row> findRows(Table table, Condition where) {
        if (where == null) {
            lastSearch = "все строки таблицы";
            return table.getRows();
        }
        if (where.getValue() == null) {
            lastSearch = "сравнение с NULL всегда ложно";
            return new ArrayList<>();
        }
        Index index = database.getIndex(table.getName(), where.getColumn());
        if (index != null && !where.getOperator().equals("!=")) {
            lastSearch = "по индексу " + index.getName();
            return index.find(where.getOperator(), where.getValue());
        }
        lastSearch = "полный перебор таблицы";
        List<Row> result = new ArrayList<>();
        for (Row row : table.getRows()) {
            if (where.matches(row)) {
                result.add(row);
            }
        }
        return result;
    }

    private Condition parseWhere(Table table) throws SqlException {
        if (!accept("WHERE")) {
            return null;
        }
        String name = nextName();
        int index = columnIndex(table, name);
        String operator = nextOperator();
        Object value = ValueConverter.convert(nextValue(), table.getColumns().get(index));
        return new Condition(name, index, operator, value);
    }

    private Table getTable(String name) throws SqlException {
        try {
            return database.getTable(name);
        } catch (IllegalArgumentException e) {
            throw new SqlException("Таблица " + name + " не существует");
        }
    }

    private int columnIndex(Table table, String name) throws SqlException {
        int index = table.getColumnIndex(name);
        if (index < 0) {
            throw new SqlException("В таблице " + table.getName() + " нет столбца " + name);
        }
        return index;
    }

    private String describeRow(Row row) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < row.size(); i++) {
            values.add(QueryResult.format(row.getValue(i)));
        }
        return "(" + String.join(", ", values) + ")";
    }

    private String nextOperator() throws SqlException {
        Token token = next();
        if (!token.isQuoted()) {
            switch (token.getText()) {
                case "=":
                case "!=":
                case "<":
                case ">":
                case "<=":
                case ">=":
                    return token.getText();
                case "<>":
                    return "!=";
                default:
                    break;
            }
        }
        throw new SqlException("Ожидался оператор сравнения, а встретилось: " + token);
    }

    private String nextValue() throws SqlException {
        Token token = next();
        if (token.isQuoted()) {
            return token.getText();
        }
        String text = token.getText();
        if (text.equals("(") || text.equals(")") || text.equals(",") || text.equals(";")
                || text.equals("*") || text.equals("=")) {
            throw new SqlException("Ожидалось значение, а встретилось: " + text);
        }
        if (text.equalsIgnoreCase("NULL")) {
            return null;
        }
        return text;
    }

    private String nextName() throws SqlException {
        Token token = next();
        if (token.isQuoted() || !token.getText().matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new SqlException("Некорректное имя: " + token);
        }
        return token.getText().toLowerCase();
    }

    private String nextWord() throws SqlException {
        Token token = next();
        if (token.isQuoted()) {
            throw new SqlException("Ожидалось ключевое слово, а встретилась строка " + token);
        }
        return token.getText().toUpperCase();
    }

    private void expect(String word) throws SqlException {
        if (!accept(word)) {
            String found = position < tokens.size() ? tokens.get(position).toString() : "конец запроса";
            throw new SqlException("Ожидалось " + word + ", а встретилось: " + found);
        }
    }

    private void expectEnd() throws SqlException {
        accept(";");
        if (position < tokens.size()) {
            throw new SqlException("Лишний текст в конце запроса: " + tokens.get(position));
        }
    }

    private boolean accept(String word) {
        if (position < tokens.size() && tokens.get(position).is(word)) {
            position++;
            return true;
        }
        return false;
    }

    private Token next() throws SqlException {
        if (position >= tokens.size()) {
            throw new SqlException("Неожиданный конец запроса");
        }
        return tokens.get(position++);
    }
}
