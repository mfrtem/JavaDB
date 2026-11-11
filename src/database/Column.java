package database;

import java.io.Serializable;

public class Column implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final String type;

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

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
