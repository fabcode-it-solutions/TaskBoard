package tuwien.taskinn.unittest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tuwien.taskinn.config.GlobalExceptionHandler;
import tuwien.taskinn.dto.response.ErrorResponseDto;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
public class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    @DisplayName("handleNoResource should return 404 with error details when NoResourceFoundException is thrown")
    public void handleNoResource_whenNoResourceFoundExceptionThrown_returns404WithErrorDetails() {
        // Arrange
        String requestUri = "/api/nonexistent-resource";
        String resourcePath = "api/nonexistent-resource.";

        NoResourceFoundException exception = new NoResourceFoundException(null, resourcePath);
        when(request.getRequestURI()).thenReturn(requestUri);

        // Act
        ResponseEntity<ErrorResponseDto> responseEntity =
                globalExceptionHandler.handleNoResource(exception, request, response);

        // Assert
        assertAll(() -> assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                () -> assertThat(responseEntity.getBody()).isNotNull(),
                () -> assertThat(responseEntity.getBody().getStatus()).isEqualTo(404),
                () -> assertThat(responseEntity.getBody().getError()).isEqualTo("Not Found"),
                () -> assertThat(responseEntity.getBody().getMessage()).contains("No static resource"),
                () -> assertThat(responseEntity.getBody().getMessage()).contains(resourcePath),
                () -> assertThat(responseEntity.getBody().getPath()).isEqualTo(requestUri),
                () -> assertThat(responseEntity.getBody().getTimestamp()).isNotNull());
    }

    @Test
    @DisplayName("handleNoResource should return 404 with default message when exception message is null")
    public void handleNoResource_whenExceptionMessageIsNull_returns404WithDefaultMessage() {
        // Arrange
        String requestUri = "/api/missing-resource";

        NoResourceFoundException exception = new NoResourceFoundException(null, null);
        when(request.getRequestURI()).thenReturn(requestUri);

        // Act
        ResponseEntity<ErrorResponseDto> responseEntity =
                globalExceptionHandler.handleNoResource(exception, request, response);

        // Assert
        assertAll(() -> assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                () -> assertThat(responseEntity.getBody()).isNotNull(),
                () -> assertThat(responseEntity.getBody().getStatus()).isEqualTo(404),
                () -> assertThat(responseEntity.getBody().getError()).isEqualTo("Not Found"),
                () -> assertThat(responseEntity.getBody().getMessage()).isNotNull(),
                () -> assertThat(responseEntity.getBody().getMessage()).contains("No static resource"),
                () -> assertThat(responseEntity.getBody().getPath()).isEqualTo(requestUri),
                () -> assertThat(responseEntity.getBody().getTimestamp()).isNotNull());
    }

    @Test
    @DisplayName("handleHttpRequestMethodNotSupported should return 405 with error details when HttpRequestMethodNotSupportedException is thrown")
    public void handleHttpRequestMethodNotSupported_whenMethodNotSupportedException_returns405WithErrorDetails() {
        // Arrange
        String requestUri = "/api/bookings/1";
        String httpMethod = "DELETE";
        String exceptionMessage = "Request method 'DELETE' is not supported";

        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException(httpMethod);
        when(request.getRequestURI()).thenReturn(requestUri);

        // Act
        ResponseEntity<ErrorResponseDto> responseEntity =
                globalExceptionHandler.handleHttpRequestMethodNotSupported(exception, request);

        // Assert
        assertAll(() -> assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED),
                () -> assertThat(responseEntity.getBody()).isNotNull(),
                () -> assertThat(responseEntity.getBody().getStatus()).isEqualTo(405),
                () -> assertThat(responseEntity.getBody().getError()).isEqualTo("Method Not Allowed"),
                () -> assertThat(responseEntity.getBody().getMessage()).contains(httpMethod),
                () -> assertThat(responseEntity.getBody().getPath()).isEqualTo(requestUri),
                () -> assertThat(responseEntity.getBody().getTimestamp()).isNotNull());
    }

    @Test
    @DisplayName("handleHttpRequestMethodNotSupported should return 405 for POST when only GET is supported")
    public void handleHttpRequestMethodNotSupported_whenPostNotSupported_returns405() {
        // Arrange
        String requestUri = "/api/status";
        String httpMethod = "POST";

        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException(httpMethod, List.of("GET"));
        when(request.getRequestURI()).thenReturn(requestUri);

        // Act
        ResponseEntity<ErrorResponseDto> responseEntity =
                globalExceptionHandler.handleHttpRequestMethodNotSupported(exception, request);

        // Assert
        assertAll(() -> assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED),
                () -> assertThat(responseEntity.getBody()).isNotNull(),
                () -> assertThat(responseEntity.getBody().getStatus()).isEqualTo(405),
                () -> assertThat(responseEntity.getBody().getError()).isEqualTo("Method Not Allowed"),
                () -> assertThat(responseEntity.getBody().getMessage()).isNotNull(),
                () -> assertThat(responseEntity.getBody().getPath()).isEqualTo(requestUri));
    }

    @Test
    @DisplayName("handleHttpRequestMethodNotSupported should handle PUT method not supported")
    public void handleHttpRequestMethodNotSupported_whenPutNotSupported_returns405() {
        // Arrange
        String requestUri = "/api/employees/123";
        String httpMethod = "PUT";

        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException(httpMethod, List.of("GET", "POST"));
        when(request.getRequestURI()).thenReturn(requestUri);

        // Act
        ResponseEntity<ErrorResponseDto> responseEntity =
                globalExceptionHandler.handleHttpRequestMethodNotSupported(exception, request);

        // Assert
        assertAll(() -> assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED),
                () -> assertThat(responseEntity.getBody()).isNotNull(),
                () -> assertThat(responseEntity.getBody().getStatus()).isEqualTo(405),
                () -> assertThat(responseEntity.getBody().getError()).isEqualTo("Method Not Allowed"),
                () -> assertThat(responseEntity.getBody().getMessage()).contains("PUT"),
                () -> assertThat(responseEntity.getBody().getPath()).isEqualTo(requestUri),
                () -> assertThat(responseEntity.getBody().getTimestamp()).isNotNull());
    }
}
