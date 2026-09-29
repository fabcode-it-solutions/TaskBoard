package tuwien.taskinn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaircaseDto {
    private Long id;
    private Integer floor;
    private Double x;
    private Double y;
    private String name;
    private boolean active;
}
