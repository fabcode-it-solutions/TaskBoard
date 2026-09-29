package tuwien.taskinn.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tuwien.taskinn.enums.BookingStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSummaryResponseDto {

    @Schema(description = "The unique ID of the booking.")
    private Long id;

    @Schema(description = "Full name of the primary guest.", example = "John Doe")
    private String guestName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Check-in date for the booking.", example = "2025-11-15")
    private LocalDate checkInDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Check-out date for the booking.", example = "2025-11-20")
    private LocalDate checkOutDate;

    @Schema(description = "The number of rooms in this booking.", example = "2")
    private int roomCount;

    @Schema(description = "The current status of the booking.", example = "CONFIRMED")
    private BookingStatus status;
}
