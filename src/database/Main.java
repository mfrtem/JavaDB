package database;

import java.io.IOException;
import java.util.Scanner;
/**
 * консольная программа для учёта студентов и групп
 * загружает базу из папки data, читает запросы и сохраняет изменения
 */
public class Main {
    private static final String DATA_FOLDER = "data";

    private Main() {
    }
    /**
     * запуск программы, запрос можно писать в несколько строк,
     * он выполнится, когда строка закончится на ;
     *
     * @param args не используются
     */
    public static void main(String[] args) {
        Storage storage = new Storage(DATA_FOLDER);
        Database database;
        try {
            database = storage.load();
        } catch (IOException e) {
            System.out.println("Не удалось загрузить базу данных: " + e.getMessage());
            return;
        }
        QueryParser parser = new QueryParser(database);
        System.out.println("Учёт студентов и учебных групп. Таблиц загружено: " + database.getTables().size());
        if (database.getTables().isEmpty()) {
            createDemo(parser);
            save(storage, database);
        }
        System.out.println("Введите HELP для справки, EXIT для выхода.");

        Scanner sc = new Scanner(System.in);
        StringBuilder query = new StringBuilder();
        while (true) {
            System.out.print(query.length() == 0 ? "sql> " : "...> ");
            if (!sc.hasNextLine()) {
                break;
            }
            
            String line = sc.nextLine().trim();
            if (query.length() == 0) {
                String command = line.endsWith(";") ? line.substring(0, line.length() - 1).trim() : line;
                if (command.isEmpty()) {
                    continue;
                }
                if (command.equalsIgnoreCase("EXIT") || command.equalsIgnoreCase("QUIT")) {
                    break;
                }
                if (command.equalsIgnoreCase("HELP")) {
                    printHelp();
                    continue;
                }
            }
            query.append(line).append(' ');
            if (line.endsWith(";")) {
                run(parser, storage, database, query.toString());
                query.setLength(0);
            }
        }

        sc.close();
        System.out.println("До свидания!");
    }

    private static void run(QueryParser parser, Storage storage, Database database, String query) {
        long start = System.nanoTime();
        try {
            QueryResult result = parser.execute(query);
            double millis = (System.nanoTime() - start) / 1_000_000.0;
            System.out.println(result);
            System.out.printf("Время выполнения: %.3f мс%n", millis);
            if (result.isChanged()) {
                save(storage, database);
            }
        } catch (SqlException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void save(Storage storage, Database database) {
        try {
            storage.save(database);
        } catch (IOException e) {
            System.out.println("Ошибка сохранения: " + e.getMessage());
        }
    }

    private static void createDemo(QueryParser parser) {
        String[] queries = {
            "CREATE TABLE groups (id INTEGER PRIMARY KEY, name STRING);",
            "CREATE TABLE students (id INTEGER PRIMARY KEY, name STRING, age INTEGER, "
                    + "group_id INTEGER REFERENCES groups(id));",
            "INSERT INTO groups VALUES (1, 'ПИ-24-1'), (2, 'ПМИ-24-2');",
            "INSERT INTO students VALUES (1, 'Анна', 19, 1), (2, 'Борис', 20, 1), (3, 'Вера', 18, 2);"
        };
        for (String query : queries) {
            try {
                parser.execute(query);
            } catch (SqlException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
        System.out.println("База была пустой: созданы таблицы groups и students (students.group_id -> groups.id).");
    }

    private static void printHelp() {
        System.out.println("Команды (запрос заканчивается точкой с запятой):");
        System.out.println("  CREATE TABLE t (col TYPE [PRIMARY KEY] [REFERENCES t2(col2)], ...);");
        System.out.println("      типы: INTEGER, REAL, STRING, BOOLEAN, DATE");
        System.out.println("  DROP TABLE t;");
        System.out.println("  CREATE INDEX name ON t (col);");
        System.out.println("  INSERT INTO t [(col, ...)] VALUES (v, ...), (v, ...);");
        System.out.println("  SELECT * | col, ... FROM t [WHERE col op value];");
        System.out.println("  UPDATE t SET col = value, ... [WHERE col op value];");
        System.out.println("  DELETE FROM t [WHERE col op value];");
        System.out.println("  SHOW TABLES;    DESCRIBE t;");
        System.out.println("Операторы: = != <> < > <= >=");
        System.out.println("Строки и даты в кавычках: 'Анна', '2024-09-01'. Логические: TRUE, FALSE. Пусто: NULL.");
        System.out.println("HELP - справка, EXIT - выход.");
    }
}
