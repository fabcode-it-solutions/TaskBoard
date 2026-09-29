package tuwien.taskinn.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tuwien.taskinn.dto.PointDto;
import tuwien.taskinn.enums.RoomStatus;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequestDto {

    // INFO: ID is only used for updates; can be null for creation
    private Long id;

    @NotBlank
    private String roomNumber;

    @NotNull
    @Min(0)
    private Integer floor;

    @NotNull
    private RoomStatus status;

    @NotEmpty
    @Size(min = 3, message = "A room must have at least 3 points")
    @Valid
    private List<PointDto> svgPoints;

    @NotNull
    private boolean active;

    @NotNull
    private Long roomTypeId;

    // Override fields - null means inherit from RoomType
    @Min(1)
    private Integer capacityOverride;

    @Min(0)
    private BigDecimal basePriceOverride;

    @Min(0)
    private Integer singleBedOverride;

    @Min(0)
    private Integer doubleBedOverride;
}
