package tuwien.taskinn.unittest.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tuwien.taskinn.dto.CleaningTaskDto;
import tuwien.taskinn.dto.request.TaskConstraintDto;
import tuwien.taskinn.dto.request.TaskScheduleSolveRequestDto;
import tuwien.taskinn.entity.CleaningTask;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.Point;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.TaskStatus;
import tuwien.taskinn.enums.TaskType;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.mapper.CleaningTaskMapper;
import tuwien.taskinn.repo.CleaningTaskRepository;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.services.impl.TaskSchedulingServiceImpl;
import tuwien.taskinn.services.validator.TaskSchedulingValidator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskSchedulingServiceImpl Unit Tests")
class TaskSchedulingServiceImplTest {

    @Mock
    private CleaningTaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RoomRepository roomRepository;

    @Spy
    private TaskSchedulingValidator taskValidator = new TaskSchedulingValidator();

    @Spy
    private CleaningTaskMapper taskMapper = new CleaningTaskMapper();

    @InjectMocks
    private TaskSchedulingServiceImpl taskSchedulingService;

    @Test
    public void getTasksForDay_whenTasksExist_returnTasks() {
        final Employee employee1 = Employee.builder().id(1L).firstName("John").lastName("Doe").build();
        final Employee employee2 = Employee.builder().id(2L).firstName("Jane").lastName("Doe").build();

        final Room room1 = Room.builder().id(1L).roomNumber("123").build();
        final Room room2 = Room.builder().id(2L).roomNumber("456").build();
        final Room room3 = Room.builder().id(3L).roomNumber("789").build();
        final Room room4 = Room.builder().id(4L).roomNumber("101").build();

        final CleaningTask task1 = CleaningTask.builder().id(1L).room(room1).employee(employee1).build();
        final CleaningTask task2 = CleaningTask.builder().id(2L).room(room2).employee(employee1).build();
        final CleaningTask task3 = CleaningTask.builder().id(3L).room(room3).employee(employee2).build();
        final CleaningTask task4 = CleaningTask.builder().id(4L).room(room4).employee(employee2).build();

        when(taskRepository.findAllTasksInTimeframe(
                argThat(x -> x.getDayOfMonth() == 1 && x.getMonthValue() == 5 && x.getYear() == 2026),
                argThat(x -> x.getDayOfMonth() == 2 && x.getMonthValue() == 5 && x.getYear() == 2026)))
                .thenReturn(List.of(task1, task2, task3, task4));

        final List<CleaningTaskDto> result = taskSchedulingService.getTasksForDay(2026, 5, 1);
        final Map<Long, CleaningTaskDto> resultMap =
                result.stream().collect(Collectors.toMap(CleaningTaskDto::getId, x -> x));

        assertAll(() -> assertThat(result).hasSize(4),
                () -> assertThat(resultMap.get(1L).getRoomId()).isEqualTo(task1.getRoom().getId()),
                () -> assertThat(resultMap.get(1L).getEmployeeId()).isEqualTo(task1.getEmployee().getId()),
                () -> assertThat(resultMap.get(2L).getRoomId()).isEqualTo(task2.getRoom().getId()),
                () -> assertThat(resultMap.get(2L).getEmployeeId()).isEqualTo(task2.getEmployee().getId()),
                () -> assertThat(resultMap.get(3L).getRoomId()).isEqualTo(task3.getRoom().getId()),
                () -> assertThat(resultMap.get(3L).getEmployeeId()).isEqualTo(task3.getEmployee().getId()),
                () -> assertThat(resultMap.get(4L).getRoomId()).isEqualTo(task4.getRoom().getId()),
                () -> assertThat(resultMap.get(4L).getEmployeeId()).isEqualTo(task4.getEmployee().getId()));
    }

