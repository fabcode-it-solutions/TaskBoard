package tuwien.taskinn.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tuwien.taskinn.dto.EmployeeMonthShiftDto;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShiftSchedulePutRequestDto {

    @NotNull
    @Min(2024)
    private Integer year;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    @NotNull
    private List<EmployeeMonthShiftDto> shifts;

}
