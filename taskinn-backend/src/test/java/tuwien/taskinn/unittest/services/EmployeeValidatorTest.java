package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tuwien.taskinn.dto.request.CreatePtoRequest;
import tuwien.taskinn.entity.PTO;
import tuwien.taskinn.entity.Shift;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.services.validator.EmployeeValidator;

@DisplayName("EmployeeValidator Unit Tests")
public class EmployeeValidatorTest {

    private final EmployeeValidator validator = new EmployeeValidator();

    @Test
    @DisplayName("Should throw exception when start date is after end date")
    void validateCreatePtoRequest_whenStartAfterEnd_throwsException() {
        CreatePtoRequest request = CreatePtoRequest.builder()
                .startDate(LocalDate.of(2026, 1, 20))
                .endDate(LocalDate.of(2026, 1, 10))
                .build();

        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> validator.validateCreatePtoRequestDto(request, Collections.emptyList(), Collections.emptyList()));

        assertThat(exception.getField()).isEqualTo("startDate");
        assertThat(exception.getMessage()).contains("Start date must be before end date");
    }

    @Test
    @DisplayName("Should throw exception when PTO overlaps existing PTO")
    void validateCreatePtoRequest_whenOverlapsExistingPto_throwsException() {
        CreatePtoRequest request = CreatePtoRequest.builder()
                .startDate(LocalDate.of(2026, 1, 10))
                .endDate(LocalDate.of(2026, 1, 12))
                .build();

        PTO existingPto = PTO.builder().startDate(LocalDate.of(2026, 1, 11)).endDate(LocalDate.of(2026, 1, 15)).build();

        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> validator.validateCreatePtoRequestDto(request, List.of(existingPto), Collections.emptyList()));

        assertThat(exception.getField()).isEqualTo("endDate");
        assertThat(exception.getMessage()).contains("overlaps with an existing PTO");
    }

    @Test
    @DisplayName("Should throw exception when PTO overlaps scheduled shift")
    void validateCreatePtoRequest_whenOverlapsShift_throwsException() {
        CreatePtoRequest request = CreatePtoRequest.builder()
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 2, 3))
                .build();

        Shift existingShift = Shift.builder().date(LocalDateTime.of(2026, 2, 2, 9, 0)).build();

        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> validator.validateCreatePtoRequestDto(request, Collections.emptyList(), List.of(existingShift)));

        assertThat(exception.getField()).isEqualTo("startDate");
        assertThat(exception.getMessage()).contains("scheduled to work");
    }

    @Test
    @DisplayName("Should not throw exception when PTO does not overlap")
    void validateCreatePtoRequest_whenNoOverlaps_doesNotThrow() {
        CreatePtoRequest request = CreatePtoRequest.builder()
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 5))
                .build();

        PTO existingPto = PTO.builder().startDate(LocalDate.of(2026, 3, 10)).endDate(LocalDate.of(2026, 3, 12)).build();

        Shift existingShift = Shift.builder().date(LocalDateTime.of(2026, 3, 20, 9, 0)).build();

        assertDoesNotThrow(
                () -> validator.validateCreatePtoRequestDto(request, List.of(existingPto), List.of(existingShift)));
    }
}
