package tuwien.taskinn.exceptions;

/**
 * Exception for expired tokens. Used to indicate that a token has expired while allowing the transaction to commit (for
 * status persistence).
 */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
