package database;

/**
 * условие из WHERE, например age &gt;= 18
 */
public class Condition {
    private final String column;
    private final int columnIndex;
    private final String operator;
    private final Object value;

    /**
     * создаёт условие
     *
     * @param column      имя столбца
     * @param columnIndex номер столбца в таблице
     * @param operator    знак сравнения: =, !=, &lt;, &lt;=, &gt; или &gt;=
     * @param value       с чем сравниваем (уже нужного типа)
     */
    public Condition(String column, int columnIndex, String operator, Object value) {
        this.column = column;
        this.columnIndex = columnIndex;
        this.operator = operator;
        this.value = value;
    }

    /**
     * возвращает имя столбца
     *
     * @return имя столбца
     */
    public String getColumn() {
        return column;
    }

    /**
     * возвращает знак сравнения
     *
     * @return знак сравнения
     */
    public String getOperator() {
        return operator;
    }

    /**
     * возвращает значение, с которым сравниваем
     *
     * @return значение (может быть null)
     */
    public Object getValue() {
        return value;
    }

    /**
     * проверяет, подходит ли строка таблицы под условие
     *
     * @param row строка таблицы
     * @return true, если подходит
     */
    public boolean matches(Row row) {
        return compare(row.getValue(columnIndex), operator, value);
    }

    /**
     * сравнивает два значения одного типа
     * если одно из них NULL, результат всегда false, как в SQL
     *
     * @param left     значение из таблицы
     * @param operator знак сравнения
     * @param right    значение из условия
     * @return результат сравнения
     */
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
