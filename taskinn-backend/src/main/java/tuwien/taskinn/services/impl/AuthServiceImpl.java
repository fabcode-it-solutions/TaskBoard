package tuwien.taskinn.services.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tuwien.taskinn.dto.request.SignUpRequestDto;
import tuwien.taskinn.dto.response.SignInResponseDto;
import tuwien.taskinn.dto.response.UserProfileDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.PasswordResetToken;
import tuwien.taskinn.entity.RevokedToken;
import tuwien.taskinn.exceptions.AuthenticationFailedException;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.PasswordResetTokenRepository;
import tuwien.taskinn.repo.RevokedTokenRepository;
import tuwien.taskinn.services.AuthService;
import tuwien.taskinn.services.EmailService;
import tuwien.taskinn.services.JwtService;
import tuwien.taskinn.services.validator.CreateOrUpdateAccountValidator;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final CreateOrUpdateAccountValidator createOrUpdateAccountValidator;

    @Transactional
    public void signUp(String email, String password) {
        if (employeeRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        Employee user = Employee.builder().email(email).passwordHash(passwordEncoder.encode(password)).build();
        employeeRepository.save(user);
    }

    @Transactional
    public void signUpEmployee(SignUpRequestDto signUpRequestDto) {
        if (employeeRepository.existsByEmail(signUpRequestDto.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        createOrUpdateAccountValidator.validateSignUpRequestDto(signUpRequestDto);

        Employee user = Employee.builder()
                .firstName(signUpRequestDto.getFirstName())
                .lastName(signUpRequestDto.getLastName())
                .active(true)
                .email(signUpRequestDto.getEmail())
                .passwordHash(passwordEncoder.encode(signUpRequestDto.getPassword()))
                .hourlyWage(signUpRequestDto.getHourlyWage())
                .hourlyOvertimeWage(signUpRequestDto.getHourlyOvertimeWage())
                .workingHoursPerWeek(signUpRequestDto.getWorkingHoursPerWeek())
                .roles(signUpRequestDto.getRoles())

                .build();
        employeeRepository.save(user);
    }

    public SignInResponseDto signIn(String email, String password) {
        Employee user = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!user.isActive()) {
            throw new AuthenticationFailedException("User account is inactive");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        String token = jwtService.generateAccessToken(user.getEmail(), user.getRoles());
        return new SignInResponseDto(token, "Bearer", jwtService.getAccessTtlSeconds());
    }

    @Transactional
    public void signOut(String token) {
        jwtService.parseAndValidate(token).ifPresent(principal -> {
            if (!revokedTokenRepository.existsByJti(principal.jti())) {
                RevokedToken rt = RevokedToken.builder()
                        .jti(principal.jti())
                        .subject(principal.subject())
                        .expiresAt(principal.expiresAt())
                        .build();
                revokedTokenRepository.save(rt);
            }
        });
    }

    @Transactional
    public void forgotPassword(String email) {
        Optional<Employee> userOpt = employeeRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Always return OK to avoid email enumeration
            return;
        }
        Employee user = userOpt.get();
        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = PasswordResetToken.builder()
                .token(token)
                .employee(user)
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .build();
        passwordResetTokenRepository.save(prt);
        this.emailService.sendResetPasswordEmail(email, token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Auth password token invalid"));
        if (prt.getUsedAt() != null) {
            throw new IllegalArgumentException("Auth password token invalid");
        }
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Auth password token expired");
        }
        Employee employee = prt.getEmployee();
        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);
        prt.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(prt);
    }

    @Override
    public UserProfileDto getUserProfile(String email) {

        Employee user =
                employeeRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<String> roleNames = user.getRoles()
                .stream()
                .map(Enum::name) // or role -> role.name()
                .collect(Collectors.toSet());

        return UserProfileDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .roles(roleNames)
                .build();
    }
}
