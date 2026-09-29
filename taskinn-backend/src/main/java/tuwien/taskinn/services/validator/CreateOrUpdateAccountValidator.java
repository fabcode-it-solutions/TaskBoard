package tuwien.taskinn.services.validator;

import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.EmployeeDto;
import tuwien.taskinn.dto.request.SignUpRequestDto;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.exceptions.BusinessValidationException;

import java.math.BigDecimal;

@Component
public class CreateOrUpdateAccountValidator {

    public void validateSignUpRequestDto(SignUpRequestDto signUpRequestDto) {
        if (!(signUpRequestDto.getRoles().contains(Roles.ADMIN))
                && signUpRequestDto.getHourlyWage().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("hourlyWage",
                    "Hourly wage must be greater than 0 for non-admin users");
        }
        if (!(signUpRequestDto.getRoles().contains(Roles.ADMIN))
                && signUpRequestDto.getWorkingHoursPerWeek().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("workingHoursPerWeek",
                    "Working hours per week must be greater than 0 for non-admin users");
        }
    }

    public void validateEmployeeDto(EmployeeDto employeeDto) {
        if (!(employeeDto.getRoles().contains(Roles.ADMIN))
                && employeeDto.getHourlyWage().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("hourlyWage",
                    "Hourly wage must be greater than 0 for non-admin users");
        }
        if (!(employeeDto.getRoles().contains(Roles.ADMIN))
                && employeeDto.getWorkingHoursPerWeek().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("workingHoursPerWeek",
                    "Working hours per week must be greater than 0 for non-admin users");
        }
    }
}
