package tuwien.taskinn.integrationtest;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.CleaningTaskDto;
import tuwien.taskinn.dto.request.TaskConstraintDto;
import tuwien.taskinn.dto.request.TaskScheduleSolveRequestDto;
import tuwien.taskinn.entity.CleaningTask;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.enums.TaskStatus;
import tuwien.taskinn.enums.TaskType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TaskSchedulingControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String TASK_SCHEDULING_BASE_URI = BASE_URI + "/task-scheduling";

    @Test
    public void getTasksForDay_whenTasksExistAndAuthorized_returnTasksForDay() throws Exception {
        final Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());
        final Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        final RoomType roomType = roomTypeRepository.save(RoomType.builder()
                .name("cool room")
                .description("cool room")
                .capacity(2)
                .basePrice(BigDecimal.ONE)
                .singleBed(0)
                .doubleBed(1)
                .build());

        final Room room1 = createAndSaveRoom("101", 1, roomType);
        final Room room2 = createAndSaveRoom("102", 1, roomType);

        final LocalDateTime start = LocalDateTime.of(2026, 5, 1, 8, 0);
        final CleaningTask task1 = taskRepository.save(CleaningTask.builder()
                .scheduledStart(start)
                .scheduledEnd(start.plusMinutes(15))
                .taskType(TaskType.DEEP_CLEAN)
                .status(TaskStatus.PENDING)
                .employee(employee1)
                .room(room1)
                .build());
        final CleaningTask task2 = taskRepository.save(CleaningTask.builder()
                .scheduledStart(start)
                .scheduledEnd(start.plusMinutes(15))
                .taskType(TaskType.DEEP_CLEAN)
                .status(TaskStatus.PENDING)
                .employee(employee2)
                .room(room2)
                .build());
        taskRepository.save(CleaningTask.builder()
                .scheduledStart(start)
                .scheduledEnd(start.plusDays(1).plusMinutes(15))
                .taskType(TaskType.DEEP_CLEAN)
                .status(TaskStatus.PENDING)
                .employee(employee1)
                .room(room1)
                .build());
        taskRepository.save(CleaningTask.builder()
                .scheduledStart(start)
                .scheduledEnd(start.plusDays(1).plusMinutes(15))
                .taskType(TaskType.DEEP_CLEAN)
                .status(TaskStatus.PENDING)
                .employee(employee2)
                .room(room2)
                .build());

        final MvcResult mvcResult =
                mockMvc.perform(get(TASK_SCHEDULING_BASE_URI + "/schedule/2026/5/1").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + getAdminAuthToken())).andExpect(status().isOk()).andReturn();

        final String responseString = mvcResult.getResponse().getContentAsString();
        final List<CleaningTaskDto> result = objectMapper.readValue(responseString, new TypeReference<>() {});

        assertAll(() -> assertThat(result).hasSize(2), () -> assertThat(result).map(CleaningTaskDto::getId)
                .containsExactlyInAnyOrder(task1.getId(), task2.getId()));
    }

    @Test
    public void getTasksForDay_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(get(TASK_SCHEDULING_BASE_URI + "/schedule/2026/5/1")).andExpect(status().isForbidden());
    }

    @Test
    public void getTasksForEmployeeAndDay_whenTasksExistAndAuthorized_returnTasksForDay() throws Exception {
        final Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());
        final Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        final RoomType roomType = roomTypeRepository.save(RoomType.builder()
                .name("cool room")
                .description("cool room")
                .capacity(2)
                .basePrice(BigDecimal.ONE)
                .singleBed(0)
                .doubleBed(1)
                .build());

        final Room room1 = createAndSaveRoom("101", 1, roomType);
        final Room room2 = createAndSaveRoom("102", 1, roomType);

        final LocalDateTime start = LocalDateTime.of(2026, 5, 1, 8, 0);
        final CleaningTask task1 = taskRepository.save(CleaningTask.builder()
                .scheduledStart(start)
                .scheduledEnd(start.plusMinutes(15))
                .taskType(TaskType.DEEP_CLEAN)
                .status(TaskStatus.PENDING)
                .employee(employee1)
                .room(room1)
                .build());
        taskRepository.save(CleaningTask.builder()
                .scheduledStart(start)
                .scheduledEnd(start.plusMinutes(15))
                .taskType(TaskType.DEEP_CLEAN)
                .status(TaskStatus.PENDING)
                .employee(employee2)
                .room(room2)
                .build());
        taskRepository.save(CleaningTask.builder()
                .scheduledStart(start)
                .scheduledEnd(start.plusDays(1).plusMinutes(15))
                .taskType(TaskType.DEEP_CLEAN)
                .status(TaskStatus.PENDING)
                .employee(employee1)
                .room(room1)
                .build());
        taskRepository.save(CleaningTask.builder()
                .scheduledStart(start)
                .scheduledEnd(start.plusDays(1).plusMinutes(15))
                .taskType(TaskType.DEEP_CLEAN)
                .status(TaskStatus.PENDING)
                .employee(employee2)
                .room(room2)
                .build());

        final MvcResult mvcResult = mockMvc
                .perform(get(TASK_SCHEDULING_BASE_URI + "/schedule/2026/5/1/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken()))
                .andExpect(status().isOk())
                .andReturn();

        final String responseString = mvcResult.getResponse().getContentAsString();
        final List<CleaningTaskDto> result = objectMapper.readValue(responseString, new TypeReference<>() {});

        assertAll(() -> assertThat(result).hasSize(1),
                () -> assertThat(result).map(CleaningTaskDto::getId).containsExactlyInAnyOrder(task1.getId()));
    }

    @Test
    public void getTasksForEmployeeAndDay_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(get(TASK_SCHEDULING_BASE_URI + "/schedule/2026/5/1/1")).andExpect(status().isForbidden());
    }

    @Test
    public void putTasksForDay_whenValidAndAuthorized_putsTasks() throws Exception {
        final Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        final RoomType roomType = roomTypeRepository.save(RoomType.builder()
                .name("cool room")
                .description("cool room")
                .capacity(2)
                .basePrice(BigDecimal.ONE)
                .singleBed(0)
                .doubleBed(1)
                .build());

        final Room room1 = createAndSaveRoom("101", 1, roomType);
        final Room room2 = createAndSaveRoom("102", 1, roomType);

        final LocalDateTime start = LocalDateTime.of(2026, 5, 1, 8, 0);
        final CleaningTask task1 = taskRepository.save(CleaningTask.builder()
                .scheduledStart(start)
                .scheduledEnd(start.plusMinutes(15))
                .taskType(TaskType.DEEP_CLEAN)
                .status(TaskStatus.PENDING)
                .employee(employee1)
                .room(room1)
                .build());

        final List<CleaningTaskDto> request = List.of(
                CleaningTaskDto.builder()
                        .scheduledStart(start.plusDays(1))
                        .scheduledEnd(start.plusDays(1).plusMinutes(10))
                        .taskType(TaskType.DEEP_CLEAN)
                        .employeeId(employee1.getId())
                        .roomId(room1.getId())
                        .build(),
                CleaningTaskDto.builder()
                        .scheduledStart(start.plusDays(1).plusMinutes(20))
                        .scheduledEnd(start.plusDays(1).plusMinutes(30))
                        .taskType(TaskType.DEEP_CLEAN)
                        .employeeId(employee1.getId())
                        .roomId(room2.getId())
                        .build());

        mockMvc.perform(put(TASK_SCHEDULING_BASE_URI + "/schedule/2026/5/2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

        final List<CleaningTask> tasks = taskRepository.findAll();

        assertAll(() -> assertThat(tasks).hasSize(3),
                () -> assertThat(tasks).map(CleaningTask::getId).contains(task1.getId()),
                () -> assertThat(tasks).map(CleaningTask::getRoom)
                        .map(Room::getId)
                        .containsExactlyInAnyOrderElementsOf(List.of(room1.getId(), room1.getId(), room2.getId())));
    }

    @Test
    public void putTasksForDay_whenValidAndAuthorizedAndPreviousTasksExist_putsTasksWithPreviousStatusAsDefault()
            throws Exception {
        final Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        final RoomType roomType = roomTypeRepository.save(RoomType.builder()
                .name("cool room")
                .description("cool room")
                .capacity(2)
                .basePrice(BigDecimal.ONE)
                .singleBed(0)
                .doubleBed(1)
                .build());

        final Room room1 = createAndSaveRoom("101", 1, roomType);
        final Room room2 = createAndSaveRoom("102", 1, roomType);

        final LocalDateTime start = LocalDateTime.of(2026, 5, 1, 8, 0);
        final CleaningTask task1 = taskRepository.save(CleaningTask.builder()
                .scheduledStart(start)
                .scheduledEnd(start.plusMinutes(15))
                .taskType(TaskType.DEEP_CLEAN)
                .status(TaskStatus.IN_PROGRESS)
                .employee(employee1)
                .room(room1)
                .build());
        final CleaningTask task2 = taskRepository.save(CleaningTask.builder()
                .scheduledStart(start.plusMinutes(20))
                .scheduledEnd(start.plusMinutes(35))
                .taskType(TaskType.DEEP_CLEAN)
                .status(TaskStatus.PENDING)
                .employee(employee1)
                .room(room2)
                .build());

        final List<CleaningTaskDto> request = List.of(
                CleaningTaskDto.builder()
                        .scheduledStart(start)
                        .scheduledEnd(start.plusMinutes(10))
                        .taskType(TaskType.DEEP_CLEAN)
                        .employeeId(employee1.getId())
                        .roomId(room1.getId())
                        .build(),
                CleaningTaskDto.builder()
                        .scheduledStart(start.plusMinutes(20))
                        .scheduledEnd(start.plusMinutes(30))
                        .taskType(TaskType.DEEP_CLEAN)
                        .employeeId(employee1.getId())
                        .roomId(room2.getId())
                        .build());

        mockMvc.perform(put(TASK_SCHEDULING_BASE_URI + "/schedule/2026/5/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

        final List<CleaningTask> tasks = taskRepository.findAll();
        final Map<Long, CleaningTask> roomTaskMap =
                tasks.stream().collect(Collectors.toMap(x -> x.getRoom().getId(), x -> x));
        assertAll(() -> assertThat(tasks).hasSize(2),
                () -> assertThat(roomTaskMap.get(room1.getId()).getStatus()).isEqualTo(TaskStatus.IN_PROGRESS),
                () -> assertThat(roomTaskMap.get(room2.getId()).getStatus()).isEqualTo(TaskStatus.PENDING));
    }

    @Test
    public void putTasksForDay_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(put(TASK_SCHEDULING_BASE_URI + "/schedule/2026/5/1").contentType(MediaType.APPLICATION_JSON)
                .content("[]")).andExpect(status().isForbidden());
    }

    @Test
    public void solveTaskScheduling_whenValidAndAuthorized_returnsTaskSchedule() throws Exception {
        final Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());
        final Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        final RoomType roomType = roomTypeRepository.save(RoomType.builder()
                .name("cool room")
                .description("cool room")
                .capacity(2)
                .basePrice(BigDecimal.ONE)
                .singleBed(0)
                .doubleBed(1)
                .build());

        final Room room1 = createAndSaveRoom("101", 1, roomType);
        final Room room2 = createAndSaveRoom("102", 1, roomType);
        final Room room3 = createAndSaveRoom("103", 1, roomType);
        final Room room4 = createAndSaveRoom("104", 1, roomType);

        final TaskScheduleSolveRequestDto request = TaskScheduleSolveRequestDto.builder()
                .startTime(LocalDateTime.of(2026, 5, 1, 8, 0))
                .employeeIds(List.of(employee1.getId(), employee2.getId()))
                .constraints(List.of(
                        TaskConstraintDto.builder()
                                .roomId(room1.getId())
                                .duration(10L)
                                .taskType(TaskType.DEEP_CLEAN)
                                .build(),
                        TaskConstraintDto.builder()
                                .roomId(room2.getId())
                                .duration(10L)
                                .taskType(TaskType.DEEP_CLEAN)
                                .build(),
                        TaskConstraintDto.builder()
                                .roomId(room3.getId())
                                .duration(10L)
                                .taskType(TaskType.DEEP_CLEAN)
                                .build(),
                        TaskConstraintDto.builder()
                                .roomId(room4.getId())
                                .duration(10L)
                                .taskType(TaskType.DEEP_CLEAN)
                                .build()))
                .build();

        final MvcResult mvcResult = mockMvc.perform(post(TASK_SCHEDULING_BASE_URI + "/optimized-schedule")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk()).andReturn();

        final String responseString = mvcResult.getResponse().getContentAsString();
        final List<CleaningTaskDto> result = objectMapper.readValue(responseString, new TypeReference<>() {});

        assertAll(() -> assertThat(result).hasSize(4), () -> assertThat(result).map(CleaningTaskDto::getRoomId)
                .containsExactlyInAnyOrder(room1.getId(), room2.getId(), room3.getId(), room4.getId()));
    }

    @Test
    public void solveTaskScheduling_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(post(TASK_SCHEDULING_BASE_URI + "/optimized-schedule").contentType(MediaType.APPLICATION_JSON)
                .content("[]")).andExpect(status().isForbidden());
    }

    @Test
    public void solveTaskScheduling_whenTasksOverlapIntoNextDay_returnsInternalServerError() throws Exception {
        // Given - create scenario where tasks would extend beyond day end
        final Employee employee = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.overlap@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        final RoomType roomType = roomTypeRepository.save(RoomType.builder()
                .name("standard room")
                .description("standard room")
                .capacity(2)
                .basePrice(BigDecimal.ONE)
                .singleBed(1)
                .doubleBed(0)
                .build());

        final Room room = createAndSaveRoom("105", 1, roomType);

        // Start very late in the day (23:00) with a long duration task that would extend into next day
        final TaskScheduleSolveRequestDto request = TaskScheduleSolveRequestDto.builder()
                .startTime(LocalDateTime.of(2026, 5, 1, 23, 0)) // Start at 11 PM
                .employeeIds(List.of(employee.getId()))
                .constraints(List.of(TaskConstraintDto.builder()
                        .roomId(room.getId())
                        .duration(120L) // 2 hour task starting at 23:00 would end at 01:00 next day
                        .taskType(TaskType.DEEP_CLEAN)
                        .build()))
                .build();

        // When & Then - should return 500 Internal Server Error due to SolverFailureException
        mockMvc.perform(post(TASK_SCHEDULING_BASE_URI + "/optimized-schedule")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isInternalServerError());
    }

}
