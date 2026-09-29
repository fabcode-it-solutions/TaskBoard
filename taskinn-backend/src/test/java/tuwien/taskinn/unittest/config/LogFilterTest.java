package tuwien.taskinn.unittest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import tuwien.taskinn.config.LogFilter;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogFilter Unit Tests")
public class LogFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private LogFilter logFilter;

    @BeforeEach
    void setUp() {
        logFilter = new LogFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("doFilterInternal should call filterChain and populate MDC for normal paths")
    void doFilterInternal_normalPath_callsFilterChainAndPopulatesMdc() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/bookings");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(response.getStatus()).thenReturn(200);

        logFilter.doFilterInternal(request, response, filterChain);

        assertAll(() -> verify(filterChain).doFilter(request, response), () -> assertThat(MDC.get("ip")).isNull());
    }

    @Test
    @DisplayName("doFilterInternal should skip logging but call filterChain for swagger-ui path")
    void doFilterInternal_swaggerUiPath_skipsLoggingButCallsFilterChain() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        // Act
        logFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertAll(() -> verify(filterChain).doFilter(request, response), () -> verify(request, never()).getMethod(),
                () -> assertThat(MDC.get("ip")).isNull());
    }

    @Test
    @DisplayName("doFilterInternal should skip logging but call filterChain for swagger.yaml path")
    void doFilterInternal_swaggerYamlPath_skipsLoggingButCallsFilterChain() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/swagger.yaml");

        logFilter.doFilterInternal(request, response, filterChain);

        assertAll(() -> verify(filterChain).doFilter(request, response), () -> verify(request, never()).getMethod(),
                () -> assertThat(MDC.get("ip")).isNull());
    }

    @Test
    @DisplayName("doFilterInternal should use X-Forwarded-For header for IP when present")
    void doFilterInternal_withXForwardedForHeader_usesForwardedIp() throws ServletException, IOException {
        String forwardedIp = "10.0.0.1";
        when(request.getRequestURI()).thenReturn("/api/v1/employees");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedIp);
        when(response.getStatus()).thenReturn(200);

        final String[] capturedIp = new String[1];
        doAnswer(invocation -> {
            capturedIp[0] = MDC.get("ip");
            return null;
        }).when(filterChain).doFilter(request, response);

        logFilter.doFilterInternal(request, response, filterChain);

        assertThat(capturedIp[0]).isEqualTo(forwardedIp);
    }

    @Test
    @DisplayName("doFilterInternal should use remoteAddr for IP when X-Forwarded-For header is absent")
    void doFilterInternal_withoutXForwardedForHeader_usesRemoteAddr() throws ServletException, IOException {
        String remoteAddr = "192.168.1.100";
        when(request.getRequestURI()).thenReturn("/api/v1/employees");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        when(response.getStatus()).thenReturn(200);

        final String[] capturedIp = new String[1];
        doAnswer(invocation -> {
            capturedIp[0] = MDC.get("ip");
            return null;
        }).when(filterChain).doFilter(request, response);

        logFilter.doFilterInternal(request, response, filterChain);

        assertThat(capturedIp[0]).isEqualTo(remoteAddr);
    }

    @Test
    @DisplayName("doFilterInternal should clear MDC after request processing")
    void doFilterInternal_clearsMdcAfterRequest() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/bookings");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(response.getStatus()).thenReturn(201);

        logFilter.doFilterInternal(request, response, filterChain);

        assertAll(() -> assertThat(MDC.get("ip")).isNull(), () -> assertThat(MDC.get("r")).isNull(),
                () -> assertThat(MDC.get("http_request_method")).isNull(),
                () -> assertThat(MDC.get("http_request_url")).isNull(), () -> assertThat(MDC.get("status")).isNull(),
                () -> assertThat(MDC.get("duration")).isNull());
    }

    @Test
    @DisplayName("doFilterInternal should wrap ServletException in RuntimeException")
    void doFilterInternal_filterChainThrowsServletException_wrapsInRuntimeException()
            throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/bookings");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        ServletException servletException = new ServletException("Test servlet exception");
        doThrow(servletException).when(filterChain).doFilter(request, response);

        assertThatThrownBy(() -> logFilter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(RuntimeException.class)
                .hasCause(servletException);
    }

    @Test
    @DisplayName("doFilterInternal should wrap IOException in RuntimeException")
    void doFilterInternal_filterChainThrowsIOException_wrapsInRuntimeException() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/bookings");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        IOException ioException = new IOException("Test IO exception");
        doThrow(ioException).when(filterChain).doFilter(request, response);

        assertThatThrownBy(() -> logFilter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(RuntimeException.class)
                .hasCause(ioException);
    }

    @Test
    @DisplayName("doFilterInternal should set MDC status and duration for 200 response")
    void doFilterInternal_status200_setsMdcStatusAndDuration() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/bookings");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(response.getStatus()).thenReturn(200);

        final String[] capturedStatus = new String[1];
        final String[] capturedDuration = new String[1];

        doAnswer(invocation -> {
            return null;
        }).when(filterChain).doFilter(request, response);

        logFilter.doFilterInternal(request, response, filterChain);

        verify(response, atLeastOnce()).getStatus();
    }

    @Test
    @DisplayName("doFilterInternal should set MDC status and duration for 404 response")
    void doFilterInternal_status404_setsMdcStatusAndDuration() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/nonexistent");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(response.getStatus()).thenReturn(404);

        logFilter.doFilterInternal(request, response, filterChain);

        verify(response, atLeastOnce()).getStatus();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal should set MDC status and duration for 500 response")
    void doFilterInternal_status500_setsMdcStatusAndDuration() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/error");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(response.getStatus()).thenReturn(500);

        logFilter.doFilterInternal(request, response, filterChain);

        verify(response, atLeastOnce()).getStatus();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal should include query string in MDC")
    void doFilterInternal_withQueryString_includesInMdc() throws ServletException, IOException {
        String queryString = "page=1&size=10";
        when(request.getRequestURI()).thenReturn("/api/v1/bookings");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getQueryString()).thenReturn(queryString);
        when(response.getStatus()).thenReturn(200);

        final String[] capturedQueryString = new String[1];
        doAnswer(invocation -> {
            capturedQueryString[0] = MDC.get("http_request_query");
            return null;
        }).when(filterChain).doFilter(request, response);

        logFilter.doFilterInternal(request, response, filterChain);

        assertThat(capturedQueryString[0]).isEqualTo(queryString);
    }

    @Test
    @DisplayName("doFilterInternal should include User-Agent in MDC")
    void doFilterInternal_withUserAgent_includesInMdc() throws ServletException, IOException {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        when(request.getRequestURI()).thenReturn("/api/v1/bookings");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn(userAgent);
        when(response.getStatus()).thenReturn(200);

        final String[] capturedUserAgent = new String[1];
        doAnswer(invocation -> {
            capturedUserAgent[0] = MDC.get("http_request_ua");
            return null;
        }).when(filterChain).doFilter(request, response);

        logFilter.doFilterInternal(request, response, filterChain);

        assertThat(capturedUserAgent[0]).isEqualTo(userAgent);
    }

    @Test
    @DisplayName("doFilterInternal should clear MDC even when exception occurs")
    void doFilterInternal_whenExceptionOccurs_stillClearsMdc() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/bookings");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);

        try {
            logFilter.doFilterInternal(request, response, filterChain);
        } catch (RuntimeException e) {
        }

        assertAll(() -> assertThat(MDC.get("ip")).isNull(), () -> assertThat(MDC.get("r")).isNull(),
                () -> assertThat(MDC.get("http_request_method")).isNull());
    }

    @Test
    @DisplayName("doFilterInternal should populate request ID in MDC")
    void doFilterInternal_populatesRequestIdInMdc() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/bookings");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(response.getStatus()).thenReturn(200);

        final String[] capturedRequestId = new String[1];
        doAnswer(invocation -> {
            capturedRequestId[0] = MDC.get("r");
            return null;
        }).when(filterChain).doFilter(request, response);

        logFilter.doFilterInternal(request, response, filterChain);

        assertThat(capturedRequestId[0]).isNotNull();
        assertThat(capturedRequestId[0]).matches("[a-f0-9]{12}"); // Last segment of UUID
    }

    @Test
    @DisplayName("doFilterInternal should populate HTTP method in MDC")
    void doFilterInternal_populatesHttpMethodInMdc() throws ServletException, IOException {
        String httpMethod = "POST";
        when(request.getRequestURI()).thenReturn("/api/v1/bookings");
        when(request.getMethod()).thenReturn(httpMethod);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(response.getStatus()).thenReturn(201);

        final String[] capturedMethod = new String[1];
        doAnswer(invocation -> {
            capturedMethod[0] = MDC.get("http_request_method");
            return null;
        }).when(filterChain).doFilter(request, response);

        logFilter.doFilterInternal(request, response, filterChain);

        assertThat(capturedMethod[0]).isEqualTo(httpMethod);
    }

    @Test
    @DisplayName("doFilterInternal should populate request URL in MDC")
    void doFilterInternal_populatesRequestUrlInMdc() throws ServletException, IOException {
        String requestUri = "/api/v1/bookings/123";
        when(request.getRequestURI()).thenReturn(requestUri);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(response.getStatus()).thenReturn(200);

        final String[] capturedUrl = new String[1];
        doAnswer(invocation -> {
            capturedUrl[0] = MDC.get("http_request_url");
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        logFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(capturedUrl[0]).isEqualTo(requestUri);
    }
}
