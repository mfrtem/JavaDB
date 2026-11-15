package database;

import java.io.Serializable;

/**
 * описывает столбец таблицы: его имя и тип данных
 */
public class Column implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final String type;

    /**
     * создаёт столбец с указанными именем и типом
     *
     * @param name имя столбца
     * @param type тип данных: INTEGER, STRING, REAL, BOOLEAN или DATE
     * @throws IllegalArgumentException если имя или тип пусты либо тип неизвестен
     */
    public Column(String name, String type) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя столбца не может быть пустым");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Тип столбца не может быть пустым");
        }

        this.name = name.trim();
        String normalizedType = type.trim().toUpperCase();

        if (!normalizedType.equals("INTEGER")
                && !normalizedType.equals("STRING")
                && !normalizedType.equals("REAL")
                && !normalizedType.equals("BOOLEAN")
                && !normalizedType.equals("DATE")) {
            throw new IllegalArgumentException("Неизвестный тип столбца: " + type);
        }

        this.type = normalizedType;
    }

    /**
     * возвращает имя столбца
     *
     * @return имя столбца
     */
    public String getName() {
        return name;
    }

    /**
     * возвращает тип данных столбца
     *
     * @return тип данных столбца
     */
    public String getType() {
        return type;
    }
}
