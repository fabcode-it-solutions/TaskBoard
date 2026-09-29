package tuwien.taskinn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FloorOccupancyDto {

    private Integer floor;

    private Long totalRooms;

    private Long occupiedRooms;

    private Double occupancyRate;
}
