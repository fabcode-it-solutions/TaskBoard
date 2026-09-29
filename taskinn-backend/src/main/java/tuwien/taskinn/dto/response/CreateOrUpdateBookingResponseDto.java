package tuwien.taskinn.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrUpdateBookingResponseDto {

    @Schema(description = "Booking ID")
    private Long bookingId;

    @JsonFormat(pattern = "dd-MM-yyyy")
    @Schema(description = "Check-in date", example = "15-11-2025")
    private LocalDate checkInDate;

    @JsonFormat(pattern = "dd-MM-yyyy")
    @Schema(description = "Check-out date", example = "20-11-2025")
    private LocalDate checkOutDate;

    @Schema(description = "List of room numbers assigned to this booking")
    private List<String> assignedRoomNumbers;

    @Schema(description = "Booking status", example = "CONFIRMED")
    private String status;

    @Schema(description = "Guest's email")
    private String guestEmail;

    @Schema(description = "Early check-in requested")
    private Boolean earlyCheckIn;

    @Schema(description = "Late check-out requested")
    private Boolean lateCheckOut;
}
