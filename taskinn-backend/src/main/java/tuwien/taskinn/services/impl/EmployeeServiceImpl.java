package tuwien.taskinn.services.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tuwien.taskinn.dto.EmployeeDto;
import tuwien.taskinn.dto.PtoDto;
import tuwien.taskinn.dto.request.CreatePtoRequest;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.PTO;
import tuwien.taskinn.entity.Shift;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.mapper.EmployeeMapper;
import tuwien.taskinn.mapper.PtoMapper;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.PTORepository;
import tuwien.taskinn.repo.ShiftRepository;
import tuwien.taskinn.services.EmployeeService;
import tuwien.taskinn.services.validator.CreateOrUpdateAccountValidator;
import tuwien.taskinn.services.validator.EmployeeValidator;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeValidator employeeValidator;
    private final PTORepository ptoRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeMapper employeeMapper;
    private final PtoMapper ptoMapper;
    private final CreateOrUpdateAccountValidator createOrUpdateAccountValidator;

    @Override
    public List<EmployeeDto> getAllStaffMembers() {
        return employeeRepository.findAll()
                .stream()
                .filter(employee -> !employee.getRoles().contains(Roles.ADMIN))
                .map(employeeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public EmployeeDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Employee not found with id: " + id));
        return employeeMapper.toDto(employee);
    }

    @Override
    @Transactional
    public EmployeeDto updateEmployee(Long id, EmployeeDto updateEmployeeRequestDto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Employee not found with id: " + id));
        createOrUpdateAccountValidator.validateEmployeeDto(updateEmployeeRequestDto);

        if (!employee.getEmail().equals(updateEmployeeRequestDto.getEmail())
                && employeeRepository.existsByEmail(updateEmployeeRequestDto.getEmail())) {
            throw new IllegalArgumentException("Email " + updateEmployeeRequestDto.getEmail() + " is already in use");
        }

        employee.setFirstName(updateEmployeeRequestDto.getFirstName());
        employee.setLastName(updateEmployeeRequestDto.getLastName());
        employee.setEmail(updateEmployeeRequestDto.getEmail());
        employee.setActive(updateEmployeeRequestDto.isActive());
        employee.setHourlyWage(updateEmployeeRequestDto.getHourlyWage());
        employee.setHourlyOvertimeWage(updateEmployeeRequestDto.getHourlyOvertimeWage());
        employee.setWorkingHoursPerWeek(updateEmployeeRequestDto.getWorkingHoursPerWeek());
        employee.setRoles(updateEmployeeRequestDto.getRoles());

        Employee updatedEmployee = employeeRepository.save(employee);
        return employeeMapper.toDto(updatedEmployee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PtoDto> getPtosByEmployeeEmail(String email) {
        if (!employeeRepository.existsByEmail(email)) {
            throw new NoSuchElementException("Employee not found with email: " + email);
        }
        return ptoRepository.findByEmployeeEmail(email).stream().map(ptoMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PtoDto createPtoForEmployee(String email, CreatePtoRequest request) {
        List<PTO> existingPtos = ptoRepository.findByEmployeeEmail(email);

        // Fetch shifts that overlap with the requested PTO period
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().plusDays(1).atStartOfDay();
        List<Shift> existingShifts =
                shiftRepository.findAllShiftsByEmployeeEmailInTimeframe(email, startDateTime, endDateTime);

        employeeValidator.validateCreatePtoRequestDto(request, existingPtos, existingShifts);

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Employee not found with email: " + email));

        PTO pto = PTO.builder()
                .employee(employee)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        PTO saved = ptoRepository.save(pto);
        return ptoMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deletePtoForEmployee(String email, Long ptoId) {
        PTO pto = ptoRepository.findById(ptoId)
                .orElseThrow(() -> new NoSuchElementException("PTO not found with id: " + ptoId));

        if (pto.getEmployee() == null || !email.equals(pto.getEmployee().getEmail())) {
            throw new IllegalArgumentException("You don't have access to delete this PTO.");
        }

        ptoRepository.delete(pto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getShiftsByEmployeeEmail(String email, int year, int month) {
        if (!employeeRepository.existsByEmail(email)) {
            throw new NoSuchElementException("Employee not found with email: " + email);
        }

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1);
        LocalDateTime startDateTime = monthStart.atStartOfDay();
        LocalDateTime endDateTime = monthEnd.atStartOfDay();

        List<Shift> shifts = shiftRepository.findAllShiftsByEmployeeEmailInTimeframe(email, startDateTime, endDateTime);

        List<Integer> dayIndices = new ArrayList<>();
        for (Shift shift : shifts) {
            dayIndices.add(shift.getDate().getDayOfMonth() - 1);
        }

        return dayIndices;
    }
}
