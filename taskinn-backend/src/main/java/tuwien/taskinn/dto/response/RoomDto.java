package tuwien.taskinn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tuwien.taskinn.dto.PointDto;
import tuwien.taskinn.enums.RoomStatus;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDto {

    private Long id;

    private String number;

    private Integer floor;

    private RoomStatus status;

    private List<PointDto> svgPoints;

    private boolean active;

    private Long roomTypeId;

    // Override fields - null means inherit from RoomType
    private Integer capacityOverride;

    private BigDecimal basePriceOverride;

    private Integer singleBedOverride;

    private Integer doubleBedOverride;
}
