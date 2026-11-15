package database;

public class Condition {
    private final String column;
    private final int columnIndex;
    private final String operator;
    private final Object value;

    public Condition(String column, int columnIndex, String operator, Object value) {
        this.column = column;
        this.columnIndex = columnIndex;
        this.operator = operator;
        this.value = value;
    }

    public String getColumn() {
        return column;
    }

    public String getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    public boolean matches(Row row) {
        return compare(row.getValue(columnIndex), operator, value);
    }

    @SuppressWarnings("unchecked")
    public static boolean compare(Object left, String operator, Object right) {
        if (left == null || right == null) {
            return false;
        }
        int result = ((Comparable<Object>) left).compareTo(right);
        switch (operator) {
            case "=":
                return result == 0;
            case "!=":
                return result != 0;
            case "<":
                return result < 0;
            case "<=":
                return result <= 0;
            case ">":
                return result > 0;
            case ">=":
                return result >= 0;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return column + " " + operator + " " + value;
    }
}
