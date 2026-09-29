package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tuwien.taskinn.dto.request.SignUpRequestDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.PasswordResetToken;
import tuwien.taskinn.entity.RevokedToken;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.exceptions.AuthenticationFailedException;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.PasswordResetTokenRepository;
import tuwien.taskinn.repo.RevokedTokenRepository;
import tuwien.taskinn.security.JwtPrincipal;
import tuwien.taskinn.services.EmailService;
import tuwien.taskinn.services.JwtService;
import tuwien.taskinn.services.impl.AuthServiceImpl;
import tuwien.taskinn.services.validator.CreateOrUpdateAccountValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
public class AuthServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RevokedTokenRepository revokedTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private CreateOrUpdateAccountValidator createOrUpdateAccountValidator;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("signUpEmployee should create and save a new user when email is not registered")
    public void signUpEmployee_whenEmailIsNew_createsEmployeeSuccessfully() {

        String password = "StrongPassword123!";
        String hashedPassword = "hashedStrongPassword";

        SignUpRequestDto signUpRequestDto = SignUpRequestDto.builder()
                .email("new.admin@taskinn.com")
                .password(password)
                .firstName("Admin")
                .lastName("User")
                .roles(Set.of(Roles.ADMIN))
                .build();

        when(employeeRepository.existsByEmail(signUpRequestDto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(hashedPassword);

        // The ArgumentCaptor will capture the User object passed to save()
        ArgumentCaptor<Employee> userCaptor = ArgumentCaptor.forClass(Employee.class);

        authService.signUpEmployee(signUpRequestDto);

        verify(employeeRepository, times(1)).save(userCaptor.capture());
        Employee savedUser = userCaptor.getValue();

        assertAll(() -> assertThat(savedUser).isNotNull(),
                () -> assertThat(savedUser.getEmail()).isEqualTo(signUpRequestDto.getEmail()),
                () -> assertThat(savedUser.getFirstName()).isEqualTo(signUpRequestDto.getFirstName()),
                () -> assertThat(savedUser.getLastName()).isEqualTo(signUpRequestDto.getLastName()),
                () -> assertThat(savedUser.getPasswordHash()).isEqualTo(hashedPassword),
                () -> assertThat(savedUser.getRoles()).isNotNull(),
                () -> assertThat(savedUser.getRoles()).containsExactly(Roles.ADMIN),
                () -> verify(createOrUpdateAccountValidator, times(1)).validateSignUpRequestDto(signUpRequestDto));
    }

    @Test
    @DisplayName("signUpAdmin should throw an IllegalArgumentException when email is already registered")
    public void signUpEmployee_whenEmailExists_throwsIllegalArgumentException() {
        String password = "anyPassword";

        SignUpRequestDto signUpRequestDto = SignUpRequestDto.builder()
                .email("existing.admin@taskinn.com")
                .password(password)
                .firstName("Admin")
                .lastName("User")
                .build();

        when(employeeRepository.existsByEmail(signUpRequestDto.getEmail())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.signUpEmployee(signUpRequestDto);
        });

        assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Email already registered"),
                () -> verify(employeeRepository, never()).save(any(Employee.class)),
                () -> verify(passwordEncoder, never()).encode(anyString()),
                () -> verify(createOrUpdateAccountValidator, never())
                        .validateSignUpRequestDto(any(SignUpRequestDto.class)));
    }

    @Test
    @DisplayName("signIn should throw an AuthenticationFailedException when user is inactive")
    public void signIn_whenInactive_throwsAuthenticationFailedException() {
        String email = "inactive.user@taskinn.com";
        String password = "password123";
        String hashedPassword = "hashedPassword";

        Employee inactiveEmployee = Employee.builder().email(email).passwordHash(hashedPassword).active(false).build();

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(inactiveEmployee));

        AuthenticationFailedException exception = assertThrows(AuthenticationFailedException.class, () -> {
            authService.signIn(email, password);
        });

        assertAll(() -> assertThat(exception.getMessage()).isEqualTo("User account is inactive"),
                () -> verify(passwordEncoder, never()).matches(anyString(), anyString()));
    }

    @Test
    @DisplayName("forgotPassword should create token and send email when user exists")
    void forgotPassword_whenUserExists_createsTokenAndSendsEmail() {
        String email = "user@test.com";
        Employee mockEmployee = Employee.builder().id(1L).email(email).build();
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(mockEmployee));
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        authService.forgotPassword(email);

        verify(passwordResetTokenRepository, times(1)).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertAll(() -> assertThat(savedToken.getEmployee()).isEqualTo(mockEmployee),
                () -> assertThat(savedToken.getToken()).isNotNull(),
                () -> assertThat(savedToken.getExpiresAt()).isAfter(Instant.now()),
                () -> verify(emailService, times(1)).sendResetPasswordEmail(email, savedToken.getToken())

        );
    }

    @Test
    @DisplayName("forgotPassword should do nothing when user does not exist (Security)")
    void forgotPassword_whenUserDoesNotExist_doesNothing() {
        String email = "nonexistent@test.com";
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());

        authService.forgotPassword(email);

        assertAll(() -> verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class)),
                () -> verify(emailService, never()).sendResetPasswordEmail(anyString(), anyString()));
    }

    @Test
    @DisplayName("resetPassword should update password when token is valid")
    void resetPassword_whenTokenIsValid_updatesPassword() {
        String token = "valid-token";
        String newPassword = "newStrongPassword123!";
        String newHashedPassword = "hashedNewPassword";

        Employee mockEmployee = Employee.builder().id(1L).email("user@test.com").build();
        PasswordResetToken prt = PasswordResetToken.builder()
                .token(token)
                .employee(mockEmployee)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES)) // Not expired
                .usedAt(null)
                .build();

        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(prt));
        when(passwordEncoder.encode(newPassword)).thenReturn(newHashedPassword);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        authService.resetPassword(token, newPassword);

        assertAll(() -> verify(employeeRepository, times(1)).save(employeeCaptor.capture()),
                () -> assertThat(employeeCaptor.getValue().getPasswordHash()).isEqualTo(newHashedPassword),
                () -> verify(passwordResetTokenRepository, times(1)).save(tokenCaptor.capture()),
                () -> assertThat(tokenCaptor.getValue().getUsedAt()).isNotNull());
    }

    @Test
    @DisplayName("resetPassword should throw exception when token does not exist")
    void resetPassword_whenTokenNotFound_throwsException() {
        when(passwordResetTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> authService.resetPassword("invalid-token", "any-password"));
        assertThat(exception.getMessage()).isEqualTo("Auth password token invalid");
    }

    @Test
    @DisplayName("resetPassword should throw exception when token has already been used")
    void resetPassword_whenTokenIsUsed_throwsException() {
        PasswordResetToken usedToken = PasswordResetToken.builder().usedAt(Instant.now()).build();
        when(passwordResetTokenRepository.findByToken(anyString())).thenReturn(Optional.of(usedToken));

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> authService.resetPassword("used-token", "any-password"));
        assertThat(exception.getMessage()).isEqualTo("Auth password token invalid");
    }

    @Test
    @DisplayName("resetPassword should throw exception when token is expired")
    void resetPassword_whenTokenIsExpired_throwsException() {
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES)) // Expired!
                .build();
        when(passwordResetTokenRepository.findByToken(anyString())).thenReturn(Optional.of(expiredToken));

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> authService.resetPassword("expired-token", "any-password"));
        assertThat(exception.getMessage()).isEqualTo("Auth password token expired");
    }

    @Test
    @DisplayName("signUp should create and save a new user when email is not registered")
    void signUp_whenEmailIsNew_createsUserSuccessfully() {
        String email = "new.user@taskinn.com";
        String password = "password123";
        String hashedPassword = "hashedPassword123";

        when(employeeRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(hashedPassword);

        ArgumentCaptor<Employee> userCaptor = ArgumentCaptor.forClass(Employee.class);

        authService.signUp(email, password);

        verify(employeeRepository, times(1)).save(userCaptor.capture());
        Employee savedUser = userCaptor.getValue();

        assertAll(() -> assertThat(savedUser).isNotNull(), () -> assertThat(savedUser.getEmail()).isEqualTo(email),
                () -> assertThat(savedUser.getPasswordHash()).isEqualTo(hashedPassword));
    }

    @Test
    @DisplayName("signUp should throw IllegalArgumentException when email already exists")
    void signUp_whenEmailExists_throwsIllegalArgumentException() {
        String email = "existing.user@taskinn.com";
        String password = "password123";

        when(employeeRepository.existsByEmail(email)).thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> authService.signUp(email, password));

        assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Email already registered"),
                () -> verify(employeeRepository, never()).save(any(Employee.class)),
                () -> verify(passwordEncoder, never()).encode(anyString()));
    }

    @Test
    @DisplayName("signOut should revoke token when token is valid and not already revoked")
    void signOut_whenTokenIsValidAndNotRevoked_revokesToken() {
        String token = "valid.jwt.token";
        String jti = "unique-jti-123";
        String subject = "user@taskinn.com";
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

        JwtPrincipal principal = new JwtPrincipal(subject, jti, Instant.now(), expiresAt, Set.of(Roles.EMPLOYEE));

        when(jwtService.parseAndValidate(token)).thenReturn(Optional.of(principal));
        when(revokedTokenRepository.existsByJti(jti)).thenReturn(false);

        ArgumentCaptor<RevokedToken> tokenCaptor = ArgumentCaptor.forClass(RevokedToken.class);

        authService.signOut(token);

        verify(revokedTokenRepository, times(1)).save(tokenCaptor.capture());
        RevokedToken savedToken = tokenCaptor.getValue();

        assertAll(() -> assertThat(savedToken).isNotNull(), () -> assertThat(savedToken.getJti()).isEqualTo(jti),
                () -> assertThat(savedToken.getSubject()).isEqualTo(subject),
                () -> assertThat(savedToken.getExpiresAt()).isEqualTo(expiresAt));
    }

    @Test
    @DisplayName("signOut should not save token when it is already revoked")
    void signOut_whenTokenIsAlreadyRevoked_doesNotSaveAgain() {
        String token = "valid.jwt.token";
        String jti = "already-revoked-jti";

        JwtPrincipal principal = new JwtPrincipal("user@taskinn.com", jti, Instant.now(),
                Instant.now().plus(1, ChronoUnit.HOURS), Set.of(Roles.EMPLOYEE));

        when(jwtService.parseAndValidate(token)).thenReturn(Optional.of(principal));
        when(revokedTokenRepository.existsByJti(jti)).thenReturn(true);

        authService.signOut(token);

        verify(revokedTokenRepository, never()).save(any(RevokedToken.class));
    }

    @Test
    @DisplayName("signOut should do nothing when token is invalid")
    void signOut_whenTokenIsInvalid_doesNothing() {
        String invalidToken = "invalid.jwt.token";

        when(jwtService.parseAndValidate(invalidToken)).thenReturn(Optional.empty());

        authService.signOut(invalidToken);

        verify(revokedTokenRepository, never()).existsByJti(anyString());
        verify(revokedTokenRepository, never()).save(any(RevokedToken.class));
    }
}
