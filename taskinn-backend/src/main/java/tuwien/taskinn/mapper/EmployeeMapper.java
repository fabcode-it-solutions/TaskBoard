package tuwien.taskinn.mapper;

import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.EmployeeDto;
import tuwien.taskinn.dto.SchedulableEmployeeDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.SchedulingRole;

/**
 * Mapper for converting between Employee entity and EmployeeDto.
 */
@Component
public class EmployeeMapper {

    /**
     * Converts an Employee entity to an EmployeeDto.
     *
     * @param employee the employee entity to convert
     * @return the converted EmployeeDto
     */
    public EmployeeDto toDto(Employee employee) {
        if (employee == null) {
            return null;
        }

        return EmployeeDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .hourlyWage(employee.getHourlyWage())
                .hourlyOvertimeWage(employee.getHourlyOvertimeWage())
                .workingHoursPerWeek(employee.getWorkingHoursPerWeek())
                .active(employee.isActive())
                .roles(employee.getRoles())
                .build();
    }

    public SchedulableEmployeeDto toSchedulableDto(Employee employee) {
        if (employee == null) {
            return null;
        }

        final SchedulingRole schedulingRole = employee.getSchedulingRole();

        return SchedulableEmployeeDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .schedulingRoleId(schedulingRole != null ? schedulingRole.getId() : null)
                .schedulingRoleName(schedulingRole != null ? schedulingRole.getRoleName() : null)
                .hourlyWage(employee.getHourlyWage())
                .hourlyOvertimeWage(employee.getHourlyOvertimeWage())
                .workingHoursPerWeek(employee.getWorkingHoursPerWeek())
                .build();
    }
}
