package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import tuwien.taskinn.dto.response.QrLoginAttemptDto;
import tuwien.taskinn.dto.response.QrTokenResponseDto;
import tuwien.taskinn.dto.response.SignInResponseDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.MobileAuthToken;
import tuwien.taskinn.enums.MobileAuthTokenStatus;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.exceptions.TokenExpiredException;
import tuwien.taskinn.repo.MobileAuthTokenRepository;
import tuwien.taskinn.services.JwtService;
import tuwien.taskinn.services.impl.MobileAuthServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("MobileAuthService Unit Tests")
public class MobileAuthServiceImplTest {

    @Mock
    private MobileAuthTokenRepository mobileAuthTokenRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private MobileAuthServiceImpl mobileAuthService;

    private static final String TEST_EMAIL = "employee@taskinn.com";
    private static final String TEST_TOKEN = "test-token-uuid";
    private static final String TEST_SESSION_ID = "test-session-uuid";
    private static final long TOKEN_TTL_SECONDS = 60;
    private static final long EXCHANGE_TIMEOUT_SECONDS = 1;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mobileAuthService, "tokenTtlSeconds", TOKEN_TTL_SECONDS);
        ReflectionTestUtils.setField(mobileAuthService, "exchangeTimeoutSeconds", EXCHANGE_TIMEOUT_SECONDS);
        // EntityManager is injected via @PersistenceContext, not constructor injection
        ReflectionTestUtils.setField(mobileAuthService, "entityManager", entityManager);
    }

    @Nested
    @DisplayName("generateQrToken Tests")
    class GenerateQrTokenTests {

        @Test
        @DisplayName("Should create token successfully when user exists and is an employee")
        void generateQrToken_whenUserExists_createsTokenSuccessfully() {
            Employee employee = Employee.builder()
                    .id(1L)
                    .email(TEST_EMAIL)
                    .firstName("John")
                    .lastName("Doe")
                    .roles(Set.of(Roles.EMPLOYEE))
                    .build();

            when(employeeRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(employee));

            ArgumentCaptor<MobileAuthToken> tokenCaptor = ArgumentCaptor.forClass(MobileAuthToken.class);

            QrTokenResponseDto response = mobileAuthService.generateQrToken(TEST_EMAIL);

            verify(mobileAuthTokenRepository, times(1)).save(tokenCaptor.capture());
            MobileAuthToken savedToken = tokenCaptor.getValue();

            assertAll(() -> assertThat(response).isNotNull(), () -> assertThat(response.getToken()).isNotNull(),
                    () -> assertThat(response.getSessionId()).isNotNull(),
                    () -> assertThat(response.getExpiresInSeconds()).isEqualTo(TOKEN_TTL_SECONDS),
                    () -> assertThat(savedToken.getStatus()).isEqualTo(MobileAuthTokenStatus.PENDING),
                    () -> assertThat(savedToken.getEmployee()).isEqualTo(employee),
                    () -> assertThat(savedToken.getExpiresAt()).isAfter(Instant.now()));
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void generateQrToken_whenUserNotFound_throwsIllegalArgumentException() {
            when(employeeRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> mobileAuthService.generateQrToken(TEST_EMAIL));

            assertAll(() -> assertThat(exception.getMessage()).isEqualTo("User not found"),
                    () -> verify(mobileAuthTokenRepository, never()).save(any(MobileAuthToken.class)));
        }

        @Test
        @DisplayName("Should throw exception when user is an admin")
        void generateQrToken_whenUserIsAdmin_throwsIllegalArgumentException() {
            Employee admin = Employee.builder()
                    .id(1L)
                    .email(TEST_EMAIL)
                    .firstName("Admin")
                    .lastName("User")
                    .roles(Set.of(Roles.ADMIN))
                    .build();

            when(employeeRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(admin));

            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> mobileAuthService.generateQrToken(TEST_EMAIL));

            assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Admins cannot use QR code login"),
                    () -> verify(mobileAuthTokenRepository, never()).save(any(MobileAuthToken.class)));
        }
    }

    @Nested
    @DisplayName("exchangeToken Tests")
    class ExchangeTokenTests {

        private Employee testEmployee;
        private MobileAuthToken validToken;

        @BeforeEach
        void setUpExchangeToken() {
            testEmployee = Employee.builder()
                    .id(1L)
                    .email(TEST_EMAIL)
                    .firstName("John")
                    .lastName("Doe")
                    .roles(Set.of(Roles.EMPLOYEE))
                    .build();

            validToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().plus(TOKEN_TTL_SECONDS, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.PENDING)
                    .build();
        }

        @Test
        @DisplayName("Should return JWT when token is valid and confirmed")
        void exchangeToken_whenTokenValidAndConfirmed_returnsJwt() {
            String expectedJwt = "jwt-access-token";
            long expectedTtl = 900L;

            // Token that will be returned after status update (for updateTokenStatus)
            MobileAuthToken awaitingToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().plus(TOKEN_TTL_SECONDS, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.AWAITING_CONFIRMATION)
                    .build();

            MobileAuthToken confirmedToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().plus(TOKEN_TTL_SECONDS, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.CONFIRMED)
                    .usedAt(Instant.now())
                    .build();

            when(mobileAuthTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(validToken));
            // First call is for updateTokenStatus, subsequent calls are for the polling loop
            when(mobileAuthTokenRepository.findById(1L)).thenReturn(Optional.of(awaitingToken))
                    .thenReturn(Optional.of(confirmedToken));
            when(jwtService.generateAccessToken(TEST_EMAIL, testEmployee.getRoles())).thenReturn(expectedJwt);
            when(jwtService.getAccessTtlSeconds()).thenReturn(expectedTtl);

            SignInResponseDto response = mobileAuthService.exchangeToken(TEST_TOKEN);

            assertAll(() -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getAccessToken()).isEqualTo(expectedJwt),
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"),
                    () -> assertThat(response.getExpiresIn()).isEqualTo(expectedTtl));
        }

        @Test
        @DisplayName("Should throw exception when token is not found")
        void exchangeToken_whenTokenNotFound_throwsIllegalArgumentException() {
            when(mobileAuthTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> mobileAuthService.exchangeToken(TEST_TOKEN));

            assertThat(exception.getMessage()).isEqualTo("Invalid token");
        }

        @Test
        @DisplayName("Should throw exception and mark token as expired when token is expired")
        void exchangeToken_whenTokenExpired_throwsIllegalArgumentException() {
            MobileAuthToken expiredToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                    .status(MobileAuthTokenStatus.PENDING)
                    .build();

            when(mobileAuthTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(expiredToken));
            when(mobileAuthTokenRepository.findById(1L)).thenReturn(Optional.of(expiredToken));

            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> mobileAuthService.exchangeToken(TEST_TOKEN));

            assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Token expired"),
                    () -> verify(mobileAuthTokenRepository, atLeastOnce()).save(any(MobileAuthToken.class)));
        }

        @Test
        @DisplayName("Should throw exception when token has already been used")
        void exchangeToken_whenTokenAlreadyUsed_throwsIllegalArgumentException() {
            MobileAuthToken usedToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().plus(TOKEN_TTL_SECONDS, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.PENDING)
                    .usedAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                    .build();

            when(mobileAuthTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(usedToken));

            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> mobileAuthService.exchangeToken(TEST_TOKEN));

            assertThat(exception.getMessage()).isEqualTo("Token already used");
        }

        @Test
        @DisplayName("Should throw exception when token is in invalid state")
        void exchangeToken_whenTokenInInvalidState_throwsIllegalArgumentException() {
            MobileAuthToken invalidStateToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().plus(TOKEN_TTL_SECONDS, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.AWAITING_CONFIRMATION)
                    .build();

            when(mobileAuthTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(invalidStateToken));

            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> mobileAuthService.exchangeToken(TEST_TOKEN));

            assertThat(exception.getMessage()).isEqualTo("Token in invalid state");
        }

        @Test
        @DisplayName("Should throw exception when login is denied")
        void exchangeToken_whenLoginDenied_throwsIllegalArgumentException() {
            MobileAuthToken awaitingToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().plus(TOKEN_TTL_SECONDS, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.AWAITING_CONFIRMATION)
                    .build();

            MobileAuthToken deniedToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().plus(TOKEN_TTL_SECONDS, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.DENIED)
                    .build();

            when(mobileAuthTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(validToken));
            // First call is for updateTokenStatus, subsequent calls are for the polling loop
            when(mobileAuthTokenRepository.findById(1L)).thenReturn(Optional.of(awaitingToken))
                    .thenReturn(Optional.of(deniedToken));

            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> mobileAuthService.exchangeToken(TEST_TOKEN));

            assertThat(exception.getMessage()).isEqualTo("Login denied by user");
        }

        @Test
        @DisplayName("Should throw exception when confirmation times out")
        void exchangeToken_whenTimeout_throwsIllegalArgumentException() {
            MobileAuthToken awaitingToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().plus(TOKEN_TTL_SECONDS, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.AWAITING_CONFIRMATION)
                    .build();

            when(mobileAuthTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(validToken));
            when(mobileAuthTokenRepository.findById(1L)).thenReturn(Optional.of(awaitingToken));

            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> mobileAuthService.exchangeToken(TEST_TOKEN));

            assertThat(exception.getMessage()).isEqualTo("Login confirmation timed out");
        }

        @Test
        @DisplayName("Should send WebSocket notification when token exchange starts")
        void exchangeToken_sendsWebSocketNotification() {
            MobileAuthToken awaitingToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().plus(TOKEN_TTL_SECONDS, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.AWAITING_CONFIRMATION)
                    .build();

            MobileAuthToken confirmedToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().plus(TOKEN_TTL_SECONDS, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.CONFIRMED)
                    .usedAt(Instant.now())
                    .build();

            when(mobileAuthTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(validToken));
            // First call is for updateTokenStatus, subsequent calls are for the polling loop
            when(mobileAuthTokenRepository.findById(1L)).thenReturn(Optional.of(awaitingToken))
                    .thenReturn(Optional.of(confirmedToken));
            when(jwtService.generateAccessToken(anyString(), any())).thenReturn("jwt-token");
            when(jwtService.getAccessTtlSeconds()).thenReturn(900L);

            mobileAuthService.exchangeToken(TEST_TOKEN);

            verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/qr-auth/" + TEST_SESSION_ID),
                    any(QrLoginAttemptDto.class));
        }
    }

    @Nested
    @DisplayName("confirmLogin Tests")
    class ConfirmLoginTests {

        private Employee testEmployee;
        private MobileAuthToken awaitingToken;

        @BeforeEach
        void setUpConfirmLogin() {
            testEmployee = Employee.builder()
                    .id(1L)
                    .email(TEST_EMAIL)
                    .firstName("John")
                    .lastName("Doe")
                    .roles(Set.of(Roles.EMPLOYEE))
                    .build();

            awaitingToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().plus(TOKEN_TTL_SECONDS, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.AWAITING_CONFIRMATION)
                    .build();
        }

        @Test
        @DisplayName("Should set status to CONFIRMED when confirmed is true")
        void confirmLogin_whenConfirmed_setsStatusToConfirmed() {
            when(mobileAuthTokenRepository.findBySessionId(TEST_SESSION_ID)).thenReturn(Optional.of(awaitingToken));

            ArgumentCaptor<MobileAuthToken> tokenCaptor = ArgumentCaptor.forClass(MobileAuthToken.class);

            mobileAuthService.confirmLogin(TEST_SESSION_ID, true, TEST_EMAIL);

            verify(mobileAuthTokenRepository, times(1)).save(tokenCaptor.capture());
            MobileAuthToken savedToken = tokenCaptor.getValue();

            assertAll(() -> assertThat(savedToken.getStatus()).isEqualTo(MobileAuthTokenStatus.CONFIRMED),
                    () -> assertThat(savedToken.getUsedAt()).isNotNull());
        }

        @Test
        @DisplayName("Should set status to DENIED when confirmed is false")
        void confirmLogin_whenDenied_setsStatusToDenied() {
            when(mobileAuthTokenRepository.findBySessionId(TEST_SESSION_ID)).thenReturn(Optional.of(awaitingToken));

            ArgumentCaptor<MobileAuthToken> tokenCaptor = ArgumentCaptor.forClass(MobileAuthToken.class);

            mobileAuthService.confirmLogin(TEST_SESSION_ID, false, TEST_EMAIL);

            verify(mobileAuthTokenRepository, times(1)).save(tokenCaptor.capture());
            MobileAuthToken savedToken = tokenCaptor.getValue();

            assertAll(() -> assertThat(savedToken.getStatus()).isEqualTo(MobileAuthTokenStatus.DENIED),
                    () -> assertThat(savedToken.getUsedAt()).isNull());
        }

        @Test
        @DisplayName("Should throw exception when session is not found")
        void confirmLogin_whenSessionNotFound_throwsIllegalArgumentException() {
            when(mobileAuthTokenRepository.findBySessionId(TEST_SESSION_ID)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> mobileAuthService.confirmLogin(TEST_SESSION_ID, true, TEST_EMAIL));

            assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Invalid session ID"),
                    () -> verify(mobileAuthTokenRepository, never()).save(any(MobileAuthToken.class)));
        }

        @Test
        @DisplayName("Should throw exception when token is expired")
        void confirmLogin_whenTokenExpired_throwsTokenExpiredException() {
            MobileAuthToken expiredToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                    .status(MobileAuthTokenStatus.AWAITING_CONFIRMATION)
                    .build();

            when(mobileAuthTokenRepository.findBySessionId(TEST_SESSION_ID)).thenReturn(Optional.of(expiredToken));

            TokenExpiredException exception = assertThrows(TokenExpiredException.class,
                    () -> mobileAuthService.confirmLogin(TEST_SESSION_ID, true, TEST_EMAIL));

            assertThat(exception.getMessage()).isEqualTo("Token expired");
        }

        @Test
        @DisplayName("Should throw exception when token is not in AWAITING_CONFIRMATION state")
        void confirmLogin_whenWrongStatus_throwsIllegalArgumentException() {
            MobileAuthToken pendingToken = MobileAuthToken.builder()
                    .id(1L)
                    .token(TEST_TOKEN)
                    .sessionId(TEST_SESSION_ID)
                    .employee(testEmployee)
                    .expiresAt(Instant.now().plus(TOKEN_TTL_SECONDS, ChronoUnit.SECONDS))
                    .status(MobileAuthTokenStatus.PENDING)
                    .build();

            when(mobileAuthTokenRepository.findBySessionId(TEST_SESSION_ID)).thenReturn(Optional.of(pendingToken));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> mobileAuthService.confirmLogin(TEST_SESSION_ID, true, TEST_EMAIL));

            assertThat(exception.getMessage()).isEqualTo("Token not awaiting confirmation");
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when user is not the token owner")
        void confirmLogin_whenNotTokenOwner_throwsAccessDeniedException() {
            when(mobileAuthTokenRepository.findBySessionId(TEST_SESSION_ID)).thenReturn(Optional.of(awaitingToken));

            String differentUserEmail = "other.user@taskinn.com";

            AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                    () -> mobileAuthService.confirmLogin(TEST_SESSION_ID, true, differentUserEmail));

            assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Not authorized to confirm this session"),
                    () -> verify(mobileAuthTokenRepository, never()).save(any(MobileAuthToken.class)));
        }
    }
}
