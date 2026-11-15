package database;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class QueryResult {
    private final List<String> headers;
    private final List<List<Object>> rows;
    private final String message;
    private final boolean changed;

    public QueryResult(List<String> headers, List<List<Object>> rows, String message) {
        this.headers = headers;
        this.rows = rows;
        this.message = message;
        this.changed = false;
    }

    public QueryResult(String message, boolean changed) {
        this.headers = null;
        this.rows = new ArrayList<>();
        this.message = message;
        this.changed = changed;
    }

    public boolean isChanged() {
        return changed;
    }

    public int getRowCount() {
        return rows.size();
    }

    public static String format(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Double) {
            return BigDecimal.valueOf((Double) value).toPlainString();
        }
        return value.toString();
    }

    @Override
    public String toString() {
        if (headers == null) {
            return message;
        }
        int[] width = new int[headers.size()];
        for (int j = 0; j < headers.size(); j++) {
            width[j] = headers.get(j).length();
        }
        for (List<Object> row : rows) {
            for (int j = 0; j < row.size(); j++) {
                width[j] = Math.max(width[j], format(row.get(j)).length());
            }
        }
        StringBuilder line = new StringBuilder("+");
        for (int w : width) {
            for (int k = 0; k < w + 2; k++) {
                line.append('-');
            }
            line.append('+');
        }
        StringBuilder sb = new StringBuilder();
        sb.append(line).append('\n');
        appendRow(sb, new ArrayList<Object>(headers), width);
        sb.append(line).append('\n');
        for (List<Object> row : rows) {
            appendRow(sb, row, width);
        }
        sb.append(line).append('\n');
        sb.append("Строк: ").append(rows.size());
        if (message != null) {
            sb.append('\n').append(message);
        }
        return sb.toString();
    }

    private void appendRow(StringBuilder sb, List<Object> values, int[] width) {
        sb.append('|');
        for (int j = 0; j < values.size(); j++) {
            sb.append(' ').append(String.format("%-" + width[j] + "s", format(values.get(j)))).append(" |");
        }
        sb.append('\n');
    }
}
