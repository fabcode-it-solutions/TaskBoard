package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tuwien.taskinn.dto.EmployeeDto;
import tuwien.taskinn.dto.PtoDto;
import tuwien.taskinn.dto.request.CreatePtoRequest;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.PTO;
import tuwien.taskinn.entity.Shift;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.mapper.EmployeeMapper;
import tuwien.taskinn.mapper.PtoMapper;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.PTORepository;
import tuwien.taskinn.repo.ShiftRepository;
import tuwien.taskinn.services.impl.EmployeeServiceImpl;
import tuwien.taskinn.services.validator.CreateOrUpdateAccountValidator;
import tuwien.taskinn.services.validator.EmployeeValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService Unit Tests")
public class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PTORepository ptoRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private CreateOrUpdateAccountValidator createOrUpdateAccountValidator;

    @Mock
    private EmployeeValidator employeeValidator;

    @Spy
    private EmployeeMapper employeeMapper = new EmployeeMapper();

    @Spy
    private PtoMapper ptoMapper = new PtoMapper();

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    @DisplayName("getAllStaffMembers should return all employees excluding admins")
    public void getAllStaffMembers_whenEmployeesExist_returnsOnlyStaff() {
        Employee staff1 = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        Employee staff2 = Employee.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(35))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        Employee admin = Employee.builder()
                .id(3L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(50))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.ADMIN))
                .build();

        when(employeeRepository.findAll()).thenReturn(List.of(staff1, staff2, admin));

        List<EmployeeDto> result = employeeService.getAllStaffMembers();

        assertAll(() -> assertThat(result).hasSize(2),
                () -> assertThat(result.contains(this.convertEmployeeToDto(staff1))).isTrue(),
                () -> assertThat(result.contains(this.convertEmployeeToDto(staff2))).isTrue(),
                () -> assertThat(result.contains(this.convertEmployeeToDto(admin))).isFalse(),
                () -> verify(employeeRepository, times(1)).findAll());
    }

    @Test
    @DisplayName("getAllStaffMembers should return empty list when no employees exist")
    public void getAllStaffMembers_whenNoEmployees_returnsEmptyList() {
        when(employeeRepository.findAll()).thenReturn(Collections.emptyList());

        List<EmployeeDto> result = employeeService.getAllStaffMembers();

        assertAll(() -> assertThat(result).isEmpty(), () -> verify(employeeRepository, times(1)).findAll());
    }

    @Test
    @DisplayName("getAllStaffMembers should return empty list when only admins exist")
    public void getAllStaffMembers_whenOnlyAdmins_returnsEmptyList() {
        Employee admin = Employee.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(50))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.ADMIN))
                .build();

        when(employeeRepository.findAll()).thenReturn(List.of(admin));

        List<EmployeeDto> result = employeeService.getAllStaffMembers();

        assertAll(() -> assertThat(result).isEmpty(), () -> verify(employeeRepository, times(1)).findAll());
    }

    @Test
    @DisplayName("getEmployeeById should return employee when id exists")
    public void getEmployeeById_whenEmployeeExists_returnsEmployeeDto() {
        Long employeeId = 1L;
        Employee employee = Employee.builder()
                .id(employeeId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        EmployeeDto result = employeeService.getEmployeeById(employeeId);

        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result.getId()).isEqualTo(employeeId),
                () -> assertThat(result.getFirstName()).isEqualTo("John"),
                () -> assertThat(result.getLastName()).isEqualTo("Doe"),
                () -> assertThat(result.getEmail()).isEqualTo("john.doe@taskinn.com"),
                () -> assertThat(result.isActive()).isTrue(),
                () -> assertThat(result.getHourlyWage()).isEqualTo(BigDecimal.valueOf(25)),
                () -> assertThat(result.getWorkingHoursPerWeek()).isEqualTo(BigDecimal.valueOf(40)),
                () -> assertThat(result.getRoles()).containsExactly(Roles.EMPLOYEE),
                () -> verify(employeeRepository, times(1)).findById(employeeId));
    }

    @Test
    @DisplayName("getEmployeeById should throw NoSuchElementException when employee doesn't exist")
    public void getEmployeeById_whenEmployeeNotFound_throwsNoSuchElementException() {
        Long employeeId = 999L;

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
            employeeService.getEmployeeById(employeeId);
        });

        assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Employee not found with id: " + employeeId),
                () -> verify(employeeRepository, times(1)).findById(employeeId),
                () -> verify(employeeRepository, never()).save(any(Employee.class)));
    }

    @Test
    @DisplayName("updateEmployee should update employee when new email is not in use")
    public void updateEmployee_whenNewEmailNotInUse_updatesSuccessfully() {
        Long employeeId = 1L;
        String oldEmail = "john.doe@taskinn.com";
        String newEmail = "john.new@taskinn.com";

        Employee existingEmployee = Employee.builder()
                .id(employeeId)
                .firstName("John")
                .lastName("Doe")
                .email(oldEmail)
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(new HashSet<>(Set.of(Roles.EMPLOYEE)))
                .build();

        EmployeeDto updateDto = EmployeeDto.builder()
                .id(employeeId)
                .firstName("John Updated")
                .lastName("Doe Updated")
                .email(newEmail)
                .active(false)
                .hourlyWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(35))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.existsByEmail(newEmail)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(existingEmployee);

        EmployeeDto result = employeeService.updateEmployee(employeeId, updateDto);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository, times(1)).save(employeeCaptor.capture());
        Employee savedEmployee = employeeCaptor.getValue();

        assertAll(() -> assertThat(savedEmployee.getFirstName()).isEqualTo("John Updated"),
                () -> assertThat(savedEmployee.getLastName()).isEqualTo("Doe Updated"),
                () -> assertThat(savedEmployee.isActive()).isFalse(),
                () -> assertThat(savedEmployee.getHourlyWage()).isEqualTo(BigDecimal.valueOf(30)),
                () -> assertThat(savedEmployee.getWorkingHoursPerWeek()).isEqualTo(BigDecimal.valueOf(35)),
                () -> assertThat(savedEmployee.getEmail()).isEqualTo(newEmail), () -> assertThat(result).isNotNull(),
                () -> verify(employeeRepository, times(1)).findById(employeeId),
                () -> verify(employeeRepository, times(1)).existsByEmail(newEmail),
                () -> verify(createOrUpdateAccountValidator, times(1)).validateEmployeeDto(updateDto));
    }

    @Test
    @DisplayName("updateEmployee should throw IllegalArgumentException when new email is already in use")
    public void updateEmployee_whenNewEmailInUse_throwsIllegalArgumentException() {
        Long employeeId = 1L;
        String oldEmail = "john.doe@taskinn.com";
        String newEmail = "existing.user@taskinn.com";

        Employee existingEmployee = Employee.builder()
                .id(employeeId)
                .firstName("John")
                .lastName("Doe")
                .email(oldEmail)
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(new HashSet<>(Set.of(Roles.EMPLOYEE)))
                .build();

        EmployeeDto updateDto = EmployeeDto.builder()
                .id(employeeId)
                .firstName("John")
                .lastName("Doe")
                .email(newEmail)
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.existsByEmail(newEmail)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            employeeService.updateEmployee(employeeId, updateDto);
        });

        assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Email " + newEmail + " is already in use"),
                () -> verify(employeeRepository, times(1)).findById(employeeId),
                () -> verify(createOrUpdateAccountValidator, times(1)).validateEmployeeDto(updateDto),
                () -> verify(employeeRepository, times(1)).existsByEmail(newEmail),
                () -> verify(employeeRepository, never()).save(any(Employee.class)));
    }

    @Test
    @DisplayName("updateEmployee should throw NoSuchElementException when employee not found")
    public void updateEmployee_whenEmployeeNotFound_throwsNoSuchElementException() {
        Long employeeId = 999L;

        EmployeeDto updateDto = EmployeeDto.builder()
                .id(employeeId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
            employeeService.updateEmployee(employeeId, updateDto);
        });

        assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Employee not found with id: " + employeeId),
                () -> verify(employeeRepository, times(1)).findById(employeeId),
                () -> verify(createOrUpdateAccountValidator, never()).validateEmployeeDto(any(EmployeeDto.class)),
                () -> verify(employeeRepository, never()).existsByEmail(anyString()),
                () -> verify(employeeRepository, never()).save(any(Employee.class)));
    }

    @Test
    @DisplayName("createPtoForEmployee should create PTO when validation passes")
    void createPtoForEmployee_success() {
        String email = "john.doe@taskinn.com";
        CreatePtoRequest request = CreatePtoRequest.builder()
                .startDate(LocalDate.of(2025, 1, 10))
                .endDate(LocalDate.of(2025, 1, 12))
                .build();

        Employee employee = Employee.builder().id(1L).email(email).build();
        PTO saved = PTO.builder()
                .id(42L)
                .employee(employee)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        when(ptoRepository.findByEmployeeEmail(email)).thenReturn(Collections.emptyList());
        when(shiftRepository.findAllShiftsByEmployeeEmailInTimeframe(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        // validator is mocked; no exception means pass
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));
        when(ptoRepository.save(any(PTO.class))).thenReturn(saved);

        PtoDto result = employeeService.createPtoForEmployee(email, request);

        ArgumentCaptor<PTO> ptoCaptor = ArgumentCaptor.forClass(PTO.class);
        verify(ptoRepository).save(ptoCaptor.capture());
        PTO persisted = ptoCaptor.getValue();

        assertAll(() -> assertThat(result.getId()).isEqualTo(42L),
                () -> assertThat(result.getStartDate()).isEqualTo(request.getStartDate()),
                () -> assertThat(result.getEndDate()).isEqualTo(request.getEndDate()),
                () -> assertThat(persisted.getEmployee()).isEqualTo(employee),
                () -> assertThat(persisted.getStartDate()).isEqualTo(request.getStartDate()),
                () -> assertThat(persisted.getEndDate()).isEqualTo(request.getEndDate()),
                () -> verify(ptoRepository).findByEmployeeEmail(email),
                () -> verify(shiftRepository).findAllShiftsByEmployeeEmailInTimeframe(eq(email),
                        eq(LocalDateTime.of(2025, 1, 10, 0, 0)), eq(LocalDateTime.of(2025, 1, 13, 0, 0))),
                () -> verify(employeeValidator).validateCreatePtoRequestDto(request, Collections.emptyList(),
                        Collections.emptyList()),
                () -> verify(employeeRepository).findByEmail(email), () -> verify(ptoRepository).save(any(PTO.class)));
    }

    @Test
    @DisplayName("createPtoForEmployee should propagate validation errors for overlaps")
    void createPtoForEmployee_overlap_throwsBusinessValidationException() {
        String email = "john.doe@taskinn.com";
        CreatePtoRequest request = CreatePtoRequest.builder()
                .startDate(LocalDate.of(2025, 1, 10))
                .endDate(LocalDate.of(2025, 1, 12))
                .build();

        List<PTO> existing = List.of(
                PTO.builder().id(5L).startDate(LocalDate.of(2025, 1, 11)).endDate(LocalDate.of(2025, 1, 15)).build());

        when(ptoRepository.findByEmployeeEmail(email)).thenReturn(existing.stream().collect(Collectors.toList()));
        when(shiftRepository.findAllShiftsByEmployeeEmailInTimeframe(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        doThrow(new BusinessValidationException("endDate", "overlap")).when(employeeValidator)
                .validateCreatePtoRequestDto(request, existing, Collections.emptyList());

        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> employeeService.createPtoForEmployee(email, request));

        assertAll(() -> assertThat(ex.getField()).isEqualTo("endDate"),
                () -> verify(ptoRepository).findByEmployeeEmail(email),
                () -> verify(employeeValidator).validateCreatePtoRequestDto(request, existing, Collections.emptyList()),
                () -> verify(employeeRepository, never()).findByEmail(anyString()),
                () -> verify(ptoRepository, never()).save(any(PTO.class)));
    }

    @Test
    @DisplayName("createPtoForEmployee should propagate validation errors for shift conflicts")
    void createPtoForEmployee_shiftConflict_throwsBusinessValidationException() {
        String email = "jane.doe@taskinn.com";
        CreatePtoRequest request = CreatePtoRequest.builder()
                .startDate(LocalDate.of(2025, 2, 10))
                .endDate(LocalDate.of(2025, 2, 12))
                .build();

        Employee employee = Employee.builder().id(4L).email(email).build();
        List<PTO> existingPtos = Collections.emptyList();
        List<Shift> shifts = List.of(Shift.builder()
                .date(LocalDateTime.of(2025, 2, 11, 0, 0))
                .department("frontdesk")
                .employee(employee)
                .build());

        when(ptoRepository.findByEmployeeEmail(email)).thenReturn(existingPtos);
        when(shiftRepository.findAllShiftsByEmployeeEmailInTimeframe(any(), any(), any())).thenReturn(shifts);
        doThrow(new BusinessValidationException("startDate", "shift overlap")).when(employeeValidator)
                .validateCreatePtoRequestDto(request, existingPtos, shifts);

        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> employeeService.createPtoForEmployee(email, request));

        assertAll(() -> assertThat(ex.getField()).isEqualTo("startDate"),
                () -> verify(ptoRepository).findByEmployeeEmail(email),
                () -> verify(shiftRepository).findAllShiftsByEmployeeEmailInTimeframe(any(), any(), any()),
                () -> verify(employeeValidator).validateCreatePtoRequestDto(request, existingPtos, shifts),
                () -> verify(employeeRepository, never()).findByEmail(anyString()),
                () -> verify(ptoRepository, never()).save(any(PTO.class)));
    }

    @Test
    @DisplayName("getShiftsByEmployeeEmail should return shift day indices for the month")
    void getShiftsByEmployeeEmail_whenShiftsExist_returnsDayIndices() {
        String email = "shifted@taskinn.com";
        int year = 2026;
        int month = 5;

        Employee employee = Employee.builder().id(10L).email(email).build();
        List<Shift> shifts = List.of(
                Shift.builder()
                        .date(LocalDateTime.of(2026, 5, 1, 0, 0))
                        .department("frontdesk")
                        .employee(employee)
                        .build(),
                Shift.builder()
                        .date(LocalDateTime.of(2026, 5, 20, 0, 0))
                        .department("frontdesk")
                        .employee(employee)
                        .build());

        when(employeeRepository.existsByEmail(email)).thenReturn(true);
        when(shiftRepository.findAllShiftsByEmployeeEmailInTimeframe(eq(email), any(), any())).thenReturn(shifts);

        List<Integer> result = employeeService.getShiftsByEmployeeEmail(email, year, month);

        assertAll(() -> assertThat(result).containsExactly(0, 19),
                () -> verify(employeeRepository).existsByEmail(email),
                () -> verify(shiftRepository).findAllShiftsByEmployeeEmailInTimeframe(eq(email),
                        eq(LocalDateTime.of(2026, 5, 1, 0, 0)), eq(LocalDateTime.of(2026, 6, 1, 0, 0))));
    }

    @Test
    @DisplayName("getShiftsByEmployeeEmail should throw when employee is missing")
    void getShiftsByEmployeeEmail_whenEmployeeMissing_throwsNoSuchElementException() {
        String email = "missing@taskinn.com";

        when(employeeRepository.existsByEmail(email)).thenReturn(false);

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> employeeService.getShiftsByEmployeeEmail(email, 2026, 5));

        assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Employee not found with email: " + email),
                () -> verify(employeeRepository).existsByEmail(email),
                () -> verify(shiftRepository, never()).findAllShiftsByEmployeeEmailInTimeframe(any(), any(), any()));
    }

    private EmployeeDto convertEmployeeToDto(Employee employee) {
        return EmployeeDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .hourlyWage(employee.getHourlyWage())
                .workingHoursPerWeek(employee.getWorkingHoursPerWeek())
                .active(employee.isActive())
                .roles(employee.getRoles())
                .build();
    }
}
