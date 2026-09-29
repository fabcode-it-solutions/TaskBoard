package tuwien.taskinn.integrationtest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.PointDto;
import tuwien.taskinn.dto.request.RoomRequestDto;
import tuwien.taskinn.dto.response.FloorOccupancyDto;
import tuwien.taskinn.dto.response.LightRoomDto;
import tuwien.taskinn.dto.response.RoomDto;
import tuwien.taskinn.entity.Point;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.Guest;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.repo.RoomTypeRepository;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Room Controller Integration Tests")
public class RoomControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String ROOM_BASE_URI = BASE_URI + "/room";

    private String adminToken;
    private String employeeToken;
    private RoomType standardRoomType;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @BeforeEach
    void setUp() {
        adminToken = getAdminAuthToken();
        employeeToken = getEmployeeAuthToken();

        standardRoomType = roomTypeRepository.save(RoomType.builder()
                .name("Standard")
                .capacity(2)
                .doubleBed(1)
                .basePrice(BigDecimal.valueOf(100))
                .build());
    }

    // ==================== GET /api/v1/room ====================

    @Test
    @DisplayName("GET /api/v1/room -> Fails with 403 Forbidden for unauthenticated user")
    void getAllRooms_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get(ROOM_BASE_URI)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/room -> Returns empty list when no rooms exist")
    void getAllRooms_whenNoRooms_thenReturnsEmptyList() throws Exception {
        // When
        var mvcResult = mockMvc.perform(get(ROOM_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<LightRoomDto> apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then
        assertThat(apiResponse).isEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/room -> Returns list of rooms for authenticated user")
    void getAllRooms_whenAuthenticated_thenReturnsRoomList() throws Exception {
        // Given
        Room room1 = createAndSaveRoom("101", 1, standardRoomType);
        Room room2 = createAndSaveRoom("102", 1, standardRoomType);

        // When
        var mvcResult = mockMvc.perform(get(ROOM_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<LightRoomDto> apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then
        assertAll(() -> assertThat(apiResponse).hasSize(2),
                () -> assertThat(apiResponse).extracting(LightRoomDto::getRoomNumber)
                        .containsExactlyInAnyOrder("101", "102"),
                () -> assertThat(apiResponse).allMatch(dto -> dto.getId() != null));
    }

    // ==================== POST /api/v1/room ====================

    @Test
    @DisplayName("POST /api/v1/room -> Fails with 403 Forbidden for unauthenticated user")
    void createRoom_whenUnauthenticated_thenForbidden() throws Exception {
        RoomRequestDto requestDto = createValidRoomRequestDto(1L, "201", 2);

        mockMvc.perform(post(ROOM_BASE_URI).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/room -> Fails with 403 Forbidden for non-admin user (employee)")
    void createRoom_whenAuthenticatedAsEmployee_thenForbidden() throws Exception {
        // Given
        RoomRequestDto requestDto = createValidRoomRequestDto(standardRoomType.getId(), "201", 2);

        // When & Then
        mockMvc.perform(post(ROOM_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/room -> Creates room successfully with valid data")
    void createRoom_whenValidData_thenCreatesRoom() throws Exception {
        // Given
        RoomRequestDto requestDto = createValidRoomRequestDto(standardRoomType.getId(), "301", 3);

        // When
        var mvcResult = mockMvc
                .perform(post(ROOM_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        RoomDto apiResponse = objectMapper.readValue(responseBody, RoomDto.class);

        // Then
        assertAll(() -> assertThat(apiResponse.getId()).isNotNull(),
                () -> assertThat(apiResponse.getNumber()).isEqualTo("301"),
                () -> assertThat(apiResponse.getFloor()).isEqualTo(3),
                () -> assertThat(apiResponse.getStatus()).isEqualTo(RoomStatus.AVAILABLE),
                () -> assertThat(apiResponse.getRoomTypeId()).isEqualTo(standardRoomType.getId()),
                () -> assertThat(apiResponse.getSvgPoints()).hasSize(4));
    }

    @Test
    @DisplayName("POST /api/v1/room -> Fails with 404 when room type does not exist")
    void createRoom_whenRoomTypeNotFound_thenBadRequest() throws Exception {
        // Given
        RoomRequestDto requestDto = createValidRoomRequestDto(999L, "401", 4);

        // When & Then
        mockMvc.perform(post(ROOM_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/room -> Fails with 400 when SVG points overlap with existing room")
    void createRoom_whenOverlappingRooms_thenBadRequest() throws Exception {
        // Create first room
        createAndSaveRoom("501", 5, standardRoomType);

        // Try to create overlapping room
        RoomRequestDto requestDto = createValidRoomRequestDto(standardRoomType.getId(), "502", 5);

        // When & Then
        mockMvc.perform(post(ROOM_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/room -> Fails with 400 when SVG points are less than 3")
    void createRoom_whenLessThan3Points_thenBadRequest() throws Exception {
        // Given
        RoomRequestDto requestDto = RoomRequestDto.builder()
                .roomNumber("601")
                .floor(6)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(Arrays.asList(new PointDto(0.0, 0.0), new PointDto(100.0, 0.0))) // Only 2 points - invalid
                .roomTypeId(standardRoomType.getId())
                .build();

        // When & Then
        mockMvc.perform(post(ROOM_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/room -> Fails with 400 when room number doesn't start with floor number")
    void createRoom_whenRoomNumberDoesNotMatchFloorPrefix_thenBadRequest() throws Exception {
        // Given - Room number "201" on floor 3 (should be 301)
        RoomRequestDto requestDto = RoomRequestDto.builder()
                .roomNumber("201")
                .floor(3)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(Arrays.asList(new PointDto(0.0, 0.0), new PointDto(100.0, 0.0), new PointDto(100.0, 100.0),
                        new PointDto(0.0, 100.0)))
                .roomTypeId(standardRoomType.getId())
                .build();

        // When & Then
        mockMvc.perform(post(ROOM_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/room -> Fails with 400 when room number is just the floor number without additional digits")
    void createRoom_whenRoomNumberIsOnlyFloorNumber_thenBadRequest() throws Exception {
        // Given - Room number "5" on floor 5 (should be 501, 502, etc.)
        RoomRequestDto requestDto = RoomRequestDto.builder()
                .roomNumber("5")
                .floor(5)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(Arrays.asList(new PointDto(0.0, 0.0), new PointDto(100.0, 0.0), new PointDto(100.0, 100.0),
                        new PointDto(0.0, 100.0)))
                .roomTypeId(standardRoomType.getId())
                .build();

        // When & Then
        mockMvc.perform(post(ROOM_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    // ==================== GET /api/v1/room/{id} ====================

    @Test
    @DisplayName("GET /api/v1/room/{id} -> Fails with 403 Forbidden for unauthenticated user")
    void getRoomById_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get(ROOM_BASE_URI + "/1")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/room/{id} -> Returns room when exists")
    void getRoomById_whenRoomExists_thenReturnsRoom() throws Exception {
        // Given
        Room room = createAndSaveRoom("701", 7, standardRoomType);

        // When
        var mvcResult = mockMvc.perform(
                get(ROOM_BASE_URI + "/" + room.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        RoomDto apiResponse = objectMapper.readValue(responseBody, RoomDto.class);

        // Then
        assertAll(() -> assertThat(apiResponse.getId()).isEqualTo(room.getId()),
                () -> assertThat(apiResponse.getNumber()).isEqualTo("701"),
                () -> assertThat(apiResponse.getFloor()).isEqualTo(7),
                () -> assertThat(apiResponse.getRoomTypeId()).isEqualTo(standardRoomType.getId()));
    }

    @Test
    @DisplayName("GET /api/v1/room/{id} -> Fails with 404 when room does not exist")
    void getRoomById_whenRoomNotFound_thenBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get(ROOM_BASE_URI + "/999").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ==================== PUT /api/v1/room/{id} ====================

    @Test
    @DisplayName("PUT /api/v1/room/{id} -> Fails with 403 Forbidden for unauthenticated user")
    void updateRoom_whenUnauthenticated_thenForbidden() throws Exception {
        RoomRequestDto requestDto = createValidRoomRequestDto(1L, "801", 8);

        mockMvc.perform(put(ROOM_BASE_URI + "/1").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/room/{id} -> Updates room successfully with valid data")
    void updateRoom_whenValidData_thenUpdatesRoom() throws Exception {
        // Given
        Room existingRoom = createAndSaveRoom("901", 9, standardRoomType);

        RoomRequestDto updateDto = RoomRequestDto.builder()
                .id(existingRoom.getId())
                .roomNumber("901")
                .floor(9)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(Arrays.asList(new PointDto(0.0, 0.0), new PointDto(100.0, 0.0), new PointDto(100.0, 100.0),
                        new PointDto(0.0, 100.0)))
                .roomTypeId(standardRoomType.getId())
                .build();

        // When
        var mvcResult = mockMvc.perform(put(ROOM_BASE_URI + "/" + existingRoom.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        RoomDto apiResponse = objectMapper.readValue(responseBody, RoomDto.class);

        // Then
        assertAll(() -> assertThat(apiResponse.getId()).isEqualTo(existingRoom.getId()),
                () -> assertThat(apiResponse.getNumber()).isEqualTo("901"),
                () -> assertThat(apiResponse.getStatus()).isEqualTo(RoomStatus.AVAILABLE));
    }

    @Test
    @DisplayName("PUT /api/v1/room/{id} -> Fails with 404 when room does not exist")
    void updateRoom_whenRoomNotFound_thenBadRequest() throws Exception {
        // Given
        RoomRequestDto updateDto = createValidRoomRequestDto(1L, "999", 9);
        updateDto.setId(999L);

        // When & Then
        mockMvc.perform(put(ROOM_BASE_URI + "/999").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/room/{id} -> Fails with 400 when updating to a room number that doesn't match floor prefix")
    void updateRoom_whenRoomNumberDoesNotMatchFloorPrefix_thenBadRequest() throws Exception {
        // Given
        Room existingRoom = createAndSaveRoom("801", 8, standardRoomType);

        RoomRequestDto updateDto = RoomRequestDto.builder()
                .id(existingRoom.getId())
                .roomNumber("901") // Wrong prefix for floor 8
                .floor(8)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(Arrays.asList(new PointDto(0.0, 0.0), new PointDto(100.0, 0.0), new PointDto(100.0, 100.0),
                        new PointDto(0.0, 100.0)))
                .roomTypeId(standardRoomType.getId())
                .build();

        // When & Then
        mockMvc.perform(put(ROOM_BASE_URI + "/" + existingRoom.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/room/{id} -> Successfully updates room with valid capacity matching bed count")
    void updateRoom_whenCapacityMatchesBedCount_thenUpdatesSuccessfully() throws Exception {
        // Given
        RoomType roomTypeWithBeds = roomTypeRepository.save(RoomType.builder()
                .name("Standard with Beds")
                .capacity(4)
                .basePrice(BigDecimal.valueOf(150))
                .singleBed(2)
                .doubleBed(1)
                .build());

        Room existingRoom = createAndSaveRoom("2201", 22, roomTypeWithBeds);

        // Update with capacity = 5 (1 single + 2 double = 1 + 2*2 = 5)
        RoomRequestDto updateDto = RoomRequestDto.builder()
                .id(existingRoom.getId())
                .roomNumber("2201")
                .floor(22)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .svgPoints(Arrays.asList(new PointDto(0.0, 0.0), new PointDto(100.0, 0.0), new PointDto(100.0, 100.0),
                        new PointDto(0.0, 100.0)))
                .roomTypeId(roomTypeWithBeds.getId())
                .capacityOverride(5)
                .singleBedOverride(1)
                .doubleBedOverride(2)
                .build();

        // When
        var mvcResult = mockMvc.perform(put(ROOM_BASE_URI + "/" + existingRoom.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        RoomDto apiResponse = objectMapper.readValue(responseBody, RoomDto.class);

        // Then
        assertAll(() -> assertThat(apiResponse.getId()).isEqualTo(existingRoom.getId()),
                () -> assertThat(apiResponse.getCapacityOverride()).isEqualTo(5),
                () -> assertThat(apiResponse.getSingleBedOverride()).isEqualTo(1),
                () -> assertThat(apiResponse.getDoubleBedOverride()).isEqualTo(2));
    }

    @Test
    @DisplayName("PUT /api/v1/room/{id} -> Fails with 400 when capacity does not match bed count")
    void updateRoom_whenCapacityDoesNotMatchBedCount_thenBadRequest() throws Exception {
        // Given
        RoomType roomTypeWithBeds = roomTypeRepository.save(RoomType.builder()
                .name("Deluxe with Beds")
                .capacity(4)
                .basePrice(BigDecimal.valueOf(200))
                .singleBed(2)
                .doubleBed(1)
                .build());

        Room existingRoom = createAndSaveRoom("2301", 23, roomTypeWithBeds);

        // Update with capacity = 3 but beds = 2 single + 2 double = 2 + 2*2 = 6 (mismatch)
        RoomRequestDto updateDto = RoomRequestDto.builder()
                .id(existingRoom.getId())
                .roomNumber("2301")
                .floor(23)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .svgPoints(Arrays.asList(new PointDto(0.0, 0.0), new PointDto(100.0, 0.0), new PointDto(100.0, 100.0),
                        new PointDto(0.0, 100.0)))
                .roomTypeId(roomTypeWithBeds.getId())
                .capacityOverride(3)
                .singleBedOverride(2)
                .doubleBedOverride(2)
                .build();

        // When & Then
        mockMvc.perform(put(ROOM_BASE_URI + "/" + existingRoom.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("capacityOverride"))
                .andExpect(jsonPath("$.validationErrors[0].message")
                        .value("Capacity must equal the sum of single and double beds."));
    }

    // ==================== GET /api/v1/room/floor/{floor} ====================

    @Test
    @DisplayName("GET /api/v1/room/floor/{floor} -> Fails with 403 Forbidden for unauthenticated user")
    void getRoomsByFloor_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get(ROOM_BASE_URI + "/floor/1")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/room/floor/{floor} -> Returns only active rooms, excludes inactive rooms")
    void getRoomsByFloor_whenInactiveRoomsExist_thenReturnsOnlyActiveRooms() throws Exception {
        // Given
        Room activeRoom1 = createAndSaveRoom("1401", 14, standardRoomType);
        Room activeRoom2 = createAndSaveRoom("1402", 14, standardRoomType);
        Room inactiveRoom = createAndSaveRoom("1403", 14, standardRoomType);

        // Soft delete the third room (set it as inactive)
        inactiveRoom.setActive(false);
        roomRepository.save(inactiveRoom);

        // When
        var mvcResult = mockMvc
                .perform(get(ROOM_BASE_URI + "/floor/14").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<RoomDto> apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then - should only return active rooms
        assertAll(() -> assertThat(apiResponse).hasSize(2),
                () -> assertThat(apiResponse).extracting(RoomDto::getNumber).containsExactlyInAnyOrder("1401", "1402"),
                () -> assertThat(apiResponse).allMatch(dto -> dto.getFloor() == 14),
                () -> assertThat(apiResponse).noneMatch(dto -> dto.getNumber().equals("1403")));
    }

    // ==================== GET /api/v1/room/max-floor ====================

    @Test
    @DisplayName("GET /api/v1/room/max-floor -> Fails with 403 Forbidden for unauthenticated user")
    void getMaxFloor_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get(ROOM_BASE_URI + "/max-floor")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/room/max-floor -> Returns null when no rooms exist")
    void getMaxFloor_whenNoRoomsExist_thenReturnsNull() throws Exception {
        // When
        var mvcResult = mockMvc
                .perform(get(ROOM_BASE_URI + "/max-floor").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        // Then - should return empty response or null
        assertThat(responseBody).isEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/room/max-floor -> Returns highest floor number when rooms exist")
    void getMaxFloor_whenRoomsExist_thenReturnsMaxFloor() throws Exception {
        // Given
        createAndSaveRoom("1501", 15, standardRoomType);
        createAndSaveRoom("2001", 20, standardRoomType);
        createAndSaveRoom("0501", 5, standardRoomType);

        // When
        var mvcResult = mockMvc
                .perform(get(ROOM_BASE_URI + "/max-floor").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Integer maxFloor = objectMapper.readValue(responseBody, Integer.class);

        // Then - should return 20 as the highest floor
        assertThat(maxFloor).isEqualTo(20);
    }

    // ==================== DELETE /api/v1/room/{room}/disable ====================

    @Test
    @DisplayName("DELETE /rooms/{id}/disable -> Succeeds (204) and disables room for ADMIN with no conflicts")
    void deactivateRoomPermanently_whenUserIsAdminAndNoConflicts_succeedsAndDisablesRoom() throws Exception {
        Room roomToDisable = roomRepository.save(Room.builder()
                .roomNumber("101")
                .floor(1)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .active(true)
                .roomType(standardRoomType)
                .build());

        mockMvc.perform(delete(ROOM_BASE_URI + "/" + roomToDisable.getId() + "/disable")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isNoContent());

        Room updatedRoom = roomRepository.findById(roomToDisable.getId()).orElseThrow();
        assertThat(updatedRoom.isActive()).isFalse();
    }

    @Test
    @DisplayName("DELETE /rooms/{id}/disable -> Fails (403) for a regular EMPLOYEE")
    void deactivateRoomPermanently_whenUserIsEmployee_failsWith403Forbidden() throws Exception {
        Room room = roomRepository.save(Room.builder()
                .roomNumber("102")
                .floor(1)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .active(true)
                .roomType(standardRoomType)
                .build());

        mockMvc.perform(delete(ROOM_BASE_URI + "/" + room.getId() + "/disable").header(HttpHeaders.AUTHORIZATION,
                "Bearer " + employeeToken)).andExpect(status().isForbidden());

        Room finalRoomState = roomRepository.findById(room.getId()).orElseThrow();
        assertThat(finalRoomState.isActive()).isTrue();
    }

    @Test
    @DisplayName("DELETE /rooms/{id}/disable -> Fails (409) when room has future booking and NO replacement is available")
    void deactivateRoomPermanently_whenRoomHasFutureBookingAndNoReplacement_failsWith409Conflict() throws Exception {
        Room roomToDisable = roomRepository.save(Room.builder()
                .roomNumber("201")
                .floor(2)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .active(true)
                .roomType(standardRoomType)
                .build());

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john.doe@email.com")
                .build());
        Booking conflictingBooking = bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(10))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(roomToDisable))
                .numAdults(2)
                .numChildren(0)
                .build());

        String expectedMessage = String.format(
                "Cannot disable room. No alternative room of type '%s' is available for booking #%d (%s to %s).",
                standardRoomType.getName(), conflictingBooking.getId(), conflictingBooking.getCheckInDate(),
                conflictingBooking.getCheckOutDate());

        mockMvc.perform(delete(ROOM_BASE_URI + "/" + roomToDisable.getId() + "/disable")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(expectedMessage));

        Room finalRoomState = roomRepository.findById(roomToDisable.getId()).orElseThrow();
        assertThat(finalRoomState.isActive()).isTrue();
    }

    @Test
    @DisplayName("DELETE /rooms/{id}/disable -> Succeeds (204) when room has future booking and a replacement IS available")
    void deactivateRoomPermanently_whenRoomHasFutureBookingAndReplacementExists_succeedsAndReallocates()
            throws Exception {
        Room roomToDisable = roomRepository.save(Room.builder()
                .roomNumber("301")
                .floor(3)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .active(true)
                .roomType(standardRoomType)
                .build());
        Room replacementRoom = roomRepository.save(Room.builder()
                .roomNumber("302")
                .floor(3)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .active(true)
                .roomType(standardRoomType)
                .build());

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("guest2@test.com")
                .build());
        Booking bookingToReallocate = bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now().plusDays(15))
                .checkOutDate(LocalDate.now().plusDays(20))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(roomToDisable))
                .numAdults(2)
                .numChildren(0)
                .build());

        mockMvc.perform(delete(ROOM_BASE_URI + "/" + roomToDisable.getId() + "/disable")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isNoContent());

        Room finalDisabledRoom = roomRepository.findById(roomToDisable.getId()).orElseThrow();
        Booking updatedBooking = bookingRepository.findById(bookingToReallocate.getId()).orElseThrow();

        assertAll("Verify reallocation and deactivation in database",
                () -> assertThat(finalDisabledRoom.isActive()).isFalse(), () -> {
                    assertThat(updatedBooking.getRooms()).hasSize(1);
                    assertThat(updatedBooking.getRooms().iterator().next().getId()).isEqualTo(replacementRoom.getId());
                });
    }

    @Test
    @DisplayName("GET /room/available -> Succeeds (200) and returns available rooms for valid date range")
    void getAvailableRooms_whenValidDateRange_returnsAvailableRooms() throws Exception {
        Room availableRoom1 = roomRepository.save(Room.builder()
                .roomNumber("401")
                .floor(4)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .svgPoints(Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .roomType(standardRoomType)
                .build());
        Room availableRoom2 = roomRepository.save(Room.builder()
                .roomNumber("402")
                .floor(4)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .svgPoints(Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .roomType(standardRoomType)
                .build());

        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);

        mockMvc.perform(get(ROOM_BASE_URI + "/available").param("checkInDate", checkInDate.toString())
                .param("checkOutDate", checkOutDate.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].roomNumber").exists())
                .andExpect(jsonPath("$[0].roomType.name").value("Standard"))
                .andExpect(jsonPath("$[0].roomType.basePrice").value(100));
    }

    @Test
    @DisplayName("GET /room/available -> Excludes rooms with bookings in the requested date range")
    void getAvailableRooms_whenRoomIsBooked_excludesBookedRoom() throws Exception {
        Room bookedRoom = roomRepository.save(Room.builder()
                .roomNumber("501")
                .floor(5)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .svgPoints(Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .roomType(standardRoomType)
                .build());
        Room availableRoom = roomRepository.save(Room.builder()
                .roomNumber("502")
                .floor(5)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .svgPoints(Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .roomType(standardRoomType)
                .build());

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("User")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("test@test.com")
                .build());

        LocalDate checkInDate = LocalDate.now().plusDays(10);
        LocalDate checkOutDate = LocalDate.now().plusDays(15);

        bookingRepository.save(Booking.builder()
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(bookedRoom))
                .numAdults(2)
                .numChildren(0)
                .build());

        mockMvc.perform(get(ROOM_BASE_URI + "/available").param("checkInDate", checkInDate.toString())
                .param("checkOutDate", checkOutDate.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roomNumber").value("502"));
    }

    @Test
    @DisplayName("GET /room/available -> Excludes inactive rooms from results")
    void getAvailableRooms_whenRoomIsInactive_excludesInactiveRoom() throws Exception {
        Room activeRoom = roomRepository.save(Room.builder()
                .roomNumber("601")
                .floor(6)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .svgPoints(Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .roomType(standardRoomType)
                .build());
        Room inactiveRoom = roomRepository.save(Room.builder()
                .roomNumber("602")
                .floor(6)
                .status(RoomStatus.AVAILABLE)
                .active(false)
                .svgPoints(Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .roomType(standardRoomType)
                .build());

        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);

        mockMvc.perform(get(ROOM_BASE_URI + "/available").param("checkInDate", checkInDate.toString())
                .param("checkOutDate", checkOutDate.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roomNumber").value("601"));
    }

    // ==================== POST /api/v1/room/copy-floor ====================

    @Test
    @DisplayName("POST /api/v1/room/copy-floor -> Fails with 403 Forbidden for unauthenticated user")
    void copyFloor_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(post(ROOM_BASE_URI + "/copy-floor").param("fromFloor", "1").param("toFloor", "2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/room/copy-floor -> Fails with 403 Forbidden for non-admin user (employee)")
    void copyFloor_whenAuthenticatedAsEmployee_thenForbidden() throws Exception {
        mockMvc.perform(post(ROOM_BASE_URI + "/copy-floor").param("fromFloor", "1")
                .param("toFloor", "2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/room/copy-floor -> Successfully copies rooms from one floor to another")
    void copyFloor_whenValidRequest_thenCopiesRoomsSuccessfully() throws Exception {
        // Given - Create rooms on floor 1
        Room room1 = createAndSaveRoom("101", 1, standardRoomType);
        Room room2 = createAndSaveRoom("102", 1, standardRoomType);
        Room room3 = createAndSaveRoom("103", 1, standardRoomType);

        // When - Copy floor 1 to floor 2
        var mvcResult = mockMvc
                .perform(post(ROOM_BASE_URI + "/copy-floor").param("fromFloor", "1")
                        .param("toFloor", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<RoomDto> copiedRooms = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then - Verify copied rooms
        assertAll(() -> assertThat(copiedRooms).hasSize(3),
                () -> assertThat(copiedRooms).allMatch(room -> room.getFloor() == 2),
                () -> assertThat(copiedRooms).extracting(RoomDto::getNumber)
                        .containsExactlyInAnyOrder("201", "202", "203"),
                () -> assertThat(copiedRooms).allMatch(room -> room.getRoomTypeId().equals(standardRoomType.getId())),
                () -> assertThat(copiedRooms).allMatch(room -> room.getStatus() == RoomStatus.AVAILABLE));
    }

    @Test
    @DisplayName("POST /api/v1/room/copy-floor -> Returns empty list when source floor has no rooms")
    void copyFloor_whenSourceFloorHasNoRooms_thenReturnsEmptyList() throws Exception {
        // When - Copy from empty floor 5 to floor 6
        var mvcResult = mockMvc
                .perform(post(ROOM_BASE_URI + "/copy-floor").param("fromFloor", "5")
                        .param("toFloor", "6")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<RoomDto> copiedRooms = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then
        assertThat(copiedRooms).isEmpty();
    }

    @Test
    @DisplayName("POST /api/v1/room/copy-floor -> Copies only active rooms, excludes inactive ones")
    void copyFloor_whenSourceFloorHasInactiveRooms_thenCopiesOnlyActiveRooms() throws Exception {
        // Given - Create active and inactive rooms on floor 3
        Room activeRoom1 = createAndSaveRoom("301", 3, standardRoomType);
        Room activeRoom2 = createAndSaveRoom("302", 3, standardRoomType);
        Room inactiveRoom = createAndSaveRoom("303", 3, standardRoomType);
        inactiveRoom.setActive(false);
        roomRepository.save(inactiveRoom);

        // When - Copy floor 3 to floor 4
        var mvcResult = mockMvc
                .perform(post(ROOM_BASE_URI + "/copy-floor").param("fromFloor", "3")
                        .param("toFloor", "4")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<RoomDto> copiedRooms = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then - Only 2 active rooms should be copied
        assertAll(() -> assertThat(copiedRooms).hasSize(2),
                () -> assertThat(copiedRooms).extracting(RoomDto::getNumber).containsExactlyInAnyOrder("401", "402"),
                () -> assertThat(copiedRooms).noneMatch(room -> room.getNumber().equals("303")));
    }

    @Test
    @DisplayName("POST /api/v1/room/copy-floor -> Preserves SVG points and room type when copying")
    void copyFloor_whenCopying_thenPreservesSvgPointsAndRoomType() throws Exception {
        // Given - Create a room with specific SVG points
        Room sourceRoom = roomRepository.save(Room.builder()
                .roomNumber("701")
                .floor(7)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .svgPoints(Arrays.asList(new Point(10.0, 20.0), new Point(110.0, 20.0), new Point(110.0, 120.0),
                        new Point(10.0, 120.0)))
                .roomType(standardRoomType)
                .build());

        // When - Copy floor 7 to floor 8
        var mvcResult = mockMvc
                .perform(post(ROOM_BASE_URI + "/copy-floor").param("fromFloor", "7")
                        .param("toFloor", "8")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<RoomDto> copiedRooms = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then - Verify SVG points are preserved
        assertAll(() -> assertThat(copiedRooms).hasSize(1),
                () -> assertThat(copiedRooms.get(0).getSvgPoints()).hasSize(4),
                () -> assertThat(copiedRooms.get(0).getSvgPoints()).containsExactly(new PointDto(10.0, 20.0),
                        new PointDto(110.0, 20.0), new PointDto(110.0, 120.0), new PointDto(10.0, 120.0)),
                () -> assertThat(copiedRooms.get(0).getRoomTypeId()).isEqualTo(standardRoomType.getId()));
    }

    // ==================== GET /api/v1/room/occupancy-rate/today ====================

    @Test
    @DisplayName("GET /api/v1/room/occupancy-rate/today -> Fails with 403 Forbidden for unauthenticated user")
    void getTodaysOccupancyRate_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get(ROOM_BASE_URI + "/occupancy-rate/today")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/room/occupancy-rate/today -> Returns 0.0 when no rooms exist")
    void getTodaysOccupancyRate_whenNoRoomsExist_thenReturnsZero() throws Exception {
        // When
        var mvcResult = mockMvc.perform(
                get(ROOM_BASE_URI + "/occupancy-rate/today").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Double occupancyRate = objectMapper.readValue(responseBody, Double.class);

        // Then
        assertThat(occupancyRate).isEqualTo(0.0);
    }

    @Test
    @DisplayName("GET /api/v1/room/occupancy-rate/today -> Returns 100.0 when all rooms are occupied")
    void getTodaysOccupancyRate_whenAllRoomsOccupied_thenReturns100() throws Exception {
        // Given
        Room room1 = createAndSaveRoom("801", 8, standardRoomType);
        Room room2 = createAndSaveRoom("802", 8, standardRoomType);

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("test@guest.com")
                .build());

        // Create bookings that include today
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now().minusDays(1))
                .checkOutDate(LocalDate.now().plusDays(2))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(3))
                .guest(guest)
                .status(BookingStatus.CHECKED_IN)
                .rooms(Set.of(room2))
                .numAdults(1)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(get(ROOM_BASE_URI + "/occupancy-rate/today").header(HttpHeaders.AUTHORIZATION,
                "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Double occupancyRate = objectMapper.readValue(responseBody, Double.class);

        // Then
        assertThat(occupancyRate).isEqualTo(100.0);
    }

    @Test
    @DisplayName("GET /api/v1/room/occupancy-rate/today -> Returns 50.0 when half of rooms are occupied")
    void getTodaysOccupancyRate_whenHalfRoomsOccupied_thenReturns50() throws Exception {
        // Given
        Room room1 = createAndSaveRoom("901", 9, standardRoomType);
        Room room2 = createAndSaveRoom("902", 9, standardRoomType);
        Room room3 = createAndSaveRoom("903", 9, standardRoomType);
        Room room4 = createAndSaveRoom("904", 9, standardRoomType);

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("occupancy@test.com")
                .build());

        // Occupy only 2 out of 4 rooms
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now().minusDays(1))
                .checkOutDate(LocalDate.now().plusDays(1))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2))
                .guest(guest)
                .status(BookingStatus.CHECKED_IN)
                .rooms(Set.of(room2))
                .numAdults(1)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(
                get(ROOM_BASE_URI + "/occupancy-rate/today").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Double occupancyRate = objectMapper.readValue(responseBody, Double.class);

        // Then
        assertThat(occupancyRate).isEqualTo(50.0);
    }

    @Test
    @DisplayName("GET /api/v1/room/occupancy-rate/today -> Excludes inactive rooms from calculation")
    void getTodaysOccupancyRate_whenInactiveRoomsExist_thenExcludesThem() throws Exception {
        // Given
        Room activeRoom1 = createAndSaveRoom("1001", 10, standardRoomType);
        Room activeRoom2 = createAndSaveRoom("1002", 10, standardRoomType);
        Room inactiveRoom = createAndSaveRoom("1003", 10, standardRoomType);

        // Deactivate one room
        inactiveRoom.setActive(false);
        roomRepository.save(inactiveRoom);

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("inactive@test.com")
                .build());

        // Occupy 1 active room (50% of active rooms)
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(activeRoom1))
                .numAdults(2)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(
                get(ROOM_BASE_URI + "/occupancy-rate/today").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Double occupancyRate = objectMapper.readValue(responseBody, Double.class);

        // Then - should be 50% (1 occupied out of 2 active rooms)
        assertThat(occupancyRate).isEqualTo(50.0);
    }

    @Test
    @DisplayName("GET /api/v1/room/occupancy-rate/today -> Excludes cancelled bookings from calculation")
    void getTodaysOccupancyRate_whenCancelledBookingsExist_thenExcludesThem() throws Exception {
        // Given
        Room room1 = createAndSaveRoom("1101", 11, standardRoomType);
        Room room2 = createAndSaveRoom("1102", 11, standardRoomType);

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("cancelled@test.com")
                .build());

        // Create a cancelled booking (should not count)
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .guest(guest)
                .status(BookingStatus.CANCELLED)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        // Create a checked-out booking (should not count)
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now().minusDays(2))
                .checkOutDate(LocalDate.now().plusDays(1))
                .guest(guest)
                .status(BookingStatus.CHECKED_OUT)
                .rooms(Set.of(room2))
                .numAdults(1)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(
                get(ROOM_BASE_URI + "/occupancy-rate/today").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Double occupancyRate = objectMapper.readValue(responseBody, Double.class);

        // Then - should be 0% because cancelled and checked-out bookings don't count
        assertThat(occupancyRate).isEqualTo(0.0);
    }

    @Test
    @DisplayName("GET /api/v1/room/occupancy-rate/today -> Excludes past and future bookings from calculation")
    void getTodaysOccupancyRate_whenPastAndFutureBookingsExist_thenExcludesThem() throws Exception {
        // Given
        Room room1 = createAndSaveRoom("1201", 12, standardRoomType);
        Room room2 = createAndSaveRoom("1202", 12, standardRoomType);
        Room room3 = createAndSaveRoom("1203", 12, standardRoomType);

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("time@test.com")
                .build());

        // Past booking (checked out yesterday)
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now().minusDays(5))
                .checkOutDate(LocalDate.now().minusDays(1))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        // Future booking (starts tomorrow)
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(3))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(room2))
                .numAdults(1)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(
                get(ROOM_BASE_URI + "/occupancy-rate/today").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Double occupancyRate = objectMapper.readValue(responseBody, Double.class);

        // Then - should be 0% because no bookings overlap with today
        assertThat(occupancyRate).isEqualTo(0.0);
    }

    // ==================== GET /api/v1/room/floor-occupancy ====================

    @Test
    @DisplayName("GET /api/v1/room/floor-occupancy -> Fails with 403 Forbidden for unauthenticated user")
    void getFloorOccupancyDistribution_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get(ROOM_BASE_URI + "/floor-occupancy")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/room/floor-occupancy -> Returns empty list when no rooms exist")
    void getFloorOccupancyDistribution_whenNoRoomsExist_thenReturnsEmptyList() throws Exception {
        // When
        var mvcResult = mockMvc.perform(
                get(ROOM_BASE_URI + "/floor-occupancy").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<FloorOccupancyDto> floorOccupancy = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then
        assertThat(floorOccupancy).isEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/room/floor-occupancy -> Returns correct data for single floor with all rooms occupied")
    void getFloorOccupancyDistribution_whenSingleFloorFullyOccupied_thenReturns100Percent() throws Exception {
        // Given
        Room room1 = createAndSaveRoom("1301", 13, standardRoomType);
        Room room2 = createAndSaveRoom("1302", 13, standardRoomType);

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("floor@test.com")
                .build());

        // Occupy both rooms
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2))
                .guest(guest)
                .status(BookingStatus.CHECKED_IN)
                .rooms(Set.of(room2))
                .numAdults(2)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(
                get(ROOM_BASE_URI + "/floor-occupancy").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<FloorOccupancyDto> floorOccupancy = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then
        assertThat(floorOccupancy).hasSize(1);
        FloorOccupancyDto floor13 = floorOccupancy.get(0);
        assertAll(() -> assertThat(floor13.getFloor()).isEqualTo(13),
                () -> assertThat(floor13.getTotalRooms()).isEqualTo(2L),
                () -> assertThat(floor13.getOccupiedRooms()).isEqualTo(2L),
                () -> assertThat(floor13.getOccupancyRate()).isEqualTo(100.0));
    }

    @Test
    @DisplayName("GET /api/v1/room/floor-occupancy -> Returns correct data for multiple floors with different occupancy rates")
    void getFloorOccupancyDistribution_whenMultipleFloorsWithDifferentOccupancy_thenReturnsCorrectData()
            throws Exception {
        // Given - Floor 14: 2 rooms, 1 occupied (50%)
        Room floor14Room1 = createAndSaveRoom("1401", 14, standardRoomType);
        Room floor14Room2 = createAndSaveRoom("1402", 14, standardRoomType);

        // Floor 15: 3 rooms, 0 occupied (0%)
        createAndSaveRoom("1501", 15, standardRoomType);
        createAndSaveRoom("1502", 15, standardRoomType);
        createAndSaveRoom("1503", 15, standardRoomType);

        // Floor 16: 4 rooms, 4 occupied (100%)
        Room floor16Room1 = createAndSaveRoom("1601", 16, standardRoomType);
        Room floor16Room2 = createAndSaveRoom("1602", 16, standardRoomType);
        Room floor16Room3 = createAndSaveRoom("1603", 16, standardRoomType);
        Room floor16Room4 = createAndSaveRoom("1604", 16, standardRoomType);

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("multifloor@test.com")
                .build());

        // Occupy 1 room on floor 14
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(floor14Room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        // Occupy all rooms on floor 16
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(floor16Room1, floor16Room2))
                .numAdults(2)
                .numChildren(0)
                .build());

        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .guest(guest)
                .status(BookingStatus.CHECKED_IN)
                .rooms(Set.of(floor16Room3, floor16Room4))
                .numAdults(2)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(
                get(ROOM_BASE_URI + "/floor-occupancy").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<FloorOccupancyDto> floorOccupancy = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then
        assertThat(floorOccupancy).hasSize(3);

        FloorOccupancyDto floor14 =
                floorOccupancy.stream().filter(f -> f.getFloor().equals(14)).findFirst().orElseThrow();
        assertAll(() -> assertThat(floor14.getTotalRooms()).isEqualTo(2L),
                () -> assertThat(floor14.getOccupiedRooms()).isEqualTo(1L),
                () -> assertThat(floor14.getOccupancyRate()).isEqualTo(50.0));

        FloorOccupancyDto floor15 =
                floorOccupancy.stream().filter(f -> f.getFloor().equals(15)).findFirst().orElseThrow();
        assertAll(() -> assertThat(floor15.getTotalRooms()).isEqualTo(3L),
                () -> assertThat(floor15.getOccupiedRooms()).isEqualTo(0L),
                () -> assertThat(floor15.getOccupancyRate()).isEqualTo(0.0));

        FloorOccupancyDto floor16 =
                floorOccupancy.stream().filter(f -> f.getFloor().equals(16)).findFirst().orElseThrow();
        assertAll(() -> assertThat(floor16.getTotalRooms()).isEqualTo(4L),
                () -> assertThat(floor16.getOccupiedRooms()).isEqualTo(4L),
                () -> assertThat(floor16.getOccupancyRate()).isEqualTo(100.0));
    }

    @Test
    @DisplayName("GET /api/v1/room/floor-occupancy -> Excludes inactive rooms from calculation")
    void getFloorOccupancyDistribution_whenInactiveRoomsExist_thenExcludesThem() throws Exception {
        // Given - Floor 17: 2 active rooms + 1 inactive room
        Room activeRoom1 = createAndSaveRoom("1701", 17, standardRoomType);
        Room activeRoom2 = createAndSaveRoom("1702", 17, standardRoomType);
        Room inactiveRoom = createAndSaveRoom("1703", 17, standardRoomType);

        // Deactivate one room
        inactiveRoom.setActive(false);
        roomRepository.save(inactiveRoom);

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("inactive-floor@test.com")
                .build());

        // Occupy 1 active room (50% of active rooms)
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(activeRoom1))
                .numAdults(2)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(
                get(ROOM_BASE_URI + "/floor-occupancy").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<FloorOccupancyDto> floorOccupancy = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then - should only count 2 active rooms
        assertThat(floorOccupancy).hasSize(1);
        FloorOccupancyDto floor17 = floorOccupancy.get(0);
        assertAll(() -> assertThat(floor17.getFloor()).isEqualTo(17),
                () -> assertThat(floor17.getTotalRooms()).isEqualTo(2L),
                () -> assertThat(floor17.getOccupiedRooms()).isEqualTo(1L),
                () -> assertThat(floor17.getOccupancyRate()).isEqualTo(50.0));
    }

    @Test
    @DisplayName("GET /api/v1/room/floor-occupancy -> Excludes cancelled and checked-out bookings from calculation")
    void getFloorOccupancyDistribution_whenCancelledBookingsExist_thenExcludesThem() throws Exception {
        // Given - Floor 18: 3 rooms
        Room room1 = createAndSaveRoom("1801", 18, standardRoomType);
        Room room2 = createAndSaveRoom("1802", 18, standardRoomType);
        Room room3 = createAndSaveRoom("1803", 18, standardRoomType);

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("cancelled-floor@test.com")
                .build());

        // Create a cancelled booking (should not count)
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .guest(guest)
                .status(BookingStatus.CANCELLED)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        // Create a checked-out booking (should not count)
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now().minusDays(2))
                .checkOutDate(LocalDate.now().plusDays(1))
                .guest(guest)
                .status(BookingStatus.CHECKED_OUT)
                .rooms(Set.of(room2))
                .numAdults(1)
                .numChildren(0)
                .build());

        // Create a confirmed booking (should count)
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(room3))
                .numAdults(2)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(
                get(ROOM_BASE_URI + "/floor-occupancy").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<FloorOccupancyDto> floorOccupancy = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then - should count only 1 occupied room
        assertThat(floorOccupancy).hasSize(1);
        FloorOccupancyDto floor18 = floorOccupancy.get(0);
        assertAll(() -> assertThat(floor18.getFloor()).isEqualTo(18),
                () -> assertThat(floor18.getTotalRooms()).isEqualTo(3L),
                () -> assertThat(floor18.getOccupiedRooms()).isEqualTo(1L),
                () -> assertThat(floor18.getOccupancyRate()).isCloseTo(33.33, within(0.01)));
    }

    @Test
    @DisplayName("GET /api/v1/room/floor-occupancy -> Returns floors sorted by floor number")
    void getFloorOccupancyDistribution_whenMultipleFloors_thenReturnsSorted() throws Exception {
        // Given - Create rooms on floors in non-sequential order
        createAndSaveRoom("1901", 19, standardRoomType);
        createAndSaveRoom("2101", 21, standardRoomType);
        createAndSaveRoom("2001", 20, standardRoomType);

        // When
        var mvcResult = mockMvc.perform(
                get(ROOM_BASE_URI + "/floor-occupancy").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<FloorOccupancyDto> floorOccupancy = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then - should be sorted by floor number
        assertThat(floorOccupancy).hasSize(3);
        assertThat(floorOccupancy).extracting(FloorOccupancyDto::getFloor).containsExactly(19, 20, 21);
    }

    // ==================== Helper Methods ====================

    /**
     * Helper method to create a valid RoomRequestDto for testing.
     */
    private RoomRequestDto createValidRoomRequestDto(Long roomTypeId, String roomNumber, Integer floor) {
        return RoomRequestDto.builder()
                .roomNumber(roomNumber)
                .floor(floor)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(Arrays.asList(new PointDto(0.0, 0.0), new PointDto(100.0, 0.0), new PointDto(100.0, 100.0),
                        new PointDto(0.0, 100.0)))
                .roomTypeId(roomTypeId)
                .build();
    }
}
