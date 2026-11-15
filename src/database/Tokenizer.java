package database;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    private static final String SYMBOLS = "(),;*=<>!";

    private Tokenizer() {
    }

    public static List<Token> tokenize(String query) throws SqlException {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < query.length()) {
            char c = query.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '\'') {
                StringBuilder text = new StringBuilder();
                i++;
                while (true) {
                    if (i >= query.length()) {
                        throw new SqlException("Не закрыта кавычка в строке '" + text);
                    }
                    if (query.charAt(i) == '\'') {
                        if (i + 1 < query.length() && query.charAt(i + 1) == '\'') {
                            text.append('\'');
                            i += 2;
                            continue;
                        }
                        i++;
                        break;
                    }
                    text.append(query.charAt(i));
                    i++;
                }
                tokens.add(new Token(text.toString(), true));
            } else if (SYMBOLS.indexOf(c) >= 0) {
                String two = i + 1 < query.length() ? query.substring(i, i + 2) : "";
                if (two.equals("<=") || two.equals(">=") || two.equals("!=") || two.equals("<>")) {
                    tokens.add(new Token(two, false));
                    i += 2;
                } else {
                    tokens.add(new Token(String.valueOf(c), false));
                    i++;
                }
            } else {
                int start = i;
                while (i < query.length() && !Character.isWhitespace(query.charAt(i))
                        && SYMBOLS.indexOf(query.charAt(i)) < 0 && query.charAt(i) != '\'') {
                    i++;
                }
                tokens.add(new Token(query.substring(start, i), false));
            }
        }
        return tokens;
    }
}
