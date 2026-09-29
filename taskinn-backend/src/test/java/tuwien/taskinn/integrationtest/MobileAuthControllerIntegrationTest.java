package tuwien.taskinn.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.request.QrLoginConfirmRequestDto;
import tuwien.taskinn.dto.request.QrTokenExchangeRequestDto;
import tuwien.taskinn.dto.response.QrTokenResponseDto;
import tuwien.taskinn.dto.response.SignInResponseDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.MobileAuthToken;
import tuwien.taskinn.enums.MobileAuthTokenStatus;
import tuwien.taskinn.enums.Roles;

@DisplayName("MobileAuth Controller Integration Tests")
public class MobileAuthControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String MOBILE_AUTH_BASE_URI = BASE_URI + "/auth/mobile";

    @Nested
    @DisplayName("POST /api/v1/auth/mobile/qr-token")
    class GenerateQrTokenTests {

        @Test
        @DisplayName("Should return 201 Created with token when authenticated as employee")
        void generateQrToken_whenAuthenticated_returnsCreatedWithToken() throws Exception {
            String employeeToken = getEmployeeAuthToken();

            var mvcResult = mockMvc.perform(post(MOBILE_AUTH_BASE_URI + "/qr-token").header(HttpHeaders.AUTHORIZATION,
                    "Bearer " + employeeToken)).andExpect(status().isCreated()).andReturn();

            String responseBody = mvcResult.getResponse().getContentAsString();
            QrTokenResponseDto response = objectMapper.readValue(responseBody, new TypeReference<>() {});

            assertAll(() -> assertThat(response.getToken()).isNotNull().isNotEmpty(),
                    () -> assertThat(response.getSessionId()).isNotNull().isNotEmpty(),
                    () -> assertThat(response.getExpiresInSeconds()).isGreaterThan(0),
                    () -> assertThat(mobileAuthTokenRepository.findByToken(response.getToken())).isPresent());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when unauthenticated")
        void generateQrToken_whenUnauthenticated_returnsForbidden() throws Exception {
            mockMvc.perform(post(MOBILE_AUTH_BASE_URI + "/qr-token")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when user is admin")
        void generateQrToken_whenAdminUser_returnsBadRequest() throws Exception {
            String adminToken = getAdminAuthToken();

            var mvcResult = mockMvc.perform(
                    post(MOBILE_AUTH_BASE_URI + "/qr-token").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String responseBody = mvcResult.getResponse().getContentAsString();
            assertThat(responseBody).contains("Admins cannot use QR code login");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/mobile/exchange")
    class ExchangeTokenTests {

        @Test
        @DisplayName("Should return 200 OK with JWT when token is valid and confirmed")
        void exchangeToken_whenTokenValidAndConfirmed_returnsJwt() throws Exception {
            Employee employee = createAndSaveEmployee("qr.employee@taskinn.com", Roles.EMPLOYEE);
            String employeeAuthToken = generateToken(employee);

            // Generate QR token
            var generateResult =
                    mockMvc.perform(post(MOBILE_AUTH_BASE_URI + "/qr-token").header(HttpHeaders.AUTHORIZATION,
                            "Bearer " + employeeAuthToken)).andExpect(status().isCreated()).andReturn();

            QrTokenResponseDto qrToken =
                    objectMapper.readValue(generateResult.getResponse().getContentAsString(), new TypeReference<>() {});

            // Start background thread to confirm login after brief delay
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread.sleep(500);
                    QrLoginConfirmRequestDto confirmRequest =
                            new QrLoginConfirmRequestDto(qrToken.getSessionId(), true);
                    mockMvc.perform(post(MOBILE_AUTH_BASE_URI + "/confirm")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeAuthToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(confirmRequest)));
                } catch (Exception e) {
                    // Ignore
                }
            });

            // Call exchange endpoint (blocks until confirmed or timeout)
            QrTokenExchangeRequestDto exchangeRequest = new QrTokenExchangeRequestDto(qrToken.getToken());
            var exchangeResult = mockMvc
                    .perform(post(MOBILE_AUTH_BASE_URI + "/exchange").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(exchangeRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            SignInResponseDto response =
                    objectMapper.readValue(exchangeResult.getResponse().getContentAsString(), new TypeReference<>() {});

            assertAll(() -> assertThat(response.getAccessToken()).isNotNull().isNotEmpty(),
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"),
                    () -> assertThat(response.getExpiresIn()).isGreaterThan(0));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when token is invalid")
        void exchangeToken_whenTokenInvalid_returnsBadRequest() throws Exception {
            QrTokenExchangeRequestDto request = new QrTokenExchangeRequestDto("invalid-token");

            var mvcResult = mockMvc
                    .perform(post(MOBILE_AUTH_BASE_URI + "/exchange").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String responseBody = mvcResult.getResponse().getContentAsString();
            assertThat(responseBody).contains("Invalid token");
        }

        @Test
        @DisplayName("Should return 400 Bad Request when token is expired")
        void exchangeToken_whenTokenExpired_returnsBadRequest() throws Exception {
            Employee employee = createAndSaveEmployee("expired.employee@taskinn.com", Roles.EMPLOYEE);

            MobileAuthToken expiredToken = MobileAuthToken.builder()
                    .token("expired-token-uuid")
                    .sessionId("expired-session-uuid")
                    .employee(employee)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                    .status(MobileAuthTokenStatus.PENDING)
                    .build();
            mobileAuthTokenRepository.save(expiredToken);

            QrTokenExchangeRequestDto request = new QrTokenExchangeRequestDto(expiredToken.getToken());

            var mvcResult = mockMvc
                    .perform(post(MOBILE_AUTH_BASE_URI + "/exchange").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String responseBody = mvcResult.getResponse().getContentAsString();
            assertThat(responseBody).contains("Token expired");
        }

        @Test
        @DisplayName("Should return 400 Bad Request when token is blank")
        void exchangeToken_whenBlankToken_returnsBadRequest() throws Exception {
            QrTokenExchangeRequestDto request = new QrTokenExchangeRequestDto("");

            mockMvc.perform(post(MOBILE_AUTH_BASE_URI + "/exchange").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/mobile/confirm")
    class ConfirmLoginTests {

        @Test
        @DisplayName("Should return 204 No Content when confirmed is true")
        void confirmLogin_whenConfirmTrue_returnsNoContent() throws Exception {
            Employee employee = createAndSaveEmployee("confirm.employee@taskinn.com", Roles.EMPLOYEE);
            String employeeAuthToken = generateToken(employee);

            MobileAuthToken awaitingToken = MobileAuthToken.builder()
                    .token("confirm-token-uuid")
                    .sessionId("confirm-session-uuid")
                    .employee(employee)
                    .expiresAt(Instant.now().plus(60, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.AWAITING_CONFIRMATION)
                    .build();
            mobileAuthTokenRepository.save(awaitingToken);

            QrLoginConfirmRequestDto request = new QrLoginConfirmRequestDto(awaitingToken.getSessionId(), true);

            mockMvc.perform(post(MOBILE_AUTH_BASE_URI + "/confirm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeAuthToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isNoContent());

            MobileAuthToken updatedToken =
                    mobileAuthTokenRepository.findBySessionId(awaitingToken.getSessionId()).orElseThrow();
            assertAll(() -> assertThat(updatedToken.getStatus()).isEqualTo(MobileAuthTokenStatus.CONFIRMED),
                    () -> assertThat(updatedToken.getUsedAt()).isNotNull());
        }

        @Test
        @DisplayName("Should return 204 No Content when confirmed is false and set status to DENIED")
        void confirmLogin_whenConfirmFalse_returnsNoContent() throws Exception {
            Employee employee = createAndSaveEmployee("deny.employee@taskinn.com", Roles.EMPLOYEE);
            String employeeAuthToken = generateToken(employee);

            MobileAuthToken awaitingToken = MobileAuthToken.builder()
                    .token("deny-token-uuid")
                    .sessionId("deny-session-uuid")
                    .employee(employee)
                    .expiresAt(Instant.now().plus(60, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.AWAITING_CONFIRMATION)
                    .build();
            mobileAuthTokenRepository.save(awaitingToken);

            QrLoginConfirmRequestDto request = new QrLoginConfirmRequestDto(awaitingToken.getSessionId(), false);

            mockMvc.perform(post(MOBILE_AUTH_BASE_URI + "/confirm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeAuthToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isNoContent());

            MobileAuthToken updatedToken =
                    mobileAuthTokenRepository.findBySessionId(awaitingToken.getSessionId()).orElseThrow();
            assertThat(updatedToken.getStatus()).isEqualTo(MobileAuthTokenStatus.DENIED);
        }

        @Test
        @DisplayName("Should return 403 Forbidden when unauthenticated")
        void confirmLogin_whenUnauthenticated_returnsForbidden() throws Exception {
            QrLoginConfirmRequestDto request = new QrLoginConfirmRequestDto("any-session-id", true);

            mockMvc.perform(post(MOBILE_AUTH_BASE_URI + "/confirm").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when session is invalid")
        void confirmLogin_whenInvalidSession_returnsBadRequest() throws Exception {
            String employeeToken = getEmployeeAuthToken();

            QrLoginConfirmRequestDto request = new QrLoginConfirmRequestDto("invalid-session-id", true);

            var mvcResult = mockMvc
                    .perform(post(MOBILE_AUTH_BASE_URI + "/confirm")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String responseBody = mvcResult.getResponse().getContentAsString();
            assertThat(responseBody).contains("Invalid session ID");
        }

        @Test
        @DisplayName("Should return 400 Bad Request when session ID is blank")
        void confirmLogin_whenBlankSessionId_returnsBadRequest() throws Exception {
            String employeeToken = getEmployeeAuthToken();

            QrLoginConfirmRequestDto request = new QrLoginConfirmRequestDto("", true);

            mockMvc.perform(
                    post(MOBILE_AUTH_BASE_URI + "/confirm").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when trying to confirm another user's session")
        void confirmLogin_whenNotTokenOwner_returnsForbidden() throws Exception {
            // Create token for employee A
            Employee employeeA = createAndSaveEmployee("employee.a@taskinn.com", Roles.EMPLOYEE);
            MobileAuthToken token = MobileAuthToken.builder()
                    .token("token-uuid")
                    .sessionId("session-uuid")
                    .employee(employeeA)
                    .expiresAt(Instant.now().plus(60, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.AWAITING_CONFIRMATION)
                    .build();
            mobileAuthTokenRepository.save(token);

            // Authenticate as employee B and try to confirm employee A's session
            Employee employeeB = createAndSaveEmployee("employee.b@taskinn.com", Roles.EMPLOYEE);
            String employeeBToken = generateToken(employeeB);

            QrLoginConfirmRequestDto request = new QrLoginConfirmRequestDto(token.getSessionId(), true);

            mockMvc.perform(post(MOBILE_AUTH_BASE_URI + "/confirm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeBToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should persist EXPIRED status when confirming expired token")
        void confirmLogin_whenTokenExpired_persistsExpiredStatus() throws Exception {
            Employee employee = createAndSaveEmployee("expired.confirm@taskinn.com", Roles.EMPLOYEE);
            String employeeToken = generateToken(employee);

            MobileAuthToken expiredToken = MobileAuthToken.builder()
                    .token("expired-confirm-token")
                    .sessionId("expired-confirm-session")
                    .employee(employee)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                    .status(MobileAuthTokenStatus.AWAITING_CONFIRMATION)
                    .build();
            mobileAuthTokenRepository.save(expiredToken);

            QrLoginConfirmRequestDto request = new QrLoginConfirmRequestDto(expiredToken.getSessionId(), true);

            mockMvc.perform(
                    post(MOBILE_AUTH_BASE_URI + "/confirm").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            // Verify EXPIRED status was persisted
            MobileAuthToken updatedToken =
                    mobileAuthTokenRepository.findBySessionId(expiredToken.getSessionId()).orElseThrow();
            assertThat(updatedToken.getStatus()).isEqualTo(MobileAuthTokenStatus.EXPIRED);
        }
    }
}
