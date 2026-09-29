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
public class RoomTypeDto {

    private Long id;

    private String name;

    private String description;

    private Integer capacity;

    private BigDecimal basePrice;

    private Integer singleBed;

    private Integer doubleBed;
}
