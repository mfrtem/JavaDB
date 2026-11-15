package database;

import java.io.Serializable;

/**
 * описывает связь между столбцами двух таблиц
 */
public class ForeignKey implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String columnName;
    private final String referencedTableName;
    private final String referencedColumnName;

    /**
     * создаёт связь между столбцами таблиц
     *
     * @param columnName имя столбца в текущей таблице
     * @param referencedTableName имя связанной таблицы
     * @param referencedColumnName имя столбца в связанной таблице
     * @throws IllegalArgumentException если любое из имён пустое или равно null
     */
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

    /**
     * возвращает имя столбца в текущей таблице
     *
     * @return имя столбца
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * возвращает имя связанной таблицы
     *
     * @return имя связанной таблицы
     */
    public String getReferencedTableName() {
        return referencedTableName;
    }

    /**
     * возвращает имя столбца в связанной таблице
     *
     * @return имя столбца
     */
    public String getReferencedColumnName() {
        return referencedColumnName;
    }
}