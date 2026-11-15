package database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Класс Index представляет индекс по конкретному столбцу таблицы
 * для оптимизации операций поиска (SELECT с условиями).
 */
public class Index implements Serializable {
    private static final long serialVersionUID = 1L;

    private String indexName;
    private String targetTableName;
    private String targetColumnName;
    
    private TreeMap<Object, List<Row>> indexStorage;

    /**
     * Создает новый индекс.
     * 
     * @param indexName имя индекса
     * @param tableName имя таблицы
     * @param columnName имя индексируемого столбца
     */
    public Index(String indexName, String tableName, String columnName) {
        this.indexName = indexName;
        this.targetTableName = tableName;
        this.targetColumnName = columnName;
        this.indexStorage = new TreeMap<>();
    }

    /**
     * Возвращает имя индекса.
     */
    public String getName() {
        return indexName;
    }

    /**
     * Возвращает имя таблицы, для которой создан индекс.
     */
    public String getTableName() {
        return targetTableName;
    }

    /**
     * Возвращает имя столбца, по которому построен индекс.
     */
    public String getColumnName() {
        return targetColumnName;
    }

    /**
     * Очищает содержимое индекса.
     */
    public void clear() {
        indexStorage.clear();
    }

    /**
     * Добавляет значение и ссылку на строку в индекс.
     * 
     * @param value значение ячейки
     * @param row ссылка на строку таблицы
     */
    public void insert(Object value, Row row) {
        if (value == null) {
            return;
        }
        
        List<Row> rowsList = indexStorage.get(value);
        if (rowsList == null) {
            rowsList = new ArrayList<>();
            indexStorage.put(value, rowsList);
        }
        rowsList.add(row);
    }

    /**
     * Удаляет ссылку на строку из индекса при изменении или удалении данных.
     * 
     * @param value значение ячейки
     * @param row ссылка на строку
     */
    public void remove(Object value, Row row) {
        if (value == null) {
            return;
        }

        List<Row> rowsList = indexStorage.get(value);
        if (rowsList != null) {
            rowsList.remove(row);
            if (rowsList.isEmpty()) {
                indexStorage.remove(value);
            }
        }
    }

    /**
     * Осуществляет поиск строк по заданному оператору и значению.
     * 
     * @param operator оператор сравнения: "=", "<", "<=", ">", ">="
     * @param value целевое значение для сравнения
     * @return список найденных строк, соответствующих исходным объектам в таблице
     */
    public List<Row> find(String operator, Object value) {
        List<Row> resultRows = new ArrayList<>();
        if (value == null || indexStorage.isEmpty()) {
            return resultRows;
        }

        switch (operator) {
            case "=":
                List<Row> exactMatch = indexStorage.get(value);
                if (exactMatch != null) {
                    resultRows.addAll(exactMatch);
                }
                break;
                
            case "<":
                for (Map.Entry<Object, List<Row>> entry : indexStorage.headMap(value, false).entrySet()) {
                    resultRows.addAll(entry.getValue());
                }
                break;
                
            case "<=":
                for (Map.Entry<Object, List<Row>> entry : indexStorage.headMap(value, true).entrySet()) {
                    resultRows.addAll(entry.getValue());
                }
                break;
                
            case ">":
                for (Map.Entry<Object, List<Row>> entry : indexStorage.tailMap(value, false).entrySet()) {
                    resultRows.addAll(entry.getValue());
                }
                break;
                
            case ">=":
                for (Map.Entry<Object, List<Row>> entry : indexStorage.tailMap(value, true).entrySet()) {
                    resultRows.addAll(entry.getValue());
                }
                break;
                
            default:
                throw new IllegalArgumentException("Неподдерживаемый оператор в индексе: " + operator);
        }

        return resultRows;
    }
}