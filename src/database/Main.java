package database;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        Database database = new Database();

        Table groups = new Table("groups");
        groups.addColumn(new Column("id", "INTEGER"));
        groups.addColumn(new Column("name", "STRING"));
        groups.setPrimaryKey("id");
        database.addTable(groups);

        Table students = new Table("students");
        students.addColumn(new Column("id", "INTEGER"));
        students.addColumn(new Column("name", "STRING"));
        students.addColumn(new Column("age", "INTEGER"));
        students.addColumn(new Column("group_id", "INTEGER"));
        students.setPrimaryKey("id");
        database.addTable(students);

        database.addForeignKey(
                "students",
                new ForeignKey("group_id", "groups", "id")
        );

        database.addRow(
                "groups",
                new Row(Arrays.asList(1, "ПИ-24-1"))
        );

        database.addRow(
                "students",
                new Row(Arrays.asList(1, "Анна", 19, 1))
        );

        System.out.println("Таблиц в базе: " + database.getTables().size());
        System.out.println(
                "Записей в students: "
                        + database.getTable("students").getRows().size()
        );
    }
}