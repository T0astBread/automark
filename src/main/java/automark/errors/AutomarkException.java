package automark.errors;

public class AutomarkException extends Exception {
    public AutomarkException() {
    }

    public AutomarkException(String message) {
        super(message);
    }

    public AutomarkException(String message, Throwable cause) {
        super(message, cause);
    }
}
