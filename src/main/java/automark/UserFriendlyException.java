package automark;

public class UserFriendlyException extends Exception {
    public UserFriendlyException(String message) {
        super(message);
    }

    public UserFriendlyException(String message, Throwable cause) {
        super(message, cause);
    }
}
