package tuwien.taskinn.dto;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tuwien.taskinn.enums.Roles;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeDto {

    @NotNull
    private Long id;

    @NotNull
    private String firstName;

    @NotNull
    private String lastName;

    @Email
    @NotNull
    private String email;

    @NotNull
    private boolean active;

    @Min(0)
    private BigDecimal hourlyWage;

    @Min(0)
    private BigDecimal hourlyOvertimeWage;

    @Min(0)
    private BigDecimal workingHoursPerWeek;

    @NotNull
    private Set<Roles> roles;
}
