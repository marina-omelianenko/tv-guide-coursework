package ua.course.tvguide.web;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static Map<String, String> parseObject(String json) {
        Parser parser = new Parser(json);
        return parser.parseObject();
    }

    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder result = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            result.append(switch (character) {
                case '"' -> "\\\"";
                case '\\' -> "\\\\";
                case '\n' -> "\\n";
                case '\r' -> "\\r";
                case '\t' -> "\\t";
                default -> character;
            });
        }
        return result.append('"').toString();
    }

    public static String property(String name, String value) {
        return quote(name) + ":" + quote(value);
    }

    public static String property(String name, int value) {
        return quote(name) + ":" + value;
    }

    public static String message(String text) {
        return "{" + property("message", text) + "}";
    }

    private static final class Parser {
        private final String source;
        private int position;

        private Parser(String source) {
            this.source = source == null ? "" : source.trim();
        }

        private Map<String, String> parseObject() {
            Map<String, String> values = new LinkedHashMap<>();
            skipWhitespace();
            expect('{');
            skipWhitespace();

            if (peek() == '}') {
                position++;
                return values;
            }

            while (position < source.length()) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                String value = parseValue();
                values.put(key, value);
                skipWhitespace();

                char next = peek();
                if (next == ',') {
                    position++;
                    skipWhitespace();
                } else if (next == '}') {
                    position++;
                    return values;
                } else {
                    throw new IllegalArgumentException("Неправильний JSON-запит");
                }
            }
            throw new IllegalArgumentException("Неправильний JSON-запит");
        }

        private String parseValue() {
            if (peek() == '"') {
                return parseString();
            }

            int start = position;
            while (position < source.length() && ",}".indexOf(source.charAt(position)) == -1) {
                position++;
            }
            return source.substring(start, position).trim();
        }

        private String parseString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            boolean escaping = false;

            while (position < source.length()) {
                char character = source.charAt(position++);
                if (escaping) {
                    result.append(switch (character) {
                        case '"' -> '"';
                        case '\\' -> '\\';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        default -> character;
                    });
                    escaping = false;
                } else if (character == '\\') {
                    escaping = true;
                } else if (character == '"') {
                    return result.toString();
                } else {
                    result.append(character);
                }
            }
            throw new IllegalArgumentException("Неправильний JSON-запит");
        }

        private void expect(char expected) {
            if (peek() != expected) {
                throw new IllegalArgumentException("Неправильний JSON-запит");
            }
            position++;
        }

        private char peek() {
            if (position >= source.length()) {
                return '\0';
            }
            return source.charAt(position);
        }

        private void skipWhitespace() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
                position++;
            }
        }
    }
}
