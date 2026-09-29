package tuwien.taskinn.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class RoomOptionsRequestDto {

    @NotNull
    @FutureOrPresent
    @JsonFormat(pattern = "dd-MM-yyyy")
    @Schema(example = "15-11-2025", format = "date")
    private LocalDate checkInDate;

    @NotNull
    @Future
    @JsonFormat(pattern = "dd-MM-yyyy")
    @Schema(example = "20-11-2025", format = "date")
    private LocalDate checkOutDate;

    @NotNull
    @Min(1)
    @Schema(minimum = "1")
    private Integer numRooms;

    @NotNull
    @Min(1)
    @Schema(minimum = "1")
    private Integer numAdults;

    @NotNull
    @Min(0)
    @Schema(minimum = "0")
    private Integer numChildren;
}
