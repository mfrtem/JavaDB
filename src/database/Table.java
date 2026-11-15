package database;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * хранит столбцы и записи таблицы и проверяет ограничения
 */
public class Table implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final List<Column> columns = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<ForeignKey> foreignKeys = new ArrayList<>();
    private int primaryKeyIndex = -1;

    /**
     * создаёт таблицу с указанным именем
     *
     * @param name имя таблицы
     * @throws IllegalArgumentException если имя пустое или равно null
     */
    public Table(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя таблицы не может быть пустым");
        }

        this.name = name.trim();
    }

    /**
     * добавляет столбец в таблицу
     *
     * @param column добавляемый столбец
     * @throws IllegalArgumentException если столбец равен null или уже существует
     * @throws IllegalStateException если в таблице уже есть строки
     */
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

    /**
     * добавляет строку в таблицу после проверки значений
     *
     * @param row добавляемая строка
     * @throws IllegalArgumentException если строка некорректна
     * @throws IllegalStateException если в таблице ещё нет столбцов
     */
    void addRow(Row row) {
        checkRow(row, null);
        rows.add(row);
    }

    /**
     * заменяет строку после проверки новых значений
     *
     * @param oldRow заменяемая строка
     * @param newRow новая строка
     * @throws IllegalArgumentException если старая строка не найдена или новая некорректна
     */
    void replaceRow(Row oldRow, Row newRow) {
        int position = rows.indexOf(oldRow);
        if (position < 0) {
            throw new IllegalArgumentException("Строка не найдена в таблице " + name);
        }
        checkRow(newRow, oldRow);
        if (primaryKeyIndex >= 0
                && !oldRow.getValue(primaryKeyIndex).equals(newRow.getValue(primaryKeyIndex))) {
            throw new IllegalArgumentException("Первичный ключ изменять нельзя");
        }
        rows.set(position, newRow);
    }

    /**
     * удаляет строку из таблицы
     *
     * @param row удаляемая строка
     * @throws IllegalArgumentException если строка не найдена
     */
    void removeRow(Row row) {
        if (!rows.remove(row)) {
            throw new IllegalArgumentException("Строка не найдена в таблице " + name);
        }
    }

    private void checkRow(Row row, Row ignoredRow) {
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

            if (value == null) {
                if (i == primaryKeyIndex) {
                    throw new IllegalArgumentException(
                            "Первичный ключ не может быть пустым: " + column.getName()
                    );
                }
                continue;
            }

            if (!isValueTypeValid(column.getType(), value)) {
                throw new IllegalArgumentException(
                        "Неверный тип значения для столбца: " + column.getName()
                );
            }
        }

        if (primaryKeyIndex >= 0) {
            Object newPrimaryKey = row.getValue(primaryKeyIndex);

            for (Row existingRow : rows) {
                if (existingRow != ignoredRow
                        && existingRow.getValue(primaryKeyIndex).equals(newPrimaryKey)) {
                    throw new IllegalArgumentException("Первичный ключ уже существует");
                }
            }
        }
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

    /**
     * задаёт первичный ключ таблицы
     *
     * @param columnName имя столбца первичного ключа
     * @throws IllegalArgumentException если имя пустое или столбец не найден
     * @throws IllegalStateException если строки уже добавлены или ключ уже задан
     */
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

    /**
     * добавляет внешний ключ в таблицу
     *
     * @param foreignKey добавляемый внешний ключ
     * @throws IllegalArgumentException если ключ равен null или его столбец не найден
     * @throws IllegalStateException если в таблице уже есть строки
     */
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

    /**
     * возвращает список внешних ключей
     *
     * @return копия списка внешних ключей
     */
    public List<ForeignKey> getForeignKeys() {
        return new ArrayList<>(foreignKeys);
    }

    /**
     * находит индекс столбца по имени
     *
     * @param columnName имя столбца
     * @return индекс столбца или -1, если он не найден
     */
    public int getColumnIndex(String columnName) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equals(columnName)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * проверяет, является ли столбец первичным ключом
     *
     * @param columnName имя столбца
     * @return true, если столбец является первичным ключом
     */
    public boolean isPrimaryKeyColumn(String columnName) {
        return primaryKeyIndex >= 0
                && columns.get(primaryKeyIndex).getName().equals(columnName);
    }

    /**
     * возвращает имя таблицы
     *
     * @return имя таблицы
     */
    public String getName() {
        return name;
    }

    /**
     * возвращает список столбцов таблицы
     *
     * @return копия списка столбцов
     */
    public List<Column> getColumns() {
        return new ArrayList<>(columns);
    }

    /**
     * возвращает строки таблицы
     *
     * @return копия списка строк
     */
    public List<Row> getRows() {
        return new ArrayList<>(rows);
    }
}
