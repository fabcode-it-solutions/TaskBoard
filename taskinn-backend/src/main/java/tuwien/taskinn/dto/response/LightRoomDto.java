package tuwien.taskinn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tuwien.taskinn.enums.RoomStatus;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LightRoomDto {

    private Long id;
    private String roomNumber;
    private Integer floor;
    private boolean active;
    private RoomStatus status;
    private RoomTypeInfo roomType;

    // Effective values (override if set, otherwise inherited from RoomType)
    private Integer effectiveCapacity;
    private BigDecimal effectiveBasePrice;
    private Integer effectiveSingleBed;
    private Integer effectiveDoubleBed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomTypeInfo {
        private Long id;
        private String name;
        private Integer capacity;
        private BigDecimal basePrice;
        private Integer singleBed;
        private Integer doubleBed;
    }
}
