package database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Класс Storage реализует механизм сохранения и восстановления 
 * состояния базы данных на жестком диске с использованием сериализации.
 */
public class Storage {
    private final String targetDirectory;
    private static final String DATABASE_FILENAME = "database.ser";

    /**
     * Конструктор инициализирует путь к директории для хранения файлов БД.
     * 
     * @param folder путь к папке хранения
     */
    public Storage(String folder) {
        this.targetDirectory = folder;
    }

    /**
     * Сохраняет текущее состояние базы данных в файл.
     * 
     * @param database объект базы данных для сохранения
     * @throws IOException при ошибках ввода-вывода
     */
    public void save(Database database) throws IOException {
        File dir = new File(targetDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        File file = new File(dir, DATABASE_FILENAME);
        try (FileOutputStream fileOut = new FileOutputStream(file);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(database);
        }
    }

    /**
     * Загружает базу данных из файла на диске.
     * Если файл отсутствует, создает и возвращает пустую базу данных.
     * 
     * @return загруженная или новая база данных
     * @throws IOException при ошибках чтения или десериализации
     */
    public Database load() throws IOException {
        File file = new File(targetDirectory, DATABASE_FILENAME);
        if (!file.exists()) {
            return new Database();
        }

        try (FileInputStream fileIn = new FileInputStream(file);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            Object readObject = in.readObject();
            if (readObject instanceof Database) {
                return (Database) readObject;
            } else {
                throw new IOException("Формат файла не соответствует объекту Database.");
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Не удалось обнаружить класс базы данных при десериализации.", e);
        }
    }
}