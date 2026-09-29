package tuwien.taskinn.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaircaseRequestDto {

    private Long id;

    @NotNull
    @Min(0)
    private Integer floor;

    @NotNull
    private Double x;

    @NotNull
    private Double y;

    private String name;

    @NotNull
    private Boolean active;
}
