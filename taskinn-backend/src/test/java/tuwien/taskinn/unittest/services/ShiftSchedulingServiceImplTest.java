package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tuwien.taskinn.dto.EmployeeMonthShiftDto;
import tuwien.taskinn.dto.SchedulableEmployeeDto;
import tuwien.taskinn.dto.SchedulingRoleDto;
import tuwien.taskinn.dto.request.ShiftScheduleSolveRequestDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.PTO;
import tuwien.taskinn.entity.SchedulingRole;
import tuwien.taskinn.entity.Shift;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.exceptions.InfeasibleInstanceException;
import tuwien.taskinn.mapper.EmployeeMapper;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.PTORepository;
import tuwien.taskinn.repo.SchedulingRoleRepository;
import tuwien.taskinn.repo.ShiftRepository;
import tuwien.taskinn.repo.ShiftScheduleTemplateRepository;
import tuwien.taskinn.services.impl.ShiftSchedulingServiceImpl;
import tuwien.taskinn.services.validator.ShiftSchedulingValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShiftSchedulingServiceImpl Unit Tests")
public class ShiftSchedulingServiceImplTest {

    @Spy
    private EmployeeMapper employeeMapper = new EmployeeMapper();

    @Mock
    private EmployeeRepository employeeRepository;

    @Spy
    private ShiftSchedulingValidator shiftSchedulingValidator = new ShiftSchedulingValidator();

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private PTORepository unavailabilityRepository;

    @Mock
    private SchedulingRoleRepository roleRepository;

    @Mock
    private ShiftScheduleTemplateRepository scheduleTemplateRepository;

    @InjectMocks
    private ShiftSchedulingServiceImpl shiftSchedulingService;

    @Test
    public void getSchedulableEmployees_whenSchedulableEmployeesExist_returnsAllSchedulableEmployees() {
        SchedulingRole schedulingRole = SchedulingRole.builder().id(1L).roleName("Test Role").build();
        Employee employee1 = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .schedulingRole(schedulingRole)
                .build();
        Employee employee2 = Employee.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        when(employeeRepository.findAllSchedulableEmployees()).thenReturn(List.of(employee1, employee2));

        List<SchedulableEmployeeDto> result = shiftSchedulingService.getSchedulableEmployees();

        assertAll(() -> assertThat(result).hasSize(2),
                () -> assertThat(result.contains(employeeMapper.toSchedulableDto(employee1))).isTrue(),
                () -> assertThat(result.contains(employeeMapper.toSchedulableDto(employee2))).isTrue());

    }

    @Test
    public void getSchedulableEmployees_whenNoSchedulableEmployeesExist_returnEmptyList() {
        when(employeeRepository.findAllSchedulableEmployees()).thenReturn(List.of());

        List<SchedulableEmployeeDto> result = shiftSchedulingService.getSchedulableEmployees();

        assertAll(() -> assertThat(result).hasSize(0));
    }

    @Test
    public void getShiftsForMonth_whenShiftsExist_returnsShiftsAndPTOs() {
        Employee employee1 = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();
        Shift shift1 = Shift.builder()
                .id(1L)
                .date(LocalDateTime.of(2026, 6, 2, 0, 0))
                .department("")
                .employee(employee1)
                .build();
        Shift shift2 = Shift.builder()
                .id(2L)
                .date(LocalDateTime.of(2026, 6, 10, 0, 0))
                .department("")
                .employee(employee1)
                .build();
        Shift shift3 = Shift.builder()
                .id(3L)
                .date(LocalDateTime.of(2026, 6, 16, 0, 0))
                .department("")
                .employee(employee1)
                .build();
        PTO unavailability = PTO.builder()
                .id(1L)
                .startDate(LocalDate.of(2026, 6, 20))
                .endDate(LocalDate.of(2026, 6, 25))
                .employee(employee1)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.ofNullable(employee1));
        when(shiftRepository.findAllShiftsInTimeframe(any(), any())).thenReturn(List.of(shift1, shift2, shift3));
        when(unavailabilityRepository.findAllPTOsInTimeframe(any(), any())).thenReturn(List.of(unavailability));

        List<EmployeeMonthShiftDto> result = shiftSchedulingService.getShiftsForMonth(2026, 6);

