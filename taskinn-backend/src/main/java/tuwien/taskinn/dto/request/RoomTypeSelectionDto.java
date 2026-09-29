package tuwien.taskinn.dto.request;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeSelectionDto {

    @NotNull(message = "Room type ID is required")
    @Schema(description = "ID of the room type to book")
    private Long roomTypeId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Number of rooms of this type to book", minimum = "1")
    private Integer quantity;

    @Schema(description = "Price per night for this selection (optional, used when room prices are overridden)")
    private BigDecimal pricePerNight;

    public RoomTypeSelectionDto(Long roomTypeId, Integer quantity) {
        this.roomTypeId = roomTypeId;
        this.quantity = quantity;
    }
}
