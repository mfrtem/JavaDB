package database;

/**
 * ошибка при выполнении SQL-запроса
 * например, неправильно написан запрос, нет такой таблицы или значение не того типа
 */
public class SqlException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * создаёт ошибку с текстом, который увидит пользователь
     *
     * @param message текст ошибки
     */
    public SqlException(String message) {
        super(message);
    }
}
