package tuwien.taskinn.datagenerator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.PTO;
import tuwien.taskinn.entity.Shift;
import tuwien.taskinn.repo.PTORepository;
import tuwien.taskinn.repo.ShiftRepository;

@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class HistoricalShiftDataGenerator {

    private final ShiftRepository shiftRepository;
    private final PTORepository ptoRepository;

    private static final LocalDate START_DATE = LocalDate.of(2023, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 12, 31);
    private static final LocalTime SHIFT_START_TIME = LocalTime.of(8, 0);
    private static final int BATCH_SIZE = 500;

    private final Random random = new Random(42); // Fixed seed for reproducibility

    /**
     * Generates historical shift data for all employees with scheduling roles.
     *
     * @param employees The list of employees to generate shifts for
     */
    public void generateData(List<Employee> employees) {
        if (shiftRepository.count() > 0) {
            log.info("Shift records already exist. Skipping historical shift generation.");
            return;
        }

        List<Employee> employeesWithRoles =
                employees.stream().filter(e -> e.getSchedulingRole() != null).collect(Collectors.toList());

        if (employeesWithRoles.isEmpty()) {
            log.warn("No employees with scheduling roles found. Skipping historical shift generation.");
            return;
        }

        log.info("Generating historical shift data for {} employees from {} to {}...", employeesWithRoles.size(),
                START_DATE, END_DATE);

        // Load all PTO records upfront for efficiency
        Map<Long, List<PTO>> ptoByEmployee = loadPtoData(employeesWithRoles);

        List<Shift> shiftsToSave = new ArrayList<>();
        int totalShifts = 0;

        for (Employee employee : employeesWithRoles) {
            String department = employee.getSchedulingRole().getRoleName();
            List<PTO> employeePtos = ptoByEmployee.getOrDefault(employee.getId(), Collections.emptyList());

            LocalDate currentDate = START_DATE;
            while (!currentDate.isAfter(END_DATE)) {
                // Generate ~5 work days per week
                List<LocalDate> workDays = selectWorkDaysForWeek(currentDate);

                for (LocalDate workDay : workDays) {
                    if (workDay.isAfter(END_DATE)) {
                        break;
                    }

                    // Skip if employee has PTO on this day
                    if (isOnPto(workDay, employeePtos)) {
                        continue;
                    }

                    Shift shift = Shift.builder()
                            .date(LocalDateTime.of(workDay, SHIFT_START_TIME))
                            .department(department)
                            .employee(employee)
                            .build();

                    shiftsToSave.add(shift);

                    // Batch save for performance
                    if (shiftsToSave.size() >= BATCH_SIZE) {
                        shiftRepository.saveAll(shiftsToSave);
                        totalShifts += shiftsToSave.size();
                        shiftsToSave.clear();
                    }
                }

                // Move to next week
                currentDate = currentDate.plusWeeks(1);
            }
        }

        // Save remaining shifts
        if (!shiftsToSave.isEmpty()) {
            shiftRepository.saveAll(shiftsToSave);
            totalShifts += shiftsToSave.size();
        }

        log.info("Historical shift data generation complete. Created {} shift records.", totalShifts);
    }

    /**
     * Loads PTO data for all employees into a map for efficient lookup.
     */
    private Map<Long, List<PTO>> loadPtoData(List<Employee> employees) {
        List<Long> employeeIds = employees.stream().map(Employee::getId).collect(Collectors.toList());

        List<PTO> allPtos = ptoRepository.findEmployeePTOsInTimeframe(employeeIds, START_DATE, END_DATE.plusDays(1));

        return allPtos.stream().collect(Collectors.groupingBy(pto -> pto.getEmployee().getId()));
    }

    /**
     * Selects approximately 5 work days for a given week. Primarily Monday-Friday, with 20% chance for weekend
     * coverage.
     */
    private List<LocalDate> selectWorkDaysForWeek(LocalDate weekStart) {
        List<LocalDate> workDays = new ArrayList<>();

        // Start from Monday of the week
        LocalDate monday = weekStart.with(DayOfWeek.MONDAY);

        // Add weekdays (Monday-Friday)
        for (int i = 0; i < 5; i++) {
            workDays.add(monday.plusDays(i));
        }

        // 20% chance to include Saturday or Sunday
        if (random.nextDouble() < 0.20) {
            if (random.nextBoolean()) {
                workDays.add(monday.plusDays(5)); // Saturday
            } else {
                workDays.add(monday.plusDays(6)); // Sunday
            }
        }

        return workDays;
    }

    /**
     * Checks if an employee is on PTO for a given date.
     */
    private boolean isOnPto(LocalDate date, List<PTO> ptos) {
        for (PTO pto : ptos) {
            if (!date.isBefore(pto.getStartDate()) && !date.isAfter(pto.getEndDate())) {
                return true;
            }
        }
        return false;
    }
}
