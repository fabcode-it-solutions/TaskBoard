package tuwien.taskinn.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.request.MaintenanceTicketCreationUpdateDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.Point;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.Priority;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.enums.TicketStatus;
import tuwien.taskinn.services.MaintenanceService;

import java.math.BigDecimal;
import java.util.List;

@DisplayName("Maintenance Controller Integration Tests")
public class MaintenanceControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    @Autowired
    private MaintenanceService maintenanceService;

    private static final String MAINTENANCE_BASE_URI = BASE_URI + "/maintenance";

    private String employeeToken;
    private Employee testReporter;
    private Room testRoom;

    /**
     * Sets up common test data before each test method runs. This includes creating a standard employee user and a room
     * to create tickets for.
     */
    @BeforeEach
    void setUp() {
        testReporter = createAndSaveEmployee("reporter@test.com", Roles.EMPLOYEE);
        employeeToken = generateToken(testReporter);

        RoomType roomType = RoomType.builder()
                .name("Standard")
                .description("Standard room type for testing")
                .capacity(2)
                .basePrice(BigDecimal.valueOf(100))
                .singleBed(1)
                .doubleBed(0)
                .build();

        roomTypeRepository.save(roomType);
        testRoom = roomRepository.save(Room.builder()
                .roomNumber("101")
                .floor(1)
                .status(RoomStatus.AVAILABLE)
                .roomType(roomType)
                .active(true)
                .svgPoints(List.of(new Point(70.0, 10.0), new Point(120.0, 10.0), new Point(120.0, 40.0),
                        new Point(70.0, 40.0)))
                .build());
    }

    @Test
    @DisplayName("POST /maintenance -> Fails with 403 for unauthenticated user")
    void createTicket_whenUnauthenticated_thenFailsWith403() throws Exception {
        var createDto = new MaintenanceTicketCreationUpdateDto();

        mockMvc.perform(post(MAINTENANCE_BASE_URI).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /maintenance -> Successfully creates a new ticket")
    void createTicket_whenAuthenticatedAndDataIsValid_thenCreatesTicket() throws Exception {
        var createDto = MaintenanceTicketCreationUpdateDto.builder()
                .title("Leaky Faucet")
                .description("The faucet in the bathroom is dripping.")
                .priority(Priority.MEDIUM)
                .roomId(testRoom.getId())
                .build();

        mockMvc.perform(post(MAINTENANCE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated()) // Verify 201 Created
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Leaky Faucet"))
                .andExpect(jsonPath("$.status").value("OPEN")) // Should default to OPEN
                .andExpect(jsonPath("$.reporterId").value(testReporter.getId()))
                .andExpect(jsonPath("$.roomId").value(testRoom.getId()))
                .andExpect(jsonPath("$.roomNumber").value("101"));

        assertThat(maintenanceTicketRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /maintenance/{id} -> Successfully retrieves an existing ticket")
    void getTicketById_whenTicketExists_thenReturnsTicket() throws Exception {
        var createDto = new MaintenanceTicketCreationUpdateDto(null, "Broken TV", "Remote is not working", Priority.LOW,
                null, testRoom.getId());
        maintenanceService.createMaintenanceTicket(createDto, testReporter.getEmail());
        Long ticketId = maintenanceTicketRepository.findAll().getFirst().getId();

        mockMvc.perform(
                get(MAINTENANCE_BASE_URI + "/" + ticketId).header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.title").value("Broken TV"));
    }

    @Test
    @DisplayName("GET /maintenance/{id} -> Fails with 404 for non-existent ticket")
    void getTicketById_whenTicketDoesNotExist_thenFailsWith404() throws Exception {
        mockMvc.perform(get(MAINTENANCE_BASE_URI + "/999").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /maintenance/{id} -> Successfully updates a ticket")
    void updateTicket_whenDataIsValid_thenUpdatesSuccessfully() throws Exception {
        var createDto = new MaintenanceTicketCreationUpdateDto(null, "Initial Title", "Initial Desc", Priority.HIGH,
                null, testRoom.getId());
        maintenanceService.createMaintenanceTicket(createDto, testReporter.getEmail());
        Long ticketId = maintenanceTicketRepository.findAll().get(0).getId();

        var updateDto = new MaintenanceTicketCreationUpdateDto(ticketId, "Updated Title", "Updated Desc",
                Priority.MEDIUM, TicketStatus.IN_PROGRESS, testRoom.getId());

        mockMvc.perform(
                put(MAINTENANCE_BASE_URI + "/" + ticketId).header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        var updatedTicket = maintenanceTicketRepository.findById(ticketId).orElseThrow();
        assertAll(() -> assertThat(updatedTicket.getTitle()).isEqualTo("Updated Title"),
                () -> assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("DELETE /maintenance/{id} -> Successfully deletes a ticket")
    void deleteTicket_whenTicketExists_thenDeletesSuccessfully() throws Exception {
        var createDto = new MaintenanceTicketCreationUpdateDto(null, "To be deleted", "Desc", Priority.LOW, null,
                testRoom.getId());
        maintenanceService.createMaintenanceTicket(createDto, testReporter.getEmail());
        Long ticketId = maintenanceTicketRepository.findAll().get(0).getId();
        assertThat(maintenanceTicketRepository.count()).isEqualTo(1);

        mockMvc.perform(delete(MAINTENANCE_BASE_URI + "/" + ticketId).header(HttpHeaders.AUTHORIZATION,
                "Bearer " + employeeToken)).andExpect(status().isNoContent());

        assertThat(maintenanceTicketRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("GET /maintenance -> Fails with 403 for unauthenticated user")
    void getAllTickets_whenUnauthenticated_thenFailsWith403() throws Exception {
        mockMvc.perform(get(MAINTENANCE_BASE_URI)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /maintenance/{id} -> Fails with 403 for unauthenticated user")
    void getTicketById_whenUnauthenticated_thenFailsWith403() throws Exception {
        mockMvc.perform(get(MAINTENANCE_BASE_URI + "/1")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /maintenance/{id} -> Fails with 403 for unauthenticated user")
    void updateTicket_whenUnauthenticated_thenFailsWith403() throws Exception {
        var updateDto = MaintenanceTicketCreationUpdateDto.builder().title("test").build();
        mockMvc.perform(put(MAINTENANCE_BASE_URI + "/1").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /maintenance/{id} -> Fails with 403 for unauthenticated user")
    void deleteTicket_whenUnauthenticated_thenFailsWith403() throws Exception {
        mockMvc.perform(delete(MAINTENANCE_BASE_URI + "/1")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /maintenance -> Fails with 400 Bad Request for blank title")
    void createTicket_whenTitleIsBlank_thenFailsWith400() throws Exception {
        var createDto = MaintenanceTicketCreationUpdateDto.builder()
                .title("") // Blank title - this should trigger @NotBlank
                .description("A valid description.")
                .priority(Priority.LOW)
                .roomId(testRoom.getId())
                .build();

        mockMvc.perform(post(MAINTENANCE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("title"));
    }

    @Test
    @DisplayName("POST /maintenance -> Fails with 400 Bad Request for null room ID")
    void createTicket_whenRoomIdIsNull_thenFailsWith400() throws Exception {
        var createDto = MaintenanceTicketCreationUpdateDto.builder()
                .title("Valid Title")
                .description("A valid description.")
                .priority(Priority.LOW)
                .roomId(null)
                .build();

        mockMvc.perform(post(MAINTENANCE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("roomId"));
    }

    @Test
    @DisplayName("GET /maintenance -> Returns a list of all existing tickets")
    void getAllTickets_whenTicketsExist_thenReturnsListOfTickets() throws Exception {
        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Ticket One", "Desc 1",
                Priority.HIGH, null, testRoom.getId()), testReporter.getEmail());
        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Ticket Two", "Desc 2",
                Priority.LOW, null, testRoom.getId()), testReporter.getEmail());

        mockMvc.perform(get(MAINTENANCE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.title == 'Ticket One')]").exists())
                .andExpect(jsonPath("$[?(@.title == 'Ticket Two')]").exists());
    }

    // ==================== GET /api/v1/maintenance/open ====================

    @Test
    @DisplayName("GET /maintenance/open -> Fails with 403 for unauthenticated user")
    void getAllOpenTickets_whenUnauthenticated_thenFailsWith403() throws Exception {
        mockMvc.perform(get(MAINTENANCE_BASE_URI + "/open")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /maintenance/open -> Returns empty list when no open tickets exist")
    void getAllOpenTickets_whenNoOpenTicketsExist_thenReturnsEmptyList() throws Exception {
        // Create tickets with non-OPEN statuses
        var ticket1 = maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null,
                "Resolved Ticket", "Desc", Priority.HIGH, null, testRoom.getId()), testReporter.getEmail());

        // Update to RESOLVED status
        maintenanceService.updateMaintenanceTicket(ticket1.getId(), new MaintenanceTicketCreationUpdateDto(
                ticket1.getId(), "Resolved Ticket", "Desc", Priority.HIGH, TicketStatus.RESOLVED, testRoom.getId()));

        mockMvc.perform(
                get(MAINTENANCE_BASE_URI + "/open").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /maintenance/open -> Returns only tickets with OPEN status")
    void getAllOpenTickets_whenOpenTicketsExist_thenReturnsOnlyOpenTickets() throws Exception {
        // Create OPEN tickets (default status)
        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Open Ticket 1",
                "Description 1", Priority.HIGH, null, testRoom.getId()), testReporter.getEmail());

        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Open Ticket 2",
                "Description 2", Priority.MEDIUM, null, testRoom.getId()), testReporter.getEmail());

        // Create ticket and update to IN_PROGRESS
        var ticket3 = maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null,
                "In Progress Ticket", "Desc", Priority.LOW, null, testRoom.getId()), testReporter.getEmail());
        maintenanceService.updateMaintenanceTicket(ticket3.getId(),
                new MaintenanceTicketCreationUpdateDto(ticket3.getId(), "In Progress Ticket", "Desc", Priority.LOW,
                        TicketStatus.IN_PROGRESS, testRoom.getId()));

        mockMvc.perform(
                get(MAINTENANCE_BASE_URI + "/open").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[1].status").value("OPEN"))
                .andExpect(jsonPath("$[?(@.title == 'Open Ticket 1')]").exists())
                .andExpect(jsonPath("$[?(@.title == 'Open Ticket 2')]").exists())
                .andExpect(jsonPath("$[?(@.title == 'In Progress Ticket')]").doesNotExist());
    }

    @Test
    @DisplayName("GET /maintenance/open -> Excludes tickets with IN_PROGRESS, RESOLVED, and CLOSED statuses")
    void getAllOpenTickets_whenMixedStatuses_thenReturnsOnlyOpen() throws Exception {
        // Create one OPEN ticket
        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Open Ticket", "Desc",
                Priority.HIGH, null, testRoom.getId()), testReporter.getEmail());

        // Create and update tickets to different statuses
        var ticket2 = maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null,
                "In Progress", "Desc", Priority.MEDIUM, null, testRoom.getId()), testReporter.getEmail());
        maintenanceService.updateMaintenanceTicket(ticket2.getId(), new MaintenanceTicketCreationUpdateDto(
                ticket2.getId(), "In Progress", "Desc", Priority.MEDIUM, TicketStatus.IN_PROGRESS, testRoom.getId()));

        var ticket3 = maintenanceService.createMaintenanceTicket(
                new MaintenanceTicketCreationUpdateDto(null, "Resolved", "Desc", Priority.LOW, null, testRoom.getId()),
                testReporter.getEmail());
        maintenanceService.updateMaintenanceTicket(ticket3.getId(), new MaintenanceTicketCreationUpdateDto(
                ticket3.getId(), "Resolved", "Desc", Priority.LOW, TicketStatus.RESOLVED, testRoom.getId()));

        var ticket4 = maintenanceService.createMaintenanceTicket(
                new MaintenanceTicketCreationUpdateDto(null, "Closed", "Desc", Priority.HIGH, null, testRoom.getId()),
                testReporter.getEmail());
        maintenanceService.updateMaintenanceTicket(ticket4.getId(), new MaintenanceTicketCreationUpdateDto(
                ticket4.getId(), "Closed", "Desc", Priority.HIGH, TicketStatus.CLOSED, testRoom.getId()));

        mockMvc.perform(
                get(MAINTENANCE_BASE_URI + "/open").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Open Ticket"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("GET /maintenance/open -> Returns multiple open tickets with correct details")
    void getAllOpenTickets_whenMultipleOpenTickets_thenReturnsAllWithCorrectDetails() throws Exception {
        // Create multiple OPEN tickets
        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Leaky Faucet",
                "Bathroom faucet dripping", Priority.HIGH, null, testRoom.getId()), testReporter.getEmail());

        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Broken AC",
                "Air conditioning not working", Priority.MEDIUM, null, testRoom.getId()), testReporter.getEmail());

        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Light Bulb",
                "Replace light bulb in hallway", Priority.LOW, null, testRoom.getId()), testReporter.getEmail());

        mockMvc.perform(
                get(MAINTENANCE_BASE_URI + "/open").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[1].status").value("OPEN"))
                .andExpect(jsonPath("$[2].status").value("OPEN"))
                .andExpect(jsonPath("$[*].title")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("Leaky Faucet", "Broken AC", "Light Bulb")));
    }

    // ==================== GET /api/v1/maintenance/open/high-priority/count ====================

    @Test
    @DisplayName("GET /maintenance/open/high-priority/count -> Fails with 403 for unauthenticated user")
    void countHighPriorityTickets_whenUnauthenticated_thenFailsWith403() throws Exception {
        mockMvc.perform(get(MAINTENANCE_BASE_URI + "/open/high-priority/count")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /maintenance/open/high-priority/count -> Returns 0 when no high priority tickets exist")
    void countHighPriorityTickets_whenNoHighPriorityTickets_thenReturnsZero() throws Exception {
        // Create tickets with LOW and MEDIUM priority
        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Low Priority Ticket",
                "Description", Priority.LOW, null, testRoom.getId()), testReporter.getEmail());

        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null,
                "Medium Priority Ticket", "Description", Priority.MEDIUM, null, testRoom.getId()),
                testReporter.getEmail());

        var mvcResult = mockMvc.perform(get(MAINTENANCE_BASE_URI + "/open/high-priority/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("GET /maintenance/open/high-priority/count -> Returns correct count for HIGH priority OPEN tickets")
    void countHighPriorityTickets_whenHighPriorityOpenTickets_thenReturnsCorrectCount() throws Exception {
        // Create HIGH priority OPEN tickets (default status is OPEN)
        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "High Priority 1",
                "Description", Priority.HIGH, null, testRoom.getId()), testReporter.getEmail());

        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "High Priority 2",
                "Description", Priority.HIGH, null, testRoom.getId()), testReporter.getEmail());

        var mvcResult = mockMvc.perform(get(MAINTENANCE_BASE_URI + "/open/high-priority/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("GET /maintenance/open/high-priority/count -> Returns correct count for URGENT priority tickets")
    void countHighPriorityTickets_whenUrgentPriorityTickets_thenReturnsCorrectCount() throws Exception {
        // Create URGENT priority OPEN tickets
        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Urgent Ticket 1",
                "Description", Priority.URGENT, null, testRoom.getId()), testReporter.getEmail());

        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Urgent Ticket 2",
                "Description", Priority.URGENT, null, testRoom.getId()), testReporter.getEmail());

        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Urgent Ticket 3",
                "Description", Priority.URGENT, null, testRoom.getId()), testReporter.getEmail());

        var mvcResult = mockMvc.perform(get(MAINTENANCE_BASE_URI + "/open/high-priority/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("GET /maintenance/open/high-priority/count -> Counts both HIGH and URGENT priority tickets")
    void countHighPriorityTickets_whenMixedHighAndUrgentPriorities_thenCountsBoth() throws Exception {
        // Create mix of HIGH and URGENT priority tickets
        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "High Priority",
                "Description", Priority.HIGH, null, testRoom.getId()), testReporter.getEmail());

        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Urgent Priority 1",
                "Description", Priority.URGENT, null, testRoom.getId()), testReporter.getEmail());

        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Urgent Priority 2",
                "Description", Priority.URGENT, null, testRoom.getId()), testReporter.getEmail());

        var mvcResult = mockMvc.perform(get(MAINTENANCE_BASE_URI + "/open/high-priority/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("GET /maintenance/open/high-priority/count -> Includes IN_PROGRESS high priority tickets")
    void countHighPriorityTickets_whenInProgressHighPriorityTickets_thenIncludesThem() throws Exception {
        // Create high priority OPEN ticket
        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "High Priority Open",
                "Description", Priority.HIGH, null, testRoom.getId()), testReporter.getEmail());

        // Create and update to IN_PROGRESS
        var ticket2 = maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null,
                "High Priority In Progress", "Description", Priority.HIGH, null, testRoom.getId()),
                testReporter.getEmail());
        maintenanceService.updateMaintenanceTicket(ticket2.getId(),
                new MaintenanceTicketCreationUpdateDto(ticket2.getId(), "High Priority In Progress", "Description",
                        Priority.HIGH, TicketStatus.IN_PROGRESS, testRoom.getId()));

        var mvcResult = mockMvc.perform(get(MAINTENANCE_BASE_URI + "/open/high-priority/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        // Should count both OPEN and IN_PROGRESS
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("GET /maintenance/open/high-priority/count -> Excludes RESOLVED and CLOSED high priority tickets")
    void countHighPriorityTickets_whenResolvedOrClosedHighPriority_thenExcludesThem() throws Exception {
        // Create high priority OPEN ticket
        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "High Priority Open",
                "Description", Priority.HIGH, null, testRoom.getId()), testReporter.getEmail());

        // Create and update to RESOLVED
        var ticket2 = maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null,
                "High Priority Resolved", "Description", Priority.HIGH, null, testRoom.getId()),
                testReporter.getEmail());
        maintenanceService.updateMaintenanceTicket(ticket2.getId(),
                new MaintenanceTicketCreationUpdateDto(ticket2.getId(), "High Priority Resolved", "Description",
                        Priority.HIGH, TicketStatus.RESOLVED, testRoom.getId()));

        // Create and update to CLOSED
        var ticket3 = maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null,
                "High Priority Closed", "Description", Priority.URGENT, null, testRoom.getId()),
                testReporter.getEmail());
        maintenanceService.updateMaintenanceTicket(ticket3.getId(),
                new MaintenanceTicketCreationUpdateDto(ticket3.getId(), "High Priority Closed", "Description",
                        Priority.URGENT, TicketStatus.CLOSED, testRoom.getId()));

        var mvcResult = mockMvc.perform(get(MAINTENANCE_BASE_URI + "/open/high-priority/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        // Should only count the OPEN ticket, not RESOLVED or CLOSED
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("GET /maintenance/open/high-priority/count -> Excludes LOW and MEDIUM priority tickets regardless of status")
    void countHighPriorityTickets_whenLowAndMediumPriority_thenExcludesThem() throws Exception {
        // Create high priority tickets
        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "High Priority",
                "Description", Priority.HIGH, null, testRoom.getId()), testReporter.getEmail());

        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Urgent Priority",
                "Description", Priority.URGENT, null, testRoom.getId()), testReporter.getEmail());

        // Create low and medium priority tickets (should not be counted)
        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Low Priority",
                "Description", Priority.LOW, null, testRoom.getId()), testReporter.getEmail());

        maintenanceService.createMaintenanceTicket(new MaintenanceTicketCreationUpdateDto(null, "Medium Priority",
                "Description", Priority.MEDIUM, null, testRoom.getId()), testReporter.getEmail());

        var mvcResult = mockMvc.perform(get(MAINTENANCE_BASE_URI + "/open/high-priority/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        // Should only count HIGH and URGENT priority tickets
        assertThat(count).isEqualTo(2L);
    }
}