    @Test
    public void getTasksForEmployeeAndDay_whenTasksExist_returnTasks() {
        final Employee employee1 = Employee.builder().id(1L).firstName("John").lastName("Doe").build();

        final Room room1 = Room.builder().id(1L).roomNumber("123").build();
        final Room room2 = Room.builder().id(2L).roomNumber("456").build();

        final CleaningTask task1 = CleaningTask.builder().id(1L).room(room1).employee(employee1).build();
        final CleaningTask task2 = CleaningTask.builder().id(2L).room(room2).employee(employee1).build();

        when(taskRepository.findAllEmployeeTasksInTimeframe(eq(1L),
                argThat(x -> x.getDayOfMonth() == 1 && x.getMonthValue() == 5 && x.getYear() == 2026),
                argThat(x -> x.getDayOfMonth() == 2 && x.getMonthValue() == 5 && x.getYear() == 2026)))
                .thenReturn(List.of(task1, task2));

        final List<CleaningTaskDto> result = taskSchedulingService.getTasksForEmployeeAndDay(2026, 5, 1, 1L);
        final Map<Long, CleaningTaskDto> resultMap =
                result.stream().collect(Collectors.toMap(CleaningTaskDto::getId, x -> x));

        assertAll(() -> assertThat(result).hasSize(2),
                () -> assertThat(resultMap.get(1L).getRoomId()).isEqualTo(task1.getRoom().getId()),
                () -> assertThat(resultMap.get(1L).getEmployeeId()).isEqualTo(task1.getEmployee().getId()),
                () -> assertThat(resultMap.get(2L).getRoomId()).isEqualTo(task2.getRoom().getId()),
                () -> assertThat(resultMap.get(2L).getEmployeeId()).isEqualTo(task2.getEmployee().getId()));

    }

    private <T> Set<T> iterableToSet(Iterable<T> it) {
        final HashSet<T> result = new HashSet<>();
        it.forEach(result::add);
        return result;
    }

    @Test
    public void putTasksForDay_whenValid_replacesDailyTasks() {
        final Employee employee1 = Employee.builder().id(1L).firstName("John").lastName("Doe").build();
        final Employee employee2 = Employee.builder().id(2L).firstName("Jane").lastName("Doe").build();

        final Room room1 = Room.builder().id(1L).roomNumber("123").build();
        final Room room2 = Room.builder().id(2L).roomNumber("456").build();

        final LocalDateTime start = LocalDateTime.of(2026, 5, 1, 0, 0);

        final List<CleaningTaskDto> request = List.of(
                CleaningTaskDto.builder()
                        .scheduledStart(start.plusMinutes(10))
                        .scheduledEnd(start.plusMinutes(20))
                        .taskType(TaskType.DEEP_CLEAN)
                        .employeeId(1L)
                        .roomId(2L)
                        .build(),
                CleaningTaskDto.builder()
                        .scheduledStart(start.plusMinutes(40))
                        .scheduledEnd(start.plusMinutes(55))
                        .taskType(TaskType.CHECKOUT_CLEAN)
                        .employeeId(2L)
                        .roomId(1L)
                        .build());

        when(employeeRepository.findAllById(argThat(x -> iterableToSet(x).containsAll(List.of(1L, 2L)))))
                .thenReturn(List.of(employee1, employee2));
        when(roomRepository.findAllById(argThat(x -> iterableToSet(x).containsAll(List.of(1L, 2L)))))
                .thenReturn(List.of(room1, room2));
        when(taskRepository.findAllTasksInTimeframe(any(), any())).thenReturn(List.of());

        taskSchedulingService.putTasksForDay(2026, 5, 1, request);

        assertAll(
                () -> verify(taskRepository, times(1)).deleteAllTasksInTimeframe(
                        argThat(x -> x.getDayOfMonth() == 1 && x.getMonthValue() == 5 && x.getYear() == 2026),
                        argThat(x -> x.getDayOfMonth() == 2 && x.getMonthValue() == 5 && x.getYear() == 2026)),
                () -> verify(taskRepository, times(1)).saveAll(argThat(x -> {
                    final var argset = iterableToSet(x);
                    final var employeeIds =
                            argset.stream().map(t -> t.getEmployee().getId()).collect(Collectors.toSet());
                    final var roomIds = argset.stream().map(t -> t.getRoom().getId()).collect(Collectors.toSet());
                    return employeeIds.containsAll(List.of(1L, 2L)) && roomIds.containsAll(List.of(1L, 2L));
                })));
    }

