package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tuwien.taskinn.dto.EmployeeDto;
import tuwien.taskinn.dto.request.SignUpRequestDto;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.services.validator.CreateOrUpdateAccountValidator;

@DisplayName("CreateOrUpdateAccountValidator Unit Tests")
public class CreateOrUpdateAccountValidatorTest {

    private final CreateOrUpdateAccountValidator validator = new CreateOrUpdateAccountValidator();

    @Test
    @DisplayName("Both validators should not throw exception when user is admin with zero hourly wage")
    public void validate_whenAdminWithZeroHourlyWage_doesNotThrow() {
        // Given
        SignUpRequestDto signUpDto = SignUpRequestDto.builder()
                .email("admin@example.com")
                .password("password")
                .firstName("John")
                .lastName("Doe")
                .hourlyWage(BigDecimal.ZERO)
                .workingHoursPerWeek(BigDecimal.ZERO)
                .roles(Set.of(Roles.ADMIN))
                .build();

        EmployeeDto employeeDto = EmployeeDto.builder()
                .id(1L)
                .email("admin@example.com")
                .firstName("John")
                .lastName("Doe")
                .active(true)
                .hourlyWage(BigDecimal.ZERO)
                .workingHoursPerWeek(BigDecimal.ZERO)
                .roles(Set.of(Roles.ADMIN))
                .build();

        // When & Then
        assertAll(() -> assertDoesNotThrow(() -> validator.validateSignUpRequestDto(signUpDto)),
                () -> assertDoesNotThrow(() -> validator.validateEmployeeDto(employeeDto)));
    }

    @Test
    @DisplayName("Both validators should not throw exception when non-admin has positive hourly wage and working hours")
    public void validate_whenNonAdminWithPositiveValues_doesNotThrow() {
        // Given
        SignUpRequestDto signUpDto = SignUpRequestDto.builder()
                .email("employee@example.com")
                .password("password")
                .firstName("Jane")
                .lastName("Smith")
                .hourlyWage(new BigDecimal("25.50"))
                .workingHoursPerWeek(new BigDecimal("40"))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        EmployeeDto employeeDto = EmployeeDto.builder()
                .id(2L)
                .email("employee@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .active(true)
                .hourlyWage(new BigDecimal("25.50"))
                .workingHoursPerWeek(new BigDecimal("40"))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        // When & Then
        assertAll(() -> assertDoesNotThrow(() -> validator.validateSignUpRequestDto(signUpDto)),
                () -> assertDoesNotThrow(() -> validator.validateEmployeeDto(employeeDto)));
    }

    @Test
    @DisplayName("Both validators should throw BusinessValidationException when non-admin has zero hourly wage")
    public void validate_whenNonAdminWithZeroHourlyWage_throwsBusinessValidationException() {
        // Given
        SignUpRequestDto signUpDto = SignUpRequestDto.builder()
                .email("employee@example.com")
                .password("password")
                .firstName("Jane")
                .lastName("Smith")
                .hourlyWage(BigDecimal.ZERO)
                .workingHoursPerWeek(new BigDecimal("40"))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        EmployeeDto employeeDto = EmployeeDto.builder()
                .id(2L)
                .email("employee@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .active(true)
                .hourlyWage(BigDecimal.ZERO)
                .workingHoursPerWeek(new BigDecimal("40"))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        // When & Then
        assertAll(() -> {
            BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                    () -> validator.validateSignUpRequestDto(signUpDto));
            assertThat(exception.getMessage()).isEqualTo("Hourly wage must be greater than 0 for non-admin users");
            assertThat(exception.getField()).isEqualTo("hourlyWage");
        }, () -> {
            BusinessValidationException exception =
                    assertThrows(BusinessValidationException.class, () -> validator.validateEmployeeDto(employeeDto));
            assertThat(exception.getMessage()).isEqualTo("Hourly wage must be greater than 0 for non-admin users");
            assertThat(exception.getField()).isEqualTo("hourlyWage");
        });
    }

    @Test
    @DisplayName("Both validators should throw BusinessValidationException when non-admin has zero working hours per week")
    public void validate_whenNonAdminWithZeroWorkingHours_throwsBusinessValidationException() {
        // Given
        SignUpRequestDto signUpDto = SignUpRequestDto.builder()
                .email("employee@example.com")
                .password("password")
                .firstName("Jane")
                .lastName("Smith")
                .hourlyWage(new BigDecimal("25.50"))
                .workingHoursPerWeek(BigDecimal.ZERO)
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        EmployeeDto employeeDto = EmployeeDto.builder()
                .id(2L)
                .email("employee@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .active(true)
                .hourlyWage(new BigDecimal("25.50"))
                .workingHoursPerWeek(BigDecimal.ZERO)
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        // When & Then
        assertAll(() -> {
            BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                    () -> validator.validateSignUpRequestDto(signUpDto));
            assertThat(exception.getMessage())
                    .isEqualTo("Working hours per week must be greater than 0 for non-admin users");
            assertThat(exception.getField()).isEqualTo("workingHoursPerWeek");
        }, () -> {
            BusinessValidationException exception =
                    assertThrows(BusinessValidationException.class, () -> validator.validateEmployeeDto(employeeDto));
            assertThat(exception.getMessage())
                    .isEqualTo("Working hours per week must be greater than 0 for non-admin users");
            assertThat(exception.getField()).isEqualTo("workingHoursPerWeek");
        });
    }

    @Test
    @DisplayName("Both validators should not throw exception when user has both admin and employee roles")
    public void validate_whenUserHasAdminAndEmployeeRoles_doesNotThrow() {
        // Given
        SignUpRequestDto signUpDto = SignUpRequestDto.builder()
                .email("multi@example.com")
                .password("password")
                .firstName("Multi")
                .lastName("Role")
                .hourlyWage(BigDecimal.ZERO)
                .workingHoursPerWeek(BigDecimal.ZERO)
                .roles(Set.of(Roles.ADMIN, Roles.EMPLOYEE))
                .build();

        EmployeeDto employeeDto = EmployeeDto.builder()
                .id(3L)
                .email("multi@example.com")
                .firstName("Multi")
                .lastName("Role")
                .active(true)
                .hourlyWage(BigDecimal.ZERO)
                .workingHoursPerWeek(BigDecimal.ZERO)
                .roles(Set.of(Roles.ADMIN, Roles.EMPLOYEE))
                .build();

        // When & Then
        assertAll(() -> assertDoesNotThrow(() -> validator.validateSignUpRequestDto(signUpDto)),
                () -> assertDoesNotThrow(() -> validator.validateEmployeeDto(employeeDto)));
    }
}
