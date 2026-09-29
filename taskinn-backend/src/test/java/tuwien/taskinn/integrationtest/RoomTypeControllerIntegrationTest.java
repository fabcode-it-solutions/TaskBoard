package tuwien.taskinn.integrationtest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import tuwien.taskinn.dto.request.RoomTypeRequestDto;
import tuwien.taskinn.dto.response.LiteRoomTypeDto;
import tuwien.taskinn.dto.response.RoomDto;
import tuwien.taskinn.dto.response.RoomTypeDto;
import tuwien.taskinn.entity.Point;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.repo.RoomTypeRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("RoomType Controller Integration Tests")
public class RoomTypeControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String ROOM_TYPE_BASE_URI = BASE_URI + "/room-type";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    @DisplayName("GET /api/v1/roomType -> Fails with 403 Forbidden for unauthenticated user")
    void getAllRoomTypes_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get(ROOM_TYPE_BASE_URI)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/roomType -> Returns list of room types for authenticated admin")
    void getAllRoomTypes_whenAuthenticatedAsAdmin_thenReturnsRoomTypeList() throws Exception {
        // Given
        RoomType standardRoom = RoomType.builder()
                .name("Standard Room")
                .description("Standard room with basic amenities")
                .capacity(2)
                .basePrice(new BigDecimal("100.00"))
                .singleBed(2)
                .doubleBed(0)
                .build();

        RoomType deluxeRoom = RoomType.builder()
                .name("Deluxe Suite")
                .description("Luxury suite with premium amenities")
                .capacity(4)
                .basePrice(new BigDecimal("250.00"))
                .singleBed(0)
                .doubleBed(2)
                .build();

        roomTypeRepository.saveAll(List.of(standardRoom, deluxeRoom));
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult =
                mockMvc.perform(get(ROOM_TYPE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<LiteRoomTypeDto> apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then
        assertAll(() -> assertThat(apiResponse).hasSize(2),
                () -> assertThat(apiResponse).extracting(LiteRoomTypeDto::getName)
                        .containsExactlyInAnyOrder("Standard Room", "Deluxe Suite"),
                () -> assertThat(apiResponse).allMatch(dto -> dto.getId() != null),
                () -> assertThat(apiResponse).allMatch(dto -> dto.getName() != null));
    }

    @Test
    @DisplayName("GET /api/v1/roomType -> Returns list of room types for authenticated employee")
    void getAllRoomTypes_whenAuthenticatedAsEmployee_thenReturnsRoomTypeList() throws Exception {
        // Given
        RoomType economyRoom = RoomType.builder()
                .name("Economy Room")
                .description("Budget-friendly room")
                .capacity(1)
                .basePrice(new BigDecimal("75.00"))
                .singleBed(1)
                .doubleBed(0)
                .build();

        roomTypeRepository.save(economyRoom);
        String employeeToken = getEmployeeAuthToken();

        // When
        var mvcResult =
                mockMvc.perform(get(ROOM_TYPE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                        .andExpect(status().isOk())
                        .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<LiteRoomTypeDto> apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then
        assertAll(() -> assertThat(apiResponse).hasSize(1),
                () -> assertThat(apiResponse.get(0).getName()).isEqualTo("Economy Room"),
                () -> assertThat(apiResponse.get(0).getId()).isNotNull());
    }

    @Test
    @DisplayName("GET /api/v1/room-type/{id} -> Returns room type by ID for authenticated admin")
    void getRoomTypeById_whenAuthenticatedAsAdmin_thenReturnsRoomType() throws Exception {
        // Given
        RoomType roomType = RoomType.builder()
                .name("Executive Suite")
                .description("Premium executive suite")
                .capacity(3)
                .basePrice(new BigDecimal("200.00"))
                .singleBed(1)
                .doubleBed(1)
                .build();
        final RoomType savedRoomType = roomTypeRepository.save(roomType);
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc.perform(get(ROOM_TYPE_BASE_URI + "/" + savedRoomType.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        RoomTypeDto apiResponse = objectMapper.readValue(responseBody, RoomTypeDto.class);

        // Then
        assertAll(() -> assertThat(apiResponse.getId()).isEqualTo(savedRoomType.getId()),
                () -> assertThat(apiResponse.getName()).isEqualTo("Executive Suite"),
                () -> assertThat(apiResponse.getDescription()).isEqualTo("Premium executive suite"),
                () -> assertThat(apiResponse.getCapacity()).isEqualTo(3),
                () -> assertThat(apiResponse.getBasePrice()).isEqualByComparingTo(new BigDecimal("200.00")));
    }

    @Test
    @DisplayName("GET /api/v1/room-type/{id} -> Returns 404 when room type not found")
    void getRoomTypeById_whenRoomTypeNotFound_thenNotFound() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When & Then
        mockMvc.perform(get(ROOM_TYPE_BASE_URI + "/99999").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/room-type -> Creates room type for authenticated admin")
    void createRoomType_whenAuthenticatedAsAdmin_thenCreatesRoomType() throws Exception {
        // Given
        RoomTypeRequestDto requestDto = RoomTypeRequestDto.builder()
                .name("Presidential Suite")
                .description("Luxury presidential suite")
                .capacity(5)
                .basePrice(new BigDecimal("500.00"))
                .singleBed(1)
                .doubleBed(2)
                .build();
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc
                .perform(post(ROOM_TYPE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        RoomTypeDto apiResponse = objectMapper.readValue(responseBody, RoomTypeDto.class);

        // Then
        assertAll(() -> assertThat(apiResponse.getId()).isNotNull(),
                () -> assertThat(apiResponse.getName()).isEqualTo("Presidential Suite"),
                () -> assertThat(apiResponse.getDescription()).isEqualTo("Luxury presidential suite"),
                () -> assertThat(apiResponse.getCapacity()).isEqualTo(5),
                () -> assertThat(apiResponse.getBasePrice()).isEqualByComparingTo(new BigDecimal("500.00")),
                () -> assertThat(apiResponse.getSingleBed()).isEqualTo(1),
                () -> assertThat(apiResponse.getDoubleBed()).isEqualTo(2));
    }

    @Test
    @DisplayName("POST /api/v1/room-type -> Returns 403 for non-admin user")
    void createRoomType_whenAuthenticatedAsEmployee_thenForbidden() throws Exception {
        // Given
        RoomTypeRequestDto requestDto =
                RoomTypeRequestDto.builder().name("Test Room").capacity(2).basePrice(new BigDecimal("100.00")).build();
        String employeeToken = getEmployeeAuthToken();

        // When & Then
        mockMvc.perform(post(ROOM_TYPE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/room-type -> Returns 400 for invalid data")
    void createRoomType_withInvalidData_thenBadRequest() throws Exception {
        // Given - missing required fields
        RoomTypeRequestDto requestDto = RoomTypeRequestDto.builder().description("No name provided").build();
        String adminToken = getAdminAuthToken();

        // When & Then
        mockMvc.perform(post(ROOM_TYPE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/room-type/{id} -> Updates room type for authenticated admin")
    void updateRoomType_whenAuthenticatedAsAdmin_thenUpdatesRoomType() throws Exception {
        // Given
        RoomType roomType = RoomType.builder()
                .name("Original Name")
                .description("Original description")
                .capacity(2)
                .basePrice(new BigDecimal("100.00"))
                .singleBed(2)
                .doubleBed(0)
                .build();
        final RoomType savedRoomType = roomTypeRepository.save(roomType);

        RoomTypeRequestDto updateDto = RoomTypeRequestDto.builder()
                .name("Updated Name")
                .description("Updated description")
                .capacity(3)
                .basePrice(new BigDecimal("150.00"))
                .singleBed(1)
                .doubleBed(1)
                .build();
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc.perform(put(ROOM_TYPE_BASE_URI + "/" + savedRoomType.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        RoomTypeDto apiResponse = objectMapper.readValue(responseBody, RoomTypeDto.class);

        // Then
        assertAll(() -> assertThat(apiResponse.getId()).isEqualTo(savedRoomType.getId()),
                () -> assertThat(apiResponse.getName()).isEqualTo("Updated Name"),
                () -> assertThat(apiResponse.getDescription()).isEqualTo("Updated description"),
                () -> assertThat(apiResponse.getCapacity()).isEqualTo(3),
                () -> assertThat(apiResponse.getBasePrice()).isEqualByComparingTo(new BigDecimal("150.00")));
    }

    @Test
    @DisplayName("GET /api/v1/room-type/{id}/rooms -> Returns rooms for room type")
    void getRoomsByRoomTypeId_whenRoomsExist_thenReturnsRooms() throws Exception {
        // Given
        RoomType roomType =
                RoomType.builder().name("Standard Room").capacity(2).basePrice(new BigDecimal("100.00")).build();
        final RoomType savedRoomType = roomTypeRepository.save(roomType);

        Room room1 = Room.builder()
                .roomNumber("201")
                .floor(2)
                .roomType(savedRoomType)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(
                        List.of(new Point(0.0, 0.0), new Point(10.0, 0.0), new Point(10.0, 10.0), new Point(0.0, 10.0)))
                .build();
        Room room2 = Room.builder()
                .roomNumber("202")
                .floor(2)
                .roomType(savedRoomType)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(List.of(new Point(10.0, 0.0), new Point(20.0, 0.0), new Point(20.0, 10.0),
                        new Point(10.0, 10.0)))
                .build();
        roomRepository.saveAll(List.of(room1, room2));

        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc
                .perform(get(ROOM_TYPE_BASE_URI + "/" + savedRoomType.getId() + "/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<RoomDto> apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then
        assertAll(() -> assertThat(apiResponse).hasSize(2),
                () -> assertThat(apiResponse).extracting(RoomDto::getNumber).containsExactlyInAnyOrder("201", "202"));
    }

    @Test
    @DisplayName("DELETE /api/v1/room-type/{id} -> Deletes room type when no rooms use it")
    void deleteRoomType_whenNoRoomsUseIt_thenDeletesSuccessfully() throws Exception {
        // Given
        RoomType roomType =
                RoomType.builder().name("Unused Room Type").capacity(2).basePrice(new BigDecimal("100.00")).build();
        final RoomType savedRoomType = roomTypeRepository.save(roomType);
        String adminToken = getAdminAuthToken();
        final Long roomTypeId = savedRoomType.getId();

        // When
        mockMvc.perform(
                delete(ROOM_TYPE_BASE_URI + "/" + roomTypeId).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Then
        assertThat(roomTypeRepository.findById(roomTypeId)).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/room-type/{id} -> Returns 403 for non-admin user")
    void deleteRoomType_whenAuthenticatedAsEmployee_thenForbidden() throws Exception {
        // Given
        RoomType roomType =
                RoomType.builder().name("Test Room Type").capacity(2).basePrice(new BigDecimal("100.00")).build();
        final RoomType savedRoomType = roomTypeRepository.save(roomType);
        String employeeToken = getEmployeeAuthToken();

        // When & Then
        mockMvc.perform(delete(ROOM_TYPE_BASE_URI + "/" + savedRoomType.getId()).header(HttpHeaders.AUTHORIZATION,
                "Bearer " + employeeToken)).andExpect(status().isForbidden());
    }
}
