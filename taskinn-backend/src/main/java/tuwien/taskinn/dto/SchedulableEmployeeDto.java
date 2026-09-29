package tuwien.taskinn.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represent employee information, condensed for scheduling purposes.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SchedulableEmployeeDto {

    @NotNull
    private Long id;

    private String firstName;

    private String lastName;

    private Long schedulingRoleId;

    private String schedulingRoleName;

    private BigDecimal hourlyWage;

    private BigDecimal hourlyOvertimeWage;

    private BigDecimal workingHoursPerWeek;

}