    @Test
    public void putTasksForDay_whenValid_useExistingStatusAsDefault() {
        final Employee employee1 = Employee.builder().id(1L).firstName("John").lastName("Doe").build();

        final Room room1 = Room.builder().id(1L).roomNumber("123").build();
        final Room room2 = Room.builder().id(2L).roomNumber("456").build();

        final CleaningTask task1 = CleaningTask.builder().room(room1).status(TaskStatus.IN_PROGRESS).build();
        final CleaningTask task2 = CleaningTask.builder().room(room2).status(TaskStatus.BLOCKED).build();

        final LocalDateTime start = LocalDateTime.of(2026, 5, 1, 0, 0);

        final List<CleaningTaskDto> request = List.of(
                CleaningTaskDto.builder()
                        .scheduledStart(start.plusMinutes(10))
                        .scheduledEnd(start.plusMinutes(20))
                        .taskType(TaskType.DEEP_CLEAN)
                        .employeeId(1L)
                        .roomId(1L)
                        .build(),
                CleaningTaskDto.builder()
                        .scheduledStart(start.plusMinutes(40))
                        .scheduledEnd(start.plusMinutes(55))
                        .taskType(TaskType.CHECKOUT_CLEAN)
                        .employeeId(1L)
                        .roomId(2L)
                        .build());

        when(employeeRepository.findAllById(argThat(x -> iterableToSet(x).contains(1L))))
                .thenReturn(List.of(employee1));
        when(roomRepository.findAllById(argThat(x -> iterableToSet(x).containsAll(List.of(1L, 2L)))))
                .thenReturn(List.of(room1, room2));
        when(taskRepository.findAllTasksInTimeframe(any(), any())).thenReturn(List.of(task1, task2));

        taskSchedulingService.putTasksForDay(2026, 5, 1, request);

        assertAll(() -> verify(taskRepository, times(1)).saveAll(argThat(x -> {
            for (final var task : x) {
                if (task.getRoom().getId().equals(room1.getId()) && task.getStatus() != TaskStatus.IN_PROGRESS) {
                    return false;
                }
                if (task.getRoom().getId().equals(room2.getId()) && task.getStatus() != TaskStatus.BLOCKED) {
                    return false;
                }
            }
            return true;
        })));
    }

    @Test
    public void solveTaskScheduling_whenValidAndSolvable_returnsTaskSchedule() {
        final Employee employee1 = Employee.builder().id(1L).firstName("John").lastName("Doe").build();
        final Employee employee2 = Employee.builder().id(2L).firstName("Jane").lastName("Doe").build();

        final Room room1 = Room.builder()
                .id(1L)
                .roomNumber("123")
                .floor(0)
                .svgPoints(List.of(new Point(0.0, 0.0), new Point(0.5, 1.0), new Point(1.0, 0.0)))
                .build();
        final Room room2 = Room.builder()
                .id(2L)
                .roomNumber("456")
                .floor(0)
                .svgPoints(List.of(new Point(10.0, 0.0), new Point(10.5, 1.0), new Point(11.0, 0.0)))
                .build();
        final Room room3 = Room.builder()
                .id(3L)
                .roomNumber("789")
                .floor(1)
                .svgPoints(List.of(new Point(0.0, 10.0), new Point(0.5, 11.0), new Point(1.0, 10.0)))
                .build();

        final TaskScheduleSolveRequestDto request = TaskScheduleSolveRequestDto.builder()
                .startTime(LocalDateTime.of(2026, 5, 1, 8, 0))
                .employeeIds(List.of(1L, 2L))
                .constraints(List.of(
                        TaskConstraintDto.builder().roomId(1L).duration(10L).taskType(TaskType.STAYOVER_CLEAN).build(),
                        TaskConstraintDto.builder()
                                .roomId(2L)
                                .duration(10L)
                                .taskType(TaskType.CHECKOUT_CLEAN)
                                .maxEndTime(LocalDateTime.of(2026, 5, 1, 10, 0))
                                .build(),
                        TaskConstraintDto.builder().roomId(3L).duration(10L).taskType(TaskType.DEEP_CLEAN).build()))
                .build();

        when(employeeRepository.findAllById(argThat(x -> iterableToSet(x).containsAll(List.of(1L, 2L)))))
                .thenReturn(List.of(employee1, employee2));
        when(roomRepository.findAllById(argThat(x -> iterableToSet(x).containsAll(List.of(1L, 2L, 3L)))))
                .thenReturn(List.of(room1, room2, room3));

        final List<CleaningTaskDto> result = taskSchedulingService.solveTaskScheduling(request, 2, 5, 50);

        assertAll(() -> assertThat(result).hasSize(3),
                () -> assertThat(result).map(CleaningTaskDto::getRoomId).containsExactlyInAnyOrder(1L, 2L, 3L));
    }

