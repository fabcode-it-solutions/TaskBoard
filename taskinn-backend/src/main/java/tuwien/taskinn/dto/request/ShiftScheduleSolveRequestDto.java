package tuwien.taskinn.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for optimizing a shift schedule for the given month/employees.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftScheduleSolveRequestDto {

    @NotNull
    @Min(2024)
    private Integer year;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    /** All employees to be considered for the resulting shift schedule, identified by their IDs */
    @NotNull
    @NotEmpty
    private List<Long> employeeIds;

    /** Target hours for every day of the month */
    @NotNull
    private List<Integer> dailyHours;

    /** Number of staff members required for every day of the month */
    @NotNull
    private List<Integer> staffingLevels;

}
