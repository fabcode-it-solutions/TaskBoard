package tuwien.taskinn.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an employee's shift for a given month.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeMonthShiftDto {

    private SchedulableEmployeeDto employee;

    /** Scheduled days, given as indices into the given month (first day of month is 0) */
    private List<Integer> dayIndices;

    /** PTO requests, given as indices into the given month (first day of month is 0) */
    private List<Integer> ptoIndices;

}
