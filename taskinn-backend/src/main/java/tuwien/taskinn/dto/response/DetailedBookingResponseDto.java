package tuwien.taskinn.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tuwien.taskinn.enums.BookingStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedBookingResponseDto {

    @Schema(description = "The unique ID of the booking.")
    private Long bookingId;

    @Schema(description = "The current status of the booking.", example = "CONFIRMED")
    private BookingStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Check-in date for the booking.", example = "2025-11-15")
    private LocalDate checkInDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Check-out date for the booking.", example = "2025-11-20")
    private LocalDate checkOutDate;

    @Schema(description = "Number of adults in the booking.", example = "2")
    private Integer numAdults;

    @Schema(description = "Number of children in the booking.", example = "1")
    private Integer numChildren;

    @Schema(description = "Total price calculated for the booking stay.")
    private BigDecimal totalPrice;

    @Schema(description = "Information about the primary guest for this booking.")
    private GuestInfo primaryGuest;

    @Schema(description = "A list of all rooms assigned to this booking.")
    private List<RoomInfo> rooms;

    @Schema(description = "A list of all registered guests currently checked into the rooms. "
            + "This field is ONLY populated for bookings with status 'CHECKED_IN'.")
    private List<CheckedInGuestInfo> checkedInGuests;

    @Schema(description = "Timestamp of when the booking was created.")
    private Instant createdAt;

    @Schema(description = "Indicates if early check-in was requested (adds 20€ fee).")
    private Boolean earlyCheckIn;

    @Schema(description = "Indicates if late check-out was requested (adds 20€ fee).")
    private Boolean lateCheckOut;

    /**
     * A simplified DTO representing the primary guest of the booking.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuestInfo {
        @Schema(description = "The unique ID of the guest.")
        private Long id;
        @Schema(description = "Guest's first name.", example = "John")
        private String firstName;
        @Schema(description = "Guest's last name.", example = "Doe")
        private String lastName;
        @Schema(description = "Guest's date of birth.", example = "1990-05-15")
        private LocalDate dateOfBirth;
        @Schema(description = "Guest's email address.", example = "john.doe@example.com")
        private String email;
        @Schema(description = "Guest's phone number.", example = "+43123456789")
        private String phone;
    }

    /**
     * A simplified DTO representing a room assigned to the booking.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomInfo {
        @Schema(description = "The unique ID of the room.")
        private Long id;
        @Schema(description = "The room number.", example = "101")
        private String roomNumber;
        @Schema(description = "The name of the room type.", example = "Deluxe Double")
        private String roomTypeName;
        @Schema(description = "The maximum capacity of the room.", example = "2")
        private Integer capacity;
    }

    /**
     * Represents a guest who has been registered during the check-in process. This includes a link to the specific room
     * they are staying in.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckedInGuestInfo {
        @Schema(description = "The unique ID of the guest record.")
        private Long id;
        @Schema(description = "Guest's first name.", example = "Jane")
        private String firstName;
        @Schema(description = "Guest's last name.", example = "Smith")
        private String lastName;
    }
}
