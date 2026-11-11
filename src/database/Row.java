package database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Row implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<Object> values;

    public Row(List<Object> values) {
        if (values == null) {
            throw new IllegalArgumentException("Значения строки не могут быть null");
        }

        this.values = new ArrayList<>(values);
    }

    public Object getValue(int index) {
        return values.get(index);
    }

    public int size() {
        return values.size();
    }
}
