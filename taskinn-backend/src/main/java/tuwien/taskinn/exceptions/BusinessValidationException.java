package tuwien.taskinn.exceptions;

/*
 * Custom exception for business validation errors. Used to return a 400 Bad Request with a detailed list of validation
 * failures.
 */
public class BusinessValidationException extends RuntimeException {
    private final String field;

    public BusinessValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }

}
