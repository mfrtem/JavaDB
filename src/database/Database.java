package database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * управляет таблицами, записями, связями и индексами базы данных
 */
public class Database implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<Table> tables = new ArrayList<>();

    private Map<String, Index> dbIndices = new HashMap<>();

    /**
     * добавляет таблицу в базу данных
     *
     * @param table добавляемая таблица
     * @throws IllegalArgumentException если таблица равна null или уже существует
     */
    public void addTable(Table table) {
        if (table == null) {
            throw new IllegalArgumentException("Таблица не может быть null");
        }

        for (Table existing : tables) {
            if (existing.getName().equals(table.getName())) {
                throw new IllegalArgumentException("Такая таблица уже есть");
            }
        }

        tables.add(table);
    }

    /**
     * находит таблицу по имени
     *
     * @param name имя таблицы
     * @return найденная таблица
     * @throws IllegalArgumentException если имя пустое или таблица не найдена
     */
    public Table getTable(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя таблицы не может быть пустым");
        }

        for (Table table : tables) {
            if (table.getName().equals(name.trim())) {
                return table;
            }
        }

        throw new IllegalArgumentException("Таблица не найдена: " + name);
    }

    /**
     * добавляет связь с первичным ключом другой таблицы
     *
     * @param tableName имя текущей таблицы
     * @param foreignKey добавляемый внешний ключ
     * @throws IllegalArgumentException если таблица, ключ или столбец некорректны
     * @throws IllegalStateException если в таблице уже есть строки
     */
    public void addForeignKey(String tableName, ForeignKey foreignKey) {
        if (foreignKey == null) {
            throw new IllegalArgumentException("Внешний ключ не может быть null");
        }

        Table sourceTable = getTable(tableName);
        Table referencedTable = getTable(foreignKey.getReferencedTableName());

        int sourceColumnIndex =
                sourceTable.getColumnIndex(foreignKey.getColumnName());
        if (sourceColumnIndex < 0) {
            throw new IllegalArgumentException(
                    "Столбец не найден: " + foreignKey.getColumnName()
            );
        }

        int referencedColumnIndex =
                referencedTable.getColumnIndex(foreignKey.getReferencedColumnName());
        if (referencedColumnIndex < 0) {
            throw new IllegalArgumentException(
                    "Связанный столбец не найден: "
                            + foreignKey.getReferencedColumnName()
            );
        }

        if (!referencedTable.isPrimaryKeyColumn(
                foreignKey.getReferencedColumnName())) {
            throw new IllegalArgumentException(
                    "Внешний ключ должен ссылаться на первичный ключ"
            );
        }

        Column sourceColumn =
                sourceTable.getColumns().get(sourceColumnIndex);
        Column referencedColumn =
                referencedTable.getColumns().get(referencedColumnIndex);

        if (!sourceColumn.getType().equals(referencedColumn.getType())) {
            throw new IllegalArgumentException(
                    "Типы связанных столбцов должны совпадать"
            );
        }

        sourceTable.addForeignKey(foreignKey);
    }

    /**
     * добавляет строку в таблицу
     *
     * @param tableName имя таблицы
     * @param row добавляемая строка
     * @throws IllegalArgumentException если строка или её значения некорректны
     * @throws IllegalStateException если в таблице нет столбцов
     */
    public void addRow(String tableName, Row row) {
        Table table = getTable(tableName);

        if (row == null) {
            throw new IllegalArgumentException("Строка не может быть null");
        }
        if (row.size() != table.getColumns().size()) {
            throw new IllegalArgumentException(
                    "Количество значений не совпадает с количеством столбцов"
            );
        }

        checkForeignKeys(table, row);
        table.addRow(row);

        for (Index index : dbIndices.values()) {
            if (index.getTableName().equals(tableName)) {
                int colIndex = table.getColumnIndex(index.getColumnName());
                if (colIndex >= 0) {
                    Object cellValue = row.getValue(colIndex);
                    index.insert(cellValue, row);
                }
            }
        }
    }

    /**
     * заменяет строку в таблице
     *
     * @param tableName имя таблицы
     * @param oldRow заменяемая строка
     * @param newRow новая строка
     * @throws IllegalArgumentException если строка или её значения некорректны
     */
    public void updateRow(String tableName, Row oldRow, Row newRow) {
        Table table = getTable(tableName);

        if (newRow == null) {
            throw new IllegalArgumentException("Строка не может быть null");
        }
        if (newRow.size() != table.getColumns().size()) {
            throw new IllegalArgumentException(
                    "Количество значений не совпадает с количеством столбцов"
            );
        }

        checkForeignKeys(table, newRow);
        table.replaceRow(oldRow, newRow);

        for (Index index : dbIndices.values()) {
            if (index.getTableName().equals(tableName)) {
                int colIndex = table.getColumnIndex(index.getColumnName());
                if (colIndex >= 0) {
                    index.remove(oldRow.getValue(colIndex), oldRow);
                    index.insert(newRow.getValue(colIndex), newRow);
                }
            }
        }
    }

    /**
     * удаляет строку из таблицы
     *
     * @param tableName имя таблицы
     * @param row удаляемая строка
     * @throws IllegalArgumentException если строка не найдена или на неё есть ссылка
     */
    public void deleteRow(String tableName, Row row) {
        Table table = getTable(tableName);
        checkNotReferenced(table, row);
        table.removeRow(row);

        for (Index index : dbIndices.values()) {
            if (index.getTableName().equals(tableName)) {
                int colIndex = table.getColumnIndex(index.getColumnName());
                if (colIndex >= 0) {
                    index.remove(row.getValue(colIndex), row);
                }
            }
        }
    }

    /**
     * удаляет таблицу из базы данных
     *
     * @param name имя таблицы
     * @throws IllegalArgumentException если таблица не найдена или на неё есть ссылка
     */
    public void removeTable(String name) {
        Table table = getTable(name);

        for (Table other : tables) {
            if (other == table) {
                continue;
            }
            for (ForeignKey foreignKey : other.getForeignKeys()) {
                if (foreignKey.getReferencedTableName().equals(table.getName())) {
                    throw new IllegalArgumentException(
                            "Нельзя удалить таблицу " + table.getName()
                                    + ": на неё ссылается таблица " + other.getName()
                    );
                }
            }
        }

        tables.remove(table);
        Iterator<Index> iterator = dbIndices.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getTableName().equals(table.getName())) {
                iterator.remove();
            }
        }
    }

    private void checkForeignKeys(Table table, Row row) {
        for (ForeignKey foreignKey : table.getForeignKeys()) {
            int sourceIndex = table.getColumnIndex(foreignKey.getColumnName());

            Table referencedTable =
                    getTable(foreignKey.getReferencedTableName());
            int referencedIndex = referencedTable.getColumnIndex(
                    foreignKey.getReferencedColumnName()
            );

            Object foreignValue = row.getValue(sourceIndex);
            if (foreignValue == null) {
                continue;
            }
            boolean referenceFound = false;

            for (Row referencedRow : referencedTable.getRows()) {
                if (referencedRow.getValue(referencedIndex).equals(foreignValue)) {
                    referenceFound = true;
                    break;
                }
            }

            if (!referenceFound) {
                throw new IllegalArgumentException(
                        "Связанная запись не найдена для столбца: "
                                + foreignKey.getColumnName()
                );
            }
        }
    }

    private void checkNotReferenced(Table table, Row row) {
        for (Table other : tables) {
            for (ForeignKey foreignKey : other.getForeignKeys()) {
                if (!foreignKey.getReferencedTableName().equals(table.getName())) {
                    continue;
                }
                int referencedIndex =
                        table.getColumnIndex(foreignKey.getReferencedColumnName());
                int sourceIndex = other.getColumnIndex(foreignKey.getColumnName());
                Object value = row.getValue(referencedIndex);

                for (Row otherRow : other.getRows()) {
                    if (value.equals(otherRow.getValue(sourceIndex))) {
                        throw new IllegalArgumentException(
                                "Нельзя удалить запись: на неё ссылается таблица "
                                        + other.getName()
                        );
                    }
                }
            }
        }
    }

    /**
     * возвращает список таблиц базы данных
     *
     * @return копия списка таблиц
     */
    public List<Table> getTables() {
        return new ArrayList<>(tables);
    }

    /**
     * создаёт индекс по таблице и столбцу и добавляет в него существующие строки
     *
     * @param indexName имя индекса
     * @param tableName имя таблицы
     * @param columnName имя столбца
     * @throws IllegalArgumentException если таблица или столбец не найдены
     */
    public void createIndex(String indexName, String tableName, String columnName) {
        Table targetTable = getTable(tableName);
        
        // Узнаем числовой индекс столбца по его имени
        int colIndex = targetTable.getColumnIndex(columnName);
        if (colIndex < 0) {
            throw new IllegalArgumentException("Столбец не найден в таблице: " + columnName);
        }

        Index newIndex = new Index(indexName, tableName, columnName);
        
        // Наполняем индекс данными, используя числовой индекс ячейки в строке
        for (Row row : targetTable.getRows()) {
            Object cellValue = row.getValue(colIndex);
            newIndex.insert(cellValue, row);
        }
        
        dbIndices.put(indexName, newIndex);
    }

    /**
     * находит индекс по имени таблицы и столбца
     *
     * @param tableName имя таблицы
     * @param columnName имя столбца
     * @return найденный индекс или null, если его нет
     */
    public Index getIndex(String tableName, String columnName) {
        for (Index index : dbIndices.values()) {
            if (index.getTableName().equals(tableName) && index.getColumnName().equals(columnName)) {
                return index;
            }
        }
        return null;
    }
}