        assertAll(() -> assertThat(result).hasSize(1), () -> {
            final var result0 = result.get(0);
            assertThat(result0.getEmployee().getId()).isEqualTo(employee1.getId());
            assertThat(result0.getDayIndices().containsAll(List.of(1, 9, 15)));
            assertThat(result0.getPtoIndices().containsAll(List.of(19, 20, 21, 22, 23, 24)));
        });
    }

    @Test
    public void putShiftSchedule_whenValid_replacesMonthlyShiftSchedule() {
        Employee employee1 = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();
        EmployeeMonthShiftDto monthShiftDto = EmployeeMonthShiftDto.builder()
                .employee(employeeMapper.toSchedulableDto(employee1))
                .dayIndices(List.of(1, 4, 10))
                .ptoIndices(List.of())
                .build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.ofNullable(employee1));

        shiftSchedulingService.putShiftSchedule(2026, 6, List.of(monthShiftDto));

        assertAll(
                () -> verify(shiftRepository, times(1))
                        .deleteAllShiftsOfEmployeesInTimeframe(argThat(x -> x.contains(1L)), any(), any()),
                () -> verify(shiftRepository, times(3)).save(argThat(x -> {
                    final var date = x.getDate();
                    return date.getYear() == 2026 && date.getMonthValue() == 6
                            && (date.getDayOfMonth() == 2 || date.getDayOfMonth() == 5 || date.getDayOfMonth() == 11);
                })));
    }

    @Test
    public void solveShiftScheduling_whenValidAndFeasible_returnsShiftSchedule() {
        Employee employee1 = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();
        Employee employee2 = Employee.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        PTO unavailability = PTO.builder()
                .id(1L)
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 11))
                .employee(employee1)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.ofNullable(employee1));
        when(employeeRepository.findById(2L)).thenReturn(Optional.ofNullable(employee2));
        when(unavailabilityRepository.findEmployeePTOsInTimeframe(argThat(x -> x.contains(1L)), any(), any()))
                .thenReturn(List.of(unavailability));

        final ShiftScheduleSolveRequestDto request = ShiftScheduleSolveRequestDto.builder()
                .year(2025)
                .month(1)
                .employeeIds(List.of(1L, 2L))
                .dailyHours(List.of(10, 10, 10, 10, 10, 7, 7, 10, 10, 10, 10, 10, 7, 7, 10, 10, 10, 10, 10, 7, 7, 10,
                        10, 10, 10, 10, 7, 7, 10, 10, 10))
                .staffingLevels(List.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1))
                .build();

        final List<EmployeeMonthShiftDto> result = shiftSchedulingService.solveShiftScheduling(request);
        final Map<Long, List<Integer>> employeeAssignmentMap = result.stream()
                .collect(Collectors.toMap(x -> x.getEmployee().getId(), EmployeeMonthShiftDto::getDayIndices));

        assertAll(() -> assertThat(result).hasSize(2), () -> assertThat(employeeAssignmentMap).containsKeys(1L, 2L),
                () -> assertThat(employeeAssignmentMap.get(1L)).doesNotContain(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void solveShiftScheduling_whenValidAndInfeasible_throwsException() {
        Employee employee1 = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.ofNullable(employee1));
        when(unavailabilityRepository.findEmployeePTOsInTimeframe(any(), any(), any())).thenReturn(List.of());

        final ShiftScheduleSolveRequestDto request = ShiftScheduleSolveRequestDto.builder()
                .year(2025)
                .month(1)
                .employeeIds(List.of(1L))
                .dailyHours(List.of(10, 10, 10, 10, 10, 7, 7, 10, 10, 10, 10, 10, 7, 7, 10, 10, 10, 10, 10, 7, 7, 10,
                        10, 10, 10, 10, 7, 7, 10, 10, 10))
                .staffingLevels(List.of(2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1))
                .build();

        assertAll(() -> assertThrows(InfeasibleInstanceException.class,
                () -> shiftSchedulingService.solveShiftScheduling(request)));
    }

    @Test
    public void getAllSchedulingRoles_returnsAllSchedulingRoles() {
        SchedulingRole role1 = SchedulingRole.builder().id(1L).roleName("Role One").build();
        SchedulingRole role2 = SchedulingRole.builder().id(2L).roleName("Role Two").build();

        Employee employee1 = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .schedulingRole(role1)
                .build();
        Employee employee2 = Employee.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .schedulingRole(role2)
                .build();

        when(roleRepository.findAll()).thenReturn(List.of(role1, role2));
        when(employeeRepository.findAllEmployeesWithSchedulingRole(1L)).thenReturn(List.of(employee1));
        when(employeeRepository.findAllEmployeesWithSchedulingRole(2L)).thenReturn(List.of(employee2));

        final List<SchedulingRoleDto> result = shiftSchedulingService.getAllSchedulingRoles();
        final var resultMap = result.stream().collect(Collectors.toMap(SchedulingRoleDto::getId, x -> x));

        assertAll(() -> assertThat(resultMap).containsKeys(1L, 2L),
                () -> assertThat(resultMap.get(1L).getEmployees()).extracting(SchedulableEmployeeDto::getId)
                        .contains(1L),
                () -> assertThat(resultMap.get(2L).getEmployees()).extracting(SchedulableEmployeeDto::getId)
                        .contains(2L));
    }

    @Test
    public void deleteSchedulingRole_whenValid_removesReferencesFromAffectedEmployeesAndDeletesRole() {
        Employee employee1 = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();
        Employee employee2 = Employee.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        doNothing().when(roleRepository).deleteById(1L);
        when(employeeRepository.findAllEmployeesWithSchedulingRole(1L)).thenReturn(List.of(employee1, employee2));

        shiftSchedulingService.deleteSchedulingRole(1L);

        assertAll(
                () -> verify(employeeRepository, times(2))
                        .save(argThat(x -> (x.getId() == 1L || x.getId() == 2L) && x.getSchedulingRole() == null)),
                () -> verify(roleRepository, times(1)).deleteById(1L));
    }

    @Test
    public void updateSchedulingRole_whenValid_updatesRoleAndReferencesFromEmployees() {
        SchedulingRole role = SchedulingRole.builder().id(1L).roleName("Role One").build();

        Employee employee1 = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .schedulingRole(role)
                .build();
        Employee employee2 = Employee.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.save(any())).thenReturn(role);
        when(employeeRepository.findAllEmployeesWithSchedulingRole(1L)).thenReturn(List.of(employee1));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee2));

        final SchedulingRoleDto request = SchedulingRoleDto.builder()
                .id(1L)
                .name("New Role Name")
                .employees(List.of(SchedulableEmployeeDto.builder().id(2L).build()))
                .build();
        shiftSchedulingService.updateSchedulingRole(1L, request);

        assertAll(
                () -> verify(employeeRepository, times(2))
                        .save(argThat(x -> (x.getId() == 1L && x.getSchedulingRole() == null)
                                || (x.getId() == 2L && x.getSchedulingRole().getId() == 1L))),
                () -> verify(roleRepository, times(1)).save(argThat(x -> x.getRoleName().equals("New Role Name"))));
    }

}
