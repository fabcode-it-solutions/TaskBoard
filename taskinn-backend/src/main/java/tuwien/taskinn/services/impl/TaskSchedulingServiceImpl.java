package tuwien.taskinn.services.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tuwien.taskinn.config.SchedulerConfig;
import tuwien.taskinn.dto.CleaningTaskDto;
import tuwien.taskinn.dto.request.TaskConstraintDto;
import tuwien.taskinn.dto.request.TaskScheduleSolveRequestDto;
import tuwien.taskinn.entity.CleaningTask;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.TaskStatus;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.exceptions.SolverFailureException;
import tuwien.taskinn.mapper.CleaningTaskMapper;
import tuwien.taskinn.repo.CleaningTaskRepository;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.services.TaskSchedulingService;
import tuwien.taskinn.services.scheduling.instance.TaskSchedulingInstance;
import tuwien.taskinn.services.scheduling.solution.TaskSchedulingSolution;
import tuwien.taskinn.services.scheduling.solution.TaskSchedulingSolutionCostFunction;
import tuwien.taskinn.services.scheduling.solver.GreedyTaskScheduleGenerator;
import tuwien.taskinn.services.scheduling.solver.TaskScheduleReassignmentNeighborhood;
import tuwien.taskinn.services.scheduling.solver.TaskSchedulingSolver;
import tuwien.taskinn.services.validator.TaskSchedulingValidator;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSchedulingServiceImpl implements TaskSchedulingService {

    private final CleaningTaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final RoomRepository roomRepository;
    private final TaskSchedulingValidator taskValidator;
    private final CleaningTaskMapper taskMapper;
    private final SchedulerConfig schedulerConfig;

    @Override
    public List<CleaningTaskDto> getTasksForDay(Integer year, Integer month, Integer day) {
        taskValidator.validateYearMonthDay(year, month, day);
        final LocalDateTime dayStart = LocalDateTime.of(year, month, day, 0, 0);
        final LocalDateTime dayEnd = dayStart.plusDays(1);
        return taskRepository.findAllTasksInTimeframe(dayStart, dayEnd)
                .stream()
                .map(taskMapper::toTaskScheduleDto)
                .toList();
    }

    @Override
    public List<CleaningTaskDto> getTasksForEmployeeAndDay(Integer year, Integer month, Integer day, Long employeeId) {
        taskValidator.validateYearMonthDay(year, month, day);
        final LocalDateTime dayStart = LocalDateTime.of(year, month, day, 0, 0);
        final LocalDateTime dayEnd = dayStart.plusDays(1);
        return taskRepository.findAllEmployeeTasksInTimeframe(employeeId, dayStart, dayEnd)
                .stream()
                .map(taskMapper::toTaskScheduleDto)
                .toList();
    }

    @Override
    @Transactional
    public void putTasksForDay(Integer year, Integer month, Integer day, List<CleaningTaskDto> tasks) {
        taskValidator.validateCleaningTaskDtoList(year, month, day, tasks);

        final LocalDateTime dayStart = LocalDateTime.of(year, month, day, 0, 0);
        final LocalDateTime dayEnd = dayStart.plusDays(1);

        // Find all previously known tasks for the given day in order to provide defaults for the task status
        final List<CleaningTask> previousTasks = taskRepository.findAllTasksInTimeframe(dayStart, dayEnd);
        final Map<Long, TaskStatus> previousStatus =
                previousTasks.stream().collect(Collectors.toMap(x -> x.getRoom().getId(), CleaningTask::getStatus));

        // Find all employees involved in the new task schedule
        final Set<Long> employeeIdSet = tasks.stream().map(CleaningTaskDto::getEmployeeId).collect(Collectors.toSet());
        final Map<Long, Employee> employeeMap = employeeRepository.findAllById(employeeIdSet)
                .stream()
                .collect(Collectors.toMap(Employee::getId, x -> x));

        // Find all rooms involved in the new task schedule
        final Set<Long> roomIdSet = tasks.stream().map(CleaningTaskDto::getRoomId).collect(Collectors.toSet());
        final Map<Long, Room> roomMap =
                roomRepository.findAllById(roomIdSet).stream().collect(Collectors.toMap(Room::getId, x -> x));

        // Validate no employee overlaps (after fetching employee and room data so we can show names and room numbers)
        taskValidator.validateNoEmployeeOverlaps(tasks, employeeMap, roomMap);

        final List<CleaningTask> newCleaningTasks = tasks.stream().map(task -> {
            final Employee employee = employeeMap.get(task.getEmployeeId());
            if (employee == null) {
                throw new EntityNotFoundException("Employee with ID " + task.getEmployeeId() + " not found");
            }
            final Room room = roomMap.get(task.getRoomId());
            if (room == null) {
                throw new EntityNotFoundException("Room with ID " + task.getRoomId() + " not found");
            }
            // In case status was not given, try to get a value from an old task on the same day/for the same room.
            // If no such task exists, default to PENDING
            final TaskStatus status = (task.getStatus() != null) ? task.getStatus()
                    : previousStatus.getOrDefault(task.getRoomId(), TaskStatus.PENDING);
            return CleaningTask.builder()
                    .scheduledStart(task.getScheduledStart())
                    .scheduledEnd(task.getScheduledEnd())
                    .taskType(task.getTaskType())
                    .status(status)
                    .notes(task.getNotes())
                    .employee(employee)
                    .room(room)
                    .build();
        }).toList();

        taskRepository.deleteAllTasksInTimeframe(dayStart, dayEnd);
        taskRepository.saveAll(newCleaningTasks);
    }

    @Override
    public List<CleaningTaskDto> solveTaskScheduling(TaskScheduleSolveRequestDto request) {
        return solveTaskScheduling(request, schedulerConfig.getTaskAttempts(), schedulerConfig.getTaskRestarts(),
                schedulerConfig.getTaskAnnealingIterations());
    }

    public List<CleaningTaskDto> solveTaskScheduling(TaskScheduleSolveRequestDto request, int attempts, int restarts,
            int annealingIterations) {
        taskValidator.validateSolveRequest(request);

        // Retrieve all the employee entities involved in the task scheduling.
        final Set<Long> employeeIdSet = new HashSet<>(request.getEmployeeIds());
        final List<Long> employeeIdList = employeeIdSet.stream().toList();
        final Map<Long, Employee> employeeMap = employeeRepository.findAllById(employeeIdSet)
                .stream()
                .collect(Collectors.toMap(Employee::getId, x -> x));
        if (employeeIdSet.size() != employeeMap.size()) {
            for (final long id : employeeIdSet) {
                if (!employeeMap.containsKey(id)) {
                    throw new BusinessValidationException("employeeIds", "Employee with id " + id + " not found");
                }
            }
        }

        // Retrieve all the room entities involved in the task scheduling.
        final Set<Long> roomIdSet =
                request.getConstraints().stream().map(TaskConstraintDto::getRoomId).collect(Collectors.toSet());
        if (roomIdSet.size() != request.getConstraints().size()) {
            // Ensures there are no duplicate constraints for the same room.
            throw new BusinessValidationException("constraints", "Constraints and rooms must be one-to-one");
        }
        final Map<Long, TaskConstraintDto> constraintMap =
                request.getConstraints().stream().collect(Collectors.toMap(TaskConstraintDto::getRoomId, x -> x));
        final Map<Long, Room> roomMap =
                roomRepository.findAllById(roomIdSet).stream().collect(Collectors.toMap(Room::getId, x -> x));
        if (roomIdSet.size() != roomMap.size()) {
            for (final long id : roomIdSet) {
                if (!roomMap.containsKey(id)) {
                    throw new BusinessValidationException("roomIds", "Room with id " + id + " not found");
                }
            }
        }

        // Starting time of the task scheduling problem.
        final LocalDateTime taskStart = request.getStartTime();
        // Start of the day of the task scheduling problem.
        final LocalDateTime dayStart = taskStart.toLocalDate().atStartOfDay();
        // End of the day of the task scheduling problem (exclusive).
        final LocalDateTime dayEnd = dayStart.plusDays(1);

        final ArrayList<TaskSchedulingInstance.RoomTask> roomTasks = new ArrayList<>(request.getConstraints().size());
        // Convert the given room constraints into the solver-internal representation.
        for (final TaskConstraintDto constraint : request.getConstraints()) {
            final Room room = roomMap.get(constraint.getRoomId());
            // Calculate the center of the room.
            int pointCount = 0;
            double roomX = 0.0;
            double roomY = 0.0;
            for (final var point : room.getSvgPoints()) {
                pointCount++;
                roomX += point.getX();
                roomY += point.getY();
            }
            if (pointCount == 0) {
                throw new IllegalArgumentException("Room without shape");
            }
            roomX /= pointCount;
            roomY /= pointCount;
            // If necessary, convert the time constraints to the required relative representation.
            int maxEndtime = -1;
            int minStartTime = -1;
            if (constraint.getMaxEndTime() != null) {
                maxEndtime = Math.toIntExact(ChronoUnit.MINUTES.between(taskStart, constraint.getMaxEndTime()));
            }
            if (constraint.getMinStartTime() != null) {
                minStartTime = Math.toIntExact(ChronoUnit.MINUTES.between(taskStart, constraint.getMinStartTime()));
            }

            roomTasks.add(new TaskSchedulingInstance.RoomTask(constraint.getRoomId(), roomX, roomY, room.getFloor(),
                    Math.toIntExact(constraint.getDuration()), maxEndtime, minStartTime));
        }

        // Do the actual solving.

        // Instance describing the given task scheduling problem.
        final TaskSchedulingInstance instance = new TaskSchedulingInstance(employeeIdList.size(), roomTasks.stream());
        // Random number generator used throughout the solving process.
        final Random random = new Random(instance.instanceSeed());
        // Generator for initial seed solutions.
        final var generator = new GreedyTaskScheduleGenerator();
        // Definition of solution neighborhoods for steps within the solver.
        final var neighborhood = new TaskScheduleReassignmentNeighborhood();
        // The actual solver itself.
        final var solver = new TaskSchedulingSolver(attempts, restarts, annealingIterations, generator, neighborhood);
        TaskSchedulingSolution solution;
        try {
            solution = solver.solve(random, instance);
        } catch (RuntimeException e) {
            // Last ditch measure: if the solver failed in an unexpected way, return the seed solution. This ensures
            // that the user sees at least something.
            solution = generator.generate(instance);
        }

        final ArrayList<CleaningTaskDto> result = new ArrayList<>(request.getConstraints().size());
        // Convert the solver's solution into the expected format.
        for (int employeeIndex = 0; employeeIndex < employeeIdList.size(); employeeIndex++) {
            // Index must be effectively final to be usable in the lambda expression.
            final int finalEmployeeIndex = employeeIndex;
            TaskSchedulingSolutionCostFunction.applySolutionRow(instance, solution, employeeIndex,
                    (room, currentTime) -> {
                        final LocalDateTime scheduledStart = taskStart.plusMinutes((long) Math.ceil(currentTime));
                        final LocalDateTime scheduledEnd = scheduledStart.plusMinutes(room.taskDuration());
                        if (!(scheduledStart.isBefore(dayEnd) && scheduledEnd.isBefore(dayEnd))) {
                            throw new SolverFailureException(
                                    "Could not find solution that doesn't overlap into the next day");
                        }

                        final Employee employeeEntity = employeeMap.get(employeeIdList.get(finalEmployeeIndex));
                        final Room roomEntity = roomMap.get(room.identifier());
                        final TaskConstraintDto constraint = constraintMap.get(room.identifier());

                        final boolean maxConstraintViolated =
                                constraint.getMaxEndTime() != null && scheduledEnd.isAfter(constraint.getMaxEndTime());
                        final boolean minConstraintViolated = constraint.getMinStartTime() != null
                                && scheduledStart.isBefore(constraint.getMinStartTime());

                        result.add(CleaningTaskDto.builder()
                                .scheduledStart(scheduledStart)
                                .scheduledEnd(scheduledEnd)
                                .taskType(constraint.getTaskType())
                                .status(constraint.getTaskStatus())
                                .notes("")
                                .constraintViolated(maxConstraintViolated || minConstraintViolated)
                                .employeeId(employeeEntity.getId())
                                .employeeName(employeeEntity.getFirstName() + " " + employeeEntity.getLastName())
                                .roomId(roomEntity.getId())
                                .roomNumber(roomEntity.getRoomNumber())
                                .build());
                    });
        }
        return result;
    }

}
