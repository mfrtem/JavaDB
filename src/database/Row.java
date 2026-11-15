package database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * представляет строку таблицы и хранит значения её ячеек
 */
public class Row implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<Object> values;

    /**
     * создаёт строку и копирует её значения
     *
     * @param values значения ячеек строки
     * @throws IllegalArgumentException если список значений равен null
     */
    public Row(List<Object> values) {
        if (values == null) {
            throw new IllegalArgumentException("Значения строки не могут быть null");
        }

        this.values = new ArrayList<>(values);
    }

    /**
     * возвращает значение ячейки по индексу
     *
     * @param index индекс ячейки
     * @return значение ячейки
     * @throws IndexOutOfBoundsException если индекс выходит за границы списка
     */
    public Object getValue(int index) {
        return values.get(index);
    }

    /**
     * возвращает количество значений в строке
     *
     * @return количество значений
     */
    public int size() {
        return values.size();
    }
}
