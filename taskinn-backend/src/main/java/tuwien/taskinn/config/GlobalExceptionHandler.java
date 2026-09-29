package tuwien.taskinn.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.servlet.resource.NoResourceFoundException;
import tuwien.taskinn.dto.response.ErrorResponseDto;
import tuwien.taskinn.exceptions.*;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles exception thrown by @Valid annotation on @RequestBody. This happens when the data in the request body
     * fails validation annotations like @NotBlank, @Email, etc. Returns a 400 Bad Request with a detailed list of
     * validation failures.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ErrorResponseDto.ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ErrorResponseDto.ValidationError(fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more fields have an error.")
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InfeasibleInstanceException.class)
    public ResponseEntity<ErrorResponseDto> handleInfeasibleInstance(InfeasibleInstanceException ex,
            HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Scheduling infeasible")
                .message("Scheduling instance is infeasible")
                .path(request.getRequestURI())
                .validationErrors(
                        List.of(new ErrorResponseDto.ValidationError("instance", "Scheduling instance is infeasible")))
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SolverFailureException.class)
    public ResponseEntity<ErrorResponseDto> handleSolverError(SolverFailureException ex, HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Solver error")
                .message(ex.getMessage() != null ? ex.getMessage() : "Unknown error")
                .path(request.getRequestURI())
                .validationErrors(List.of())
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles exception thrown by @Validated annotation on @RestController. This happens when the data in the request
     * param fails validation annotations like @NotNull, @Email etc. Returns a 400 Bad Request with a detailed list of
     * validation failures.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {

        List<ErrorResponseDto.ValidationError> validationErrors =
                ex.getConstraintViolations().stream().map(violation -> {
                    String fieldName = StreamSupport.stream(violation.getPropertyPath().spliterator(), false)
                            .reduce((first, second) -> second)
                            .map(node -> node.getName())
                            .orElse(violation.getPropertyPath().toString());

                    return new ErrorResponseDto.ValidationError(fieldName, violation.getMessage());
                }).collect(Collectors.toList());

        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more parameters have an error.")
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles exception thrown by @RequestParam annotation. This happens when the request param is missing or
     * malformed. Returns a 400 Bad Request with a detailed list of validation failures.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingParams(MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Missing Parameter")
                .message("Required parameter '" + ex.getParameterName() + "' is missing.")
                .path(request.getRequestURI())
                .validationErrors(
                        List.of(new ErrorResponseDto.ValidationError(ex.getParameterName(), "parameter is required")))
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles custom business validation exceptions. This happens when business logic validation fails (e.g., date
     * ranges, complex rules). Returns a 400 Bad Request with a detailed list of validation failures.
     */
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessValidation(BusinessValidationException ex,
            HttpServletRequest request) {

        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more fields have an error.")
                .path(request.getRequestURI())
                .validationErrors(List.of(new ErrorResponseDto.ValidationError(ex.getField(), ex.getMessage())))
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles type mismatch exceptions (e.g., sending "abc" for an Integer parameter). Returns a 400 Bad Request with
     * details about the invalid parameter.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s", ex.getValue(),
                ex.getName(), expectedType);

        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Parameter Type")
                .message(message)
                .path(request.getRequestURI())
                .validationErrors(
                        List.of(new ErrorResponseDto.ValidationError(ex.getName(), "must be a valid " + expectedType)))
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles cases where the request body is missing or malformed. Returns a 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingRequestBody(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Request body is missing or malformed.")
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles business logic validation errors. Returns a 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException ex,
            HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage()) // Use
                // the
                // exception's
                // message
                // directly
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles business logic state validation errors. Returns a 400 Bad Request.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    /**
     * Returns a 404 Not Found.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponseDto> handleNoSuchElement(NoSuchElementException ex, HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage()) // Use the exception's message directly
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorDto, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles JPA EntityNotFoundException. Returns a 404 Not Found.
     */
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityNotFound(jakarta.persistence.EntityNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage() != null ? ex.getMessage() : "Entity not found")
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorDto, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles authorization failures. Returns a 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("You do not have permission to access this resource.")
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorDto, HttpStatus.FORBIDDEN);
    }

    /**
     * A catch-all handler for any other unexpected exceptions. Returns a 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleAny(Exception ex, HttpServletRequest request) {
        // It's a good practice to log the full stack trace for 500 errors
        log.error("An unexpected error occurred", ex);

        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred on the server.")
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthenticationFailed(AuthenticationFailedException ex,
            HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorDto, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResource(NoResourceFoundException ex, HttpServletRequest request,
            HttpServletResponse response) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage() != null ? ex.getMessage() : "Resource not found")
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("Method Not Allowed")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorDto, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(RoomConflictException.class)
    public ResponseEntity<ErrorResponseDto> handleRoomConflict(RoomConflictException ex, HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorDto, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponseDto> handleTokenExpiredException(TokenExpiredException ex,
            HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }
}
