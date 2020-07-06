package automark.errors;

public class InvalidConfigException extends AutomarkException {
    public InvalidConfigException(String key, String message) {
        super("Invalid config at key " + key + ": " + message);
    }
}
