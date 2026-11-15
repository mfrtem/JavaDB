package database;

/**
 * одна часть запроса: слово, число, знак или строка в кавычках
 */
public class Token {
    private final String text;
    private final boolean quoted;

    /**
     * создаёт часть запроса
     *
     * @param text   текст (у строки без кавычек)
     * @param quoted true, если это была строка в кавычках
     */
    public Token(String text, boolean quoted) {
        this.text = text;
        this.quoted = quoted;
    }

    /**
     * возвращает текст
     *
     * @return текст
     */
    public String getText() {
        return text;
    }

    /**
     * проверяет, была ли это строка в кавычках
     *
     * @return true, если строка была в кавычках
     */
    public boolean isQuoted() {
        return quoted;
    }

    /**
     * проверяет, совпадает ли текст со словом, большие и маленькие буквы не важны
     * строка в кавычках ни с каким словом не совпадает
     *
     * @param word слово или знак, например SELECT или (
     * @return true, если совпадает
     */
    public boolean is(String word) {
        return !quoted && text.equalsIgnoreCase(word);
    }

    @Override
    public String toString() {
        return quoted ? "'" + text + "'" : text;
    }
}
