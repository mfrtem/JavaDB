package database;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * переводит значения из текста запроса в типы, которые хранятся в таблице:
 * INTEGER, REAL, STRING, BOOLEAN и DATE
 */
public class ValueConverter {
    private ValueConverter() {
    }

    /**
     * приводит название типа из CREATE TABLE к нашему
     * например, INT превращается в INTEGER, а VARCHAR в STRING
     *
     * @param type название типа из запроса
     * @return INTEGER, REAL, STRING, BOOLEAN или DATE
     * @throws SqlException если такого типа нет
     */
    public static String normalizeType(String type) throws SqlException {
        switch (type.toUpperCase()) {
            case "INT":
            case "INTEGER":
                return "INTEGER";
            case "REAL":
            case "DOUBLE":
            case "FLOAT":
                return "REAL";
            case "STRING":
            case "VARCHAR":
            case "TEXT":
                return "STRING";
            case "BOOLEAN":
            case "BOOL":
                return "BOOLEAN";
            case "DATE":
                return "DATE";
            default:
                throw new SqlException("Неизвестный тип данных: " + type);
        }
    }

    /**
     * переводит значение из текста в тип столбца
     * например, "19" для INTEGER станет числом 19, а '2024-09-01' для DATE станет датой
     *
     * @param text   значение из запроса, null означает NULL
     * @param column столбец, в который пойдёт значение
     * @return значение нужного типа или null
     * @throws SqlException если значение не подходит под тип столбца
     */
    public static Object convert(String text, Column column) throws SqlException {
        if (text == null) {
            return null;
        }
        try {
            switch (column.getType()) {
                case "INTEGER":
                    return Integer.parseInt(text);
                case "REAL":
                    return Double.parseDouble(text);
                case "BOOLEAN":
                    if (text.equalsIgnoreCase("true")) {
                        return Boolean.TRUE;
                    }
                    if (text.equalsIgnoreCase("false")) {
                        return Boolean.FALSE;
                    }
                    break;
                case "DATE":
                    return LocalDate.parse(text);
                default:
                    return text;
            }
        } catch (NumberFormatException | DateTimeParseException e) {
        }
        throw new SqlException("Столбец " + column.getName() + ": значение '" + text
                + "' не подходит для типа " + column.getType() + hint(column.getType()));
    }

    private static String hint(String type) {
        switch (type) {
            case "BOOLEAN":
                return " (нужно TRUE или FALSE)";
            case "DATE":
                return " (нужен формат ГГГГ-ММ-ДД)";
            default:
                return "";
        }
    }
}
