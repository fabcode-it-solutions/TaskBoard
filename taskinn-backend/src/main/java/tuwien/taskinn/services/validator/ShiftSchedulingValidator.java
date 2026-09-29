package tuwien.taskinn.services.validator;

import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.EmployeeMonthShiftDto;
import tuwien.taskinn.dto.SchedulingRoleDto;
import tuwien.taskinn.dto.ShiftScheduleTemplateDto;
import tuwien.taskinn.dto.request.ShiftScheduleSolveRequestDto;
import tuwien.taskinn.exceptions.BusinessValidationException;

@Component
public class ShiftSchedulingValidator {

    public void validateYearAndMonth(Integer year, Integer month) {
        if (year == null) {
            throw new BusinessValidationException("year", "Year must not be null");
        } else if (year < 2014 || year > 9999) {
            throw new BusinessValidationException("year", "Year is not in the expected range 2014-9999");
        }

        if (month == null) {
            throw new BusinessValidationException("month", "Month must not be null");
        } else if (month <= 0 || month > 12) {
            throw new BusinessValidationException("month", "Month not in the expected range 1-12");
        }
    }

    public void validateEmployeeMonthShiftForPut(long daysInMonth, EmployeeMonthShiftDto dto) {
        if (dto.getEmployee().getId() == null) {
            throw new BusinessValidationException("employee.id", "Employee ID must not be null");
        }
        for (final int dayIndex : dto.getDayIndices()) {
            if (dayIndex < 0 || dayIndex >= daysInMonth) {
                throw new BusinessValidationException("dayIndices", "Day index out of range");
            }
        }
    }

    public void validateShiftScheduleSolveRequest(ShiftScheduleSolveRequestDto dto) {
        validateYearAndMonth(dto.getYear(), dto.getMonth());

        for (final int dailyHours : dto.getDailyHours()) {
            if (dailyHours < 0 || dailyHours > 24) {
                throw new BusinessValidationException("dailyHours", "Daily hours must be in range 0-24");
            }
        }

        for (final int staffingLevel : dto.getStaffingLevels()) {
            if (staffingLevel < 0) {
                throw new BusinessValidationException("staffingLevel", "Staffing level must not be negative");
            }
        }
    }

    public void validateSchedulingRole(SchedulingRoleDto dto) {
        if (dto.getName() == null || dto.getName().isEmpty()) {
            throw new BusinessValidationException("name", "Scheduling role name cannot be null or empty");
        }
    }

    public void validateShiftScheduleTemplate(ShiftScheduleTemplateDto dto) {
        if (dto.getName() == null || dto.getName().isEmpty()) {
            throw new BusinessValidationException("name", "Shift scheduling template name cannot be null or empty");
        }
    }

}
