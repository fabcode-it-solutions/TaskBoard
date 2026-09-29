package tuwien.taskinn.unittest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tuwien.taskinn.dto.EmployeeDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.mapper.EmployeeMapper;

@DisplayName("EmployeeMapper Unit Tests")
public class EmployeeMapperTest {

    private final EmployeeMapper employeeMapper = new EmployeeMapper();

    @Test
    @DisplayName("toDto should correctly map Employee entity to EmployeeDto")
    public void toDto_whenValidEmployee_correctlyMapsToDto() {
        Employee employee = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25.50))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .passwordHash("hashedPassword")
                .build();

        EmployeeDto result = employeeMapper.toDto(employee);

        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result.getId()).isEqualTo(1L),
                () -> assertThat(result.getFirstName()).isEqualTo("John"),
                () -> assertThat(result.getLastName()).isEqualTo("Doe"),
                () -> assertThat(result.getEmail()).isEqualTo("john.doe@taskinn.com"),
                () -> assertThat(result.isActive()).isTrue(),
                () -> assertThat(result.getHourlyWage()).usingComparator(BigDecimal::compareTo)
                        .isEqualTo(BigDecimal.valueOf(25.50)),
                () -> assertThat(result.getWorkingHoursPerWeek()).usingComparator(BigDecimal::compareTo)
                        .isEqualTo(BigDecimal.valueOf(40)),
                () -> assertThat(result.getRoles()).contains(Roles.EMPLOYEE));
    }

    @Test
    @DisplayName("toDto should return null when employee is null")
    public void toDto_whenEmployeeIsNull_returnsNull() {
        EmployeeDto result = employeeMapper.toDto(null);

        assertThat(result).isNull();
    }
}
