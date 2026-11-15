package database;

public class SqlException extends Exception {
    private static final long serialVersionUID = 1L;

    public SqlException(String message) {
        super(message);
    }
}
