package tuwien.taskinn.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeOptionDto {

    private RoomTypeDto roomType;

    /**
     * Effective price per night for this option (room override if set, otherwise RoomType base price)
     */
    private BigDecimal pricePerNight;

    /**
     * Total number of rooms of this type and price available for the requested date range
     */
    private Integer availableCount;

    /**
     * Recommended number of rooms of this type and price to fulfill the guest requirements
     */
    private Integer recommendedCount;

    /**
     * Total price for the recommended count per night (pricePerNight * recommendedCount)
     */
    private BigDecimal totalPricePerNight;
}