    @Test
    public void putTasksForDay_whenEmployeeHasOverlappingTasks_throwsException() {
        final Employee employee1 = Employee.builder().id(1L).firstName("John").lastName("Doe").build();
        final Room room1 = Room.builder().id(1L).roomNumber("101").build();
        final Room room2 = Room.builder().id(2L).roomNumber("102").build();

        final LocalDateTime start = LocalDateTime.of(2026, 5, 1, 10, 0);

        // Create tasks where employee 1 is assigned to two rooms with overlapping times
        final List<CleaningTaskDto> request = List.of(
                CleaningTaskDto.builder()
                        .scheduledStart(start)
                        .scheduledEnd(start.plusHours(1)) // 10:00 - 11:00
                        .taskType(TaskType.CHECKOUT_CLEAN)
                        .employeeId(1L)
                        .roomId(1L)
                        .build(),
                CleaningTaskDto.builder()
                        .scheduledStart(start.plusMinutes(30)) // 10:30 - 11:30 (overlaps with first task)
                        .scheduledEnd(start.plusMinutes(90))
                        .taskType(TaskType.STAYOVER_CLEAN)
                        .employeeId(1L)
                        .roomId(2L)
                        .build());

        when(employeeRepository.findAllById(argThat(x -> iterableToSet(x).contains(1L))))
                .thenReturn(List.of(employee1));
        when(roomRepository.findAllById(argThat(x -> iterableToSet(x).containsAll(List.of(1L, 2L)))))
                .thenReturn(List.of(room1, room2));

        assertThatThrownBy(() -> taskSchedulingService.putTasksForDay(2026, 5, 1, request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("John Doe cannot work on room 101")
                .hasMessageContaining("room 102")
                .hasMessageContaining("at the same time");
    }

    @Test
    public void putTasksForDay_whenEmployeeHasNonOverlappingTasks_succeeds() {
        final Employee employee1 = Employee.builder().id(1L).firstName("John").lastName("Doe").build();
        final Room room1 = Room.builder().id(1L).roomNumber("123").build();
        final Room room2 = Room.builder().id(2L).roomNumber("456").build();

        final LocalDateTime start = LocalDateTime.of(2026, 5, 1, 10, 0);

        // Create tasks where employee 1 works on two rooms but at different times
        final List<CleaningTaskDto> request = List.of(
                CleaningTaskDto.builder()
                        .scheduledStart(start)
                        .scheduledEnd(start.plusHours(1)) // 10:00 - 11:00
                        .taskType(TaskType.CHECKOUT_CLEAN)
                        .employeeId(1L)
                        .roomId(1L)
                        .build(),
                CleaningTaskDto.builder()
                        .scheduledStart(start.plusHours(1)) // 11:00 - 12:00 (no overlap)
                        .scheduledEnd(start.plusHours(2))
                        .taskType(TaskType.STAYOVER_CLEAN)
                        .employeeId(1L)
                        .roomId(2L)
                        .build());

        when(employeeRepository.findAllById(argThat(x -> iterableToSet(x).contains(1L))))
                .thenReturn(List.of(employee1));
        when(roomRepository.findAllById(argThat(x -> iterableToSet(x).containsAll(List.of(1L, 2L)))))
                .thenReturn(List.of(room1, room2));
        when(taskRepository.findAllTasksInTimeframe(any(), any())).thenReturn(List.of());

        // Should not throw exception
        taskSchedulingService.putTasksForDay(2026, 5, 1, request);

        verify(taskRepository, times(1)).saveAll(any());
    }

}
