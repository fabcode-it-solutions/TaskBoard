package tuwien.taskinn.exceptions;

/*
 * Custom exception for authentication failures. Used to indicate that user authentication has failed.
 */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
