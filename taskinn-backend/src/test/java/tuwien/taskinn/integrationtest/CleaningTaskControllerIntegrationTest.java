package tuwien.taskinn.integrationtest;

import com.fasterxml.jackson.core.type.TypeReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.request.FinishCleaningRequestDto;
import tuwien.taskinn.dto.request.SkipCleaningRequestDto;
import tuwien.taskinn.dto.request.StartCleaningRequestDto;
import tuwien.taskinn.dto.response.CleaningTaskDto;
import tuwien.taskinn.entity.CleaningTask;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.Point;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.enums.TaskStatus;
import tuwien.taskinn.enums.TaskType;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.CleaningTaskRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.repo.RoomTypeRepository;

import java.util.Arrays;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CleaningTask Controller Integration Tests")
public class CleaningTaskControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String CLEANING_TASK_BASE_URI = BASE_URI + "/cleaning-task";

    private String adminToken;
    private String employeeToken;
    private RoomType roomType;
    private Employee employee;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private CleaningTaskRepository cleaningTaskRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        adminToken = getAdminAuthToken();
        employeeToken = getEmployeeAuthToken();

        roomType = roomTypeRepository
                .save(RoomType.builder().name("Standard").capacity(2).basePrice(BigDecimal.valueOf(100)).build());

        employee = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .roles(Set.of(Roles.EMPLOYEE))
                .build());
    }

    @Test
    @DisplayName("POST /api/v1/cleaning-task/start -> Fails with 403 Forbidden for unauthenticated user")
    void startCleaning_whenUnauthenticated_thenForbidden() throws Exception {
        StartCleaningRequestDto requestDto = StartCleaningRequestDto.builder().roomId(1L).build();

        mockMvc.perform(post(CLEANING_TASK_BASE_URI + "/start").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/cleaning-task/start -> Starts cleaning successfully with valid data")
    void startCleaning_whenValidData_thenStartsCleaning() throws Exception {
        Room room = createRoomForCleaning("101");
        createAndSaveCleaningTask(room, employee, TaskStatus.PENDING);
        String userToken = generateToken(employee);

        StartCleaningRequestDto requestDto = StartCleaningRequestDto.builder().roomId(room.getId()).build();

        var mvcResult = mockMvc
                .perform(
                        post(CLEANING_TASK_BASE_URI + "/start").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        CleaningTaskDto apiResponse = objectMapper.readValue(responseBody, CleaningTaskDto.class);

        assertAll(() -> assertThat(apiResponse).isNotNull(),
                () -> assertThat(apiResponse.getRoomId()).isEqualTo(room.getId()),
                () -> assertThat(apiResponse.getRoomNumber()).isEqualTo("101"),
                () -> assertThat(apiResponse.getTaskType()).isEqualTo(TaskType.CHECKOUT_CLEAN),
                () -> assertThat(apiResponse.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS),
                () -> assertThat(apiResponse.getEmployeeName()).isEqualTo("John Doe"));
    }

    @Test
    @DisplayName("POST /api/v1/cleaning-task/start -> Fails with 404 when room does not exist")
    void startCleaning_whenRoomNotFound_thenNotFound() throws Exception {
        String userToken = generateToken(employee);
        StartCleaningRequestDto requestDto = StartCleaningRequestDto.builder().roomId(999L).build();

        mockMvc.perform(post(CLEANING_TASK_BASE_URI + "/start").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/cleaning-task/start -> Fails with 400 when roomId is null")
    void startCleaning_whenRoomIdIsNull_thenBadRequest() throws Exception {
        String userToken = generateToken(employee);
        StartCleaningRequestDto requestDto = StartCleaningRequestDto.builder().roomId(null).build();

        mockMvc.perform(post(CLEANING_TASK_BASE_URI + "/start").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/cleaning-task/finish -> Finishes cleaning successfully with valid data")
    void finishCleaning_whenValidData_thenFinishesCleaning() throws Exception {
        Room room = createRoomForCleaning("102");
        Employee testEmployee = employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        CleaningTask task = createAndSaveCleaningTask(room, testEmployee, TaskStatus.IN_PROGRESS);
        String userToken = generateToken(testEmployee);

        FinishCleaningRequestDto requestDto =
                FinishCleaningRequestDto.builder().roomId(room.getId()).notes("Task completed").build();

        var mvcResult = mockMvc.perform(
                post(CLEANING_TASK_BASE_URI + "/finish").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        CleaningTaskDto apiResponse = objectMapper.readValue(responseBody, CleaningTaskDto.class);

        assertAll(() -> assertThat(apiResponse).isNotNull(),
                () -> assertThat(apiResponse.getRoomId()).isEqualTo(room.getId()),
                () -> assertThat(apiResponse.getRoomNumber()).isEqualTo("102"),
                () -> assertThat(apiResponse.getStatus()).isEqualTo(TaskStatus.COMPLETED),
                () -> assertThat(apiResponse.getNotes()).isEqualTo("Task completed"),
                () -> assertThat(apiResponse.getActualStart()).isNotNull(),
                () -> assertThat(apiResponse.getActualEnd()).isNotNull());
    }

    @Test
    @DisplayName("POST /api/v1/cleaning-task/finish -> Fails with 400 when room does not exist")
    void finishCleaning_whenRoomNotFound_thenBadRequest() throws Exception {
        String userToken = generateToken(employee);
        FinishCleaningRequestDto requestDto =
                FinishCleaningRequestDto.builder().roomId(999L).notes("Task completed").build();

        mockMvc.perform(
                post(CLEANING_TASK_BASE_URI + "/finish").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/cleaning-task/room/{roomId} -> Returns current task for room when exists")
    void getCurrentTaskForRoom_whenTaskExists_returnsTask() throws Exception {
        Room room = createRoomForCleaning("103");
        createAndSaveCleaningTask(room, employee, TaskStatus.IN_PROGRESS);

        var mvcResult = mockMvc.perform(get(CLEANING_TASK_BASE_URI + "/room/" + room.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        CleaningTaskDto apiResponse = objectMapper.readValue(responseBody, CleaningTaskDto.class);

        assertAll(() -> assertThat(apiResponse).isNotNull(),
                () -> assertThat(apiResponse.getRoomId()).isEqualTo(room.getId()),
                () -> assertThat(apiResponse.getRoomNumber()).isEqualTo("103"),
                () -> assertThat(apiResponse.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("GET /api/v1/cleaning-task/room/{roomId} -> Returns 204 when no current task exists")
    void getCurrentTaskForRoom_whenNoTaskExists_returnsNoContent() throws Exception {
        Room room = createRoomForCleaning("104");

        mockMvc.perform(get(CLEANING_TASK_BASE_URI + "/room/" + room.getId()).header(HttpHeaders.AUTHORIZATION,
                "Bearer " + employeeToken)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/cleaning-task/room/{roomId} -> Fails with 403 Forbidden for unauthenticated user")
    void getCurrentTaskForRoom_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get(CLEANING_TASK_BASE_URI + "/room/1")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/cleaning-task/in-progress -> Returns all in-progress tasks")
    void getAllInProgressTasks_whenTasksExist_returnsTasks() throws Exception {
        Room room1 = createRoomForCleaning("105");
        Room room2 = createRoomForCleaning("106");

        Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob.johnson@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        createAndSaveCleaningTask(room1, employee, TaskStatus.IN_PROGRESS);
        createAndSaveCleaningTask(room2, employee2, TaskStatus.IN_PROGRESS);
        Room room3 = createRoomForCleaning("107");
        createAndSaveCleaningTask(room3, employee, TaskStatus.PENDING);

        var mvcResult = mockMvc.perform(get(CLEANING_TASK_BASE_URI + "/in-progress").header(HttpHeaders.AUTHORIZATION,
                "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<CleaningTaskDto> apiResponse =
                objectMapper.readValue(responseBody, new TypeReference<List<CleaningTaskDto>>() {});

        assertAll(() -> assertThat(apiResponse).isNotNull().hasSize(2),
                () -> assertThat(apiResponse).allMatch(task -> task.getStatus() == TaskStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("GET /api/v1/cleaning-task/in-progress -> Returns empty list when no in-progress tasks")
    void getAllInProgressTasks_whenNoTasksExists_returnsEmptyList() throws Exception {
        var mvcResult = mockMvc.perform(get(CLEANING_TASK_BASE_URI + "/in-progress").header(HttpHeaders.AUTHORIZATION,
                "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<CleaningTaskDto> apiResponse =
                objectMapper.readValue(responseBody, new TypeReference<List<CleaningTaskDto>>() {});

        assertThat(apiResponse).isEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/cleaning-task/in-progress -> Fails with 403 Forbidden for unauthenticated user")
    void getAllInProgressTasks_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get(CLEANING_TASK_BASE_URI + "/in-progress")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/cleaning-task/skip -> Skips cleaning successfully with valid data")
    void skipCleaning_whenValidData_thenSkipsCleaning() throws Exception {
        Room room = createRoomForCleaning("108");
        Employee testEmployee = employeeRepository.save(Employee.builder()
                .firstName("Mary")
                .lastName("Williams")
                .email("mary.williams@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        CleaningTask task = createAndSaveCleaningTask(room, testEmployee, TaskStatus.IN_PROGRESS);
        String userToken = generateToken(testEmployee);

        SkipCleaningRequestDto requestDto =
                SkipCleaningRequestDto.builder().roomId(room.getId()).reason("Guest requested privacy").build();

        var mvcResult = mockMvc
                .perform(post(CLEANING_TASK_BASE_URI + "/skip").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        CleaningTaskDto apiResponse = objectMapper.readValue(responseBody, CleaningTaskDto.class);

        assertAll(() -> assertThat(apiResponse).isNotNull(),
                () -> assertThat(apiResponse.getRoomId()).isEqualTo(room.getId()),
                () -> assertThat(apiResponse.getRoomNumber()).isEqualTo("108"),
                () -> assertThat(apiResponse.getStatus()).isEqualTo(TaskStatus.BLOCKED),
                () -> assertThat(apiResponse.getNotes()).isEqualTo("Guest requested privacy"));
    }

    @Test
    @DisplayName("POST /api/v1/cleaning-task/skip -> Fails with 404 when room does not exist")
    void skipCleaning_whenRoomNotFound_thenNotFound() throws Exception {
        String userToken = generateToken(employee);
        SkipCleaningRequestDto requestDto =
                SkipCleaningRequestDto.builder().roomId(999L).reason("Guest requested privacy").build();

        mockMvc.perform(post(CLEANING_TASK_BASE_URI + "/skip").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/cleaning-task/skip -> Fails with 403 Forbidden for unauthenticated user")
    void skipCleaning_whenUnauthenticated_thenForbidden() throws Exception {
        SkipCleaningRequestDto requestDto =
                SkipCleaningRequestDto.builder().roomId(1L).reason("Guest requested privacy").build();

        mockMvc.perform(post(CLEANING_TASK_BASE_URI + "/skip").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isForbidden());
    }

    private Room createRoomForCleaning(String roomNumber) {
        return roomRepository.save(Room.builder()
                .roomNumber(roomNumber)
                .floor(1)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .svgPoints(new ArrayList<>(Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0),
                        new Point(100.0, 100.0), new Point(0.0, 100.0))))
                .roomType(roomType)
                .build());
    }

    private CleaningTask createAndSaveCleaningTask(Room room, Employee employee, TaskStatus status) {
        CleaningTask.CleaningTaskBuilder builder = CleaningTask.builder()
                .room(room)
                .employee(employee)
                .taskType(TaskType.CHECKOUT_CLEAN)
                .status(status)
                .scheduledStart(LocalDateTime.now())
                .scheduledEnd(LocalDateTime.now().plusHours(1))
                .createdAt(Instant.now())
                .updatedAt(Instant.now());

        // Set actualStart if task is in progress
        if (status == TaskStatus.IN_PROGRESS) {
            builder.actualStart(LocalDateTime.now());
        }

        return cleaningTaskRepository.save(builder.build());
    }
}
