package tuwien.taskinn.dto.request;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrUpdateBookingRequestDto {

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    @JsonFormat(pattern = "dd-MM-yyyy")
    @Schema(description = "Check-in date", example = "15-11-2025", format = "date")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    @JsonFormat(pattern = "dd-MM-yyyy")
    @Schema(description = "Check-out date", example = "20-11-2025", format = "date")
    private LocalDate checkOutDate;

    @NotNull(message = "Number of adults is required")
    @Min(value = 1, message = "Number of adults must be at least 1")
    @Schema(description = "Number of adult guests", minimum = "1", example = "2")
    private Integer numAdults;

    @NotNull(message = "Number of children is required")
    @Min(value = 0, message = "Number of children must be at least 0")
    @Schema(description = "Number of child guests", minimum = "0", example = "1")
    private Integer numChildren;

    @NotNull(message = "Guest information is required")
    @Valid
    @Schema(description = "Primary guest information")
    private GuestInfoDto guestInfo;

    @Valid
    @Schema(description = "List of selected room types with quantities (for customer booking)")
    private List<RoomTypeSelectionDto> roomTypeSelections;

    @Schema(description = "List of specific room IDs (for manual booking)")
    private List<Long> roomIds;

    @Schema(description = "Indicates if this booking was created manually by staff", example = "false")
    private Boolean createdManually;

    @Schema(description = "Request early check-in (adds 20€ fee)", example = "false")
    private Boolean earlyCheckIn;

    @Schema(description = "Request late check-out (adds 20€ fee)", example = "false")
    private Boolean lateCheckOut;
}
