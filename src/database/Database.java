package database;

import java.util.ArrayList;
import java.util.List;

public class Database {
    private final List<Table> tables = new ArrayList<>();

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

        for (ForeignKey foreignKey : table.getForeignKeys()) {
            int sourceIndex = table.getColumnIndex(foreignKey.getColumnName());

            Table referencedTable =
                    getTable(foreignKey.getReferencedTableName());
            int referencedIndex = referencedTable.getColumnIndex(
                    foreignKey.getReferencedColumnName()
            );

            Object foreignValue = row.getValue(sourceIndex);
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

        table.addRow(row);
    }

    public List<Table> getTables() {
        return new ArrayList<>(tables);
    }
}