package database;

import java.io.Serializable;

public class ForeignKey implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String columnName;
    private final String referencedTableName;
    private final String referencedColumnName;

    public ForeignKey(
            String columnName,
            String referencedTableName,
            String referencedColumnName
    ) {
        if (columnName == null || columnName.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя столбца не может быть пустым");
        }
        if (referencedTableName == null || referencedTableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя связанной таблицы не может быть пустым");
        }
        if (referencedColumnName == null || referencedColumnName.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя связанного столбца не может быть пустым");
        }

        this.columnName = columnName.trim();
        this.referencedTableName = referencedTableName.trim();
        this.referencedColumnName = referencedColumnName.trim();
    }

    public String getColumnName() {
        return columnName;
    }

    public String getReferencedTableName() {
        return referencedTableName;
    }

    public String getReferencedColumnName() {
        return referencedColumnName;
    }
}