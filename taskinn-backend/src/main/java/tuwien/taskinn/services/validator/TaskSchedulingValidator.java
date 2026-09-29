package tuwien.taskinn.services.validator;

import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.CleaningTaskDto;
import tuwien.taskinn.dto.request.TaskConstraintDto;
import tuwien.taskinn.dto.request.TaskScheduleSolveRequestDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.exceptions.BusinessValidationException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Component
public class TaskSchedulingValidator {

    public void validateYearMonthDay(Integer year, Integer month, Integer day) {
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

        final int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        if (day == null) {
            throw new BusinessValidationException("day", "Day must not be null");
        } else if (day <= 0) {
            throw new BusinessValidationException("day", "Day must be greater than 0");
        } else if (day > daysInMonth) {
            throw new BusinessValidationException("day", "Day must not exceed days in month");
        }
    }

    private boolean dateTimeDayDifferent(LocalDateTime date, int year, int month, int day) {
        return date.getDayOfMonth() != day || date.getMonthValue() != month || date.getYear() != year;
    }

    private boolean dateTimeDayDifferent(LocalDateTime left, LocalDateTime right) {
        return left.getDayOfMonth() != right.getDayOfMonth() || left.getMonthValue() != right.getMonthValue()
                || left.getYear() != right.getYear();
    }

    public void validateCleaningTaskDto(Integer year, Integer month, Integer day, CleaningTaskDto dto) {
        if (dto.getScheduledStart() == null) {
            throw new BusinessValidationException("scheduledStart", "Scheduled start must not be null");
        } else if (dateTimeDayDifferent(dto.getScheduledStart(), year, month, day)) {
            throw new BusinessValidationException("scheduledStart",
                    "Scheduled start must be on the same day as the cleaning task");
        }
        if (dto.getScheduledEnd() == null) {
            throw new BusinessValidationException("scheduledEnd", "Scheduled end must not be null");
        } else if (dateTimeDayDifferent(dto.getScheduledEnd(), year, month, day)) {
            throw new BusinessValidationException("scheduledEnd",
                    "Scheduled end must be on the same day as the cleaning task");
        }
        if (dto.getScheduledEnd().isBefore(dto.getScheduledStart())) {
            throw new BusinessValidationException("scheduledEnd", "Scheduled end must not be before scheduled start");
        }

        if (dto.getTaskType() == null) {
            throw new BusinessValidationException("taskType", "Task type must not be null");
        }

        if (dto.getEmployeeId() == null) {
            throw new BusinessValidationException("employeeId", "Employee ID must not be null");
        }

        if (dto.getRoomId() == null) {
            throw new BusinessValidationException("roomId", "Room ID must not be null");
        }
    }

    public void validateCleaningTaskDtoList(Integer year, Integer month, Integer day, List<CleaningTaskDto> dtoList) {
        validateYearMonthDay(year, month, day);
        dtoList.forEach(x -> validateCleaningTaskDto(year, month, day, x));
    }

    /**
     * Validates that no employee is assigned to multiple tasks with overlapping time periods. Two tasks overlap if:
     * task1.start < task2.end AND task2.start < task1.end
     *
     * @param dtoList List of cleaning task DTOs to validate
     * @param employeeMap Map of employee ID to Employee entity for name lookup
     * @param roomMap Map of room ID to Room entity for room number lookup
     */
    public void validateNoEmployeeOverlaps(List<CleaningTaskDto> dtoList, Map<Long, Employee> employeeMap,
            Map<Long, Room> roomMap) {
        // Group tasks by employee ID
        var tasksByEmployee =
                dtoList.stream().collect(java.util.stream.Collectors.groupingBy(CleaningTaskDto::getEmployeeId));

        // Check each employee's tasks for overlaps
        for (var entry : tasksByEmployee.entrySet()) {
            Long employeeId = entry.getKey();
            List<CleaningTaskDto> employeeTasks = entry.getValue();

            // Compare each task with every other task for this employee
            for (int i = 0; i < employeeTasks.size(); i++) {
                CleaningTaskDto task1 = employeeTasks.get(i);
                for (int j = i + 1; j < employeeTasks.size(); j++) {
                    CleaningTaskDto task2 = employeeTasks.get(j);

                    // Check if tasks overlap: task1.start < task2.end AND task2.start < task1.end
                    boolean overlaps = task1.getScheduledStart().isBefore(task2.getScheduledEnd())
                            && task2.getScheduledStart().isBefore(task1.getScheduledEnd());

                    if (overlaps) {
                        // Get employee name from the map
                        String employeeName = "Employee " + employeeId;
                        Employee employee = employeeMap.get(employeeId);
                        if (employee != null) {
                            employeeName = employee.getFirstName() + " " + employee.getLastName();
                        }

                        // Get room numbers from the map
                        String room1Number = String.valueOf(task1.getRoomId());
                        Room room1 = roomMap.get(task1.getRoomId());
                        if (room1 != null) {
                            room1Number = room1.getRoomNumber();
                        }

                        String room2Number = String.valueOf(task2.getRoomId());
                        Room room2 = roomMap.get(task2.getRoomId());
                        if (room2 != null) {
                            room2Number = room2.getRoomNumber();
                        }

                        String message =
                                String.format("%s cannot work on room %s (%s-%s) and room %s (%s-%s) at the same time",
                                        employeeName, room1Number, task1.getScheduledStart(), task1.getScheduledEnd(),
                                        room2Number, task2.getScheduledStart(), task2.getScheduledEnd());
                        throw new BusinessValidationException("tasks", message);
                    }
                }
            }
        }
    }

    public void validateSolveRequest(TaskScheduleSolveRequestDto request) {
        if (request.getEmployeeIds().isEmpty()) {
            throw new BusinessValidationException("employeeIds", "Employee IDs cannot be empty");
        }

        for (final Long id : request.getEmployeeIds()) {
            if (id == null) {
                throw new BusinessValidationException("employeeIds", "Employee ID must not be null");
            }
        }

        for (final TaskConstraintDto constraint : request.getConstraints()) {
            if (constraint.getDuration() < 0) {
                throw new BusinessValidationException("duration", "Duration constraint must not be negative");
            }
            if (constraint.getMaxEndTime() != null
                    && dateTimeDayDifferent(request.getStartTime(), constraint.getMaxEndTime())) {
                throw new BusinessValidationException("maxEndTime",
                        "Maximum end time must be on same day as all tasks");
            }
            if (constraint.getMinStartTime() != null
                    && dateTimeDayDifferent(request.getStartTime(), constraint.getMinStartTime())) {
                throw new BusinessValidationException("minStartTime",
                        "Minimum start time must be on same day as all tasks");
            }
        }
    }

}
