package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tuwien.taskinn.dto.request.RoomOptionsRequestDto;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.services.validator.BookingValidator;

@DisplayName("BookingValidator Unit Tests")
public class BookingValidatorTest {

    private final BookingValidator bookingValidator = new BookingValidator();

    @Test
    @DisplayName("validateRoomOptionsRequestDto should not throw exception when check-out is after check-in")
    public void validateRoomOptionsRequestDto_whenCheckOutAfterCheckIn_doesNotThrow() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(2);
        Integer numRooms = 2;
        Integer numAdults = 4;
        Integer numChildren = 2;
        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        // When & Then
        assertDoesNotThrow(() -> bookingValidator.validateRoomOptionsRequestDto(requestDto));
    }

    @Test
    @DisplayName("validateRoomOptionsRequestDto should not throw exception when check-in and check-out are on same day")
    public void validateRoomOptionsRequestDto_whenCheckInEqualsCheckOut_doesNotThrow() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(1);
        Integer numRooms = 2;
        Integer numAdults = 4;
        Integer numChildren = 2;
        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        // When & Then
        assertDoesNotThrow(() -> bookingValidator.validateRoomOptionsRequestDto(requestDto));
    }

    @Test
    @DisplayName("validateRoomOptionsRequestDto should throw BusinessValidationException when check-in is after check-out")
    public void validateRoomOptionsRequestDto_whenCheckInAfterCheckOut_throwsBusinessValidationException() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(2);
        LocalDate checkOutDate = LocalDate.now().plusDays(1);
        Integer numRooms = 2;
        Integer numAdults = 4;
        Integer numChildren = 2;
        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> bookingValidator.validateRoomOptionsRequestDto(requestDto));

        assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Check-out date must be after check-in date"),
                () -> assertThat(exception.getField()).isEqualTo("checkOutDate"));
    }

    @Test
    @DisplayName("validateRoomOptionsRequestDto should not throw exception for past dates")
    public void validateRoomOptionsRequestDto_whenDatesAreInPast_doesNotThrow() {
        // Given
        LocalDate checkInDate = LocalDate.now().minusDays(2);
        LocalDate checkOutDate = LocalDate.now().minusDays(1);
        Integer numRooms = 2;
        Integer numAdults = 4;
        Integer numChildren = 2;
        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        // When & Then
        assertDoesNotThrow(() -> bookingValidator.validateRoomOptionsRequestDto(requestDto));
    }
}
