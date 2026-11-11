package database;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Table implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final List<Column> columns = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<ForeignKey> foreignKeys = new ArrayList<>();
    private int primaryKeyIndex = -1;

    public Table(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя таблицы не может быть пустым");
        }

        this.name = name.trim();
    }

    public void addColumn(Column column) {
        if (column == null) {
            throw new IllegalArgumentException("Столбец не может быть null");
        }

        if (!rows.isEmpty()) {
            throw new IllegalStateException("Нельзя добавлять столбцы после добавления строк");
        }

        for (Column existing : columns) {
            if (existing.getName().equals(column.getName())) {
                throw new IllegalArgumentException("Такой столбец уже есть");
            }
        }

        columns.add(column);
    }

    void addRow(Row row) {
        if (row == null) {
            throw new IllegalArgumentException("Строка не может быть null");
        }

        if (columns.isEmpty()) {
            throw new IllegalStateException("Сначала добавь столбцы");
        }

        if (row.size() != columns.size()) {
            throw new IllegalArgumentException("Количество значений не совпадает с количеством столбцов");
        }

        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            Object value = row.getValue(i);

            if (!isValueTypeValid(column.getType(), value)) {
                throw new IllegalArgumentException(
                        "Неверный тип значения для столбца: " + column.getName()
                );
            }
        }

        if (primaryKeyIndex >= 0) {
            Object newPrimaryKey = row.getValue(primaryKeyIndex);

            for (Row existingRow : rows) {
                if (existingRow.getValue(primaryKeyIndex).equals(newPrimaryKey)) {
                    throw new IllegalArgumentException("Первичный ключ уже существует");
                }
            }
        }

        rows.add(row);
    }

    private boolean isValueTypeValid(String type, Object value) {
        if (type.equals("INTEGER")) {
            return value instanceof Integer;
        }
        if (type.equals("STRING")) {
            return value instanceof String;
        }
        if (type.equals("REAL")) {
            return value instanceof Double;
        }
        if (type.equals("BOOLEAN")) {
            return value instanceof Boolean;
        }
        if (type.equals("DATE")) {
            return value instanceof LocalDate;
        }

        return false;
    }

    public void setPrimaryKey(String columnName) {
        if (columnName == null || columnName.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя столбца не может быть пустым");
        }
        if (!rows.isEmpty()) {
            throw new IllegalStateException("Первичный ключ нужно задать до добавления строк");
        }
        if (primaryKeyIndex >= 0) {
            throw new IllegalStateException("Первичный ключ уже задан");
        }

        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equals(columnName.trim())) {
                primaryKeyIndex = i;
                return;
            }
        }

        throw new IllegalArgumentException("Столбец не найден: " + columnName);
    }

    void addForeignKey(ForeignKey foreignKey) {
        if (foreignKey == null) {
            throw new IllegalArgumentException("Внешний ключ не может быть null");
        }

        if (!rows.isEmpty()) {
            throw new IllegalStateException("Связь нужно задать до добавления строк");
        }

        boolean columnFound = false;

        for (Column column : columns) {
            if (column.getName().equals(foreignKey.getColumnName())) {
                columnFound = true;
                break;
            }
        }

        if (!columnFound) {
            throw new IllegalArgumentException(
                    "Столбец не найден: " + foreignKey.getColumnName()
            );
        }

        foreignKeys.add(foreignKey);
    }

    public List<ForeignKey> getForeignKeys() {
        return new ArrayList<>(foreignKeys);
    }

    public int getColumnIndex(String columnName) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equals(columnName)) {
                return i;
            }
        }

        return -1;
    }

    public boolean isPrimaryKeyColumn(String columnName) {
        return primaryKeyIndex >= 0
                && columns.get(primaryKeyIndex).getName().equals(columnName);
    }

    public String getName() {
        return name;
    }

    public List<Column> getColumns() {
        return new ArrayList<>(columns);
    }

    public List<Row> getRows() {
        return new ArrayList<>(rows);
    }
}
