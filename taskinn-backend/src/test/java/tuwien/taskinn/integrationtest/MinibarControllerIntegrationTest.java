package tuwien.taskinn.integrationtest;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.request.InventoryItemRequestDto;
import tuwien.taskinn.dto.request.MinibarBatchConsumptionRequestDto;
import tuwien.taskinn.dto.request.MinibarConsumptionRequestDto;
import tuwien.taskinn.dto.request.MinibarRestockRequestDto;
import tuwien.taskinn.dto.response.InventoryItemDto;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.Guest;
import tuwien.taskinn.entity.InventoryItem;
import tuwien.taskinn.entity.Point;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.repo.InventoryItemRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Minibar Controller Integration Tests")
public class MinibarControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String MINIBAR_BASE_URI = BASE_URI + "/minibar";
    private static final String ITEMS_URI = MINIBAR_BASE_URI + "/items";

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    // ================== GET /items tests ==================

    @Test
    @DisplayName("GET /api/v1/minibar/items -> Returns 403 Forbidden for unauthenticated user")
    void getAllItems_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get(ITEMS_URI)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/minibar/items -> Returns list of items for ADMIN and EMPLOYEE")
    void getAllItems_whenAuthenticatedAsEmployee_thenReturnsItemList() throws Exception {
        // Given
        InventoryItem employeeItem = InventoryItem.builder()
                .name("Employee Item")
                .category("Test")
                .unitOfMeasurement("unit")
                .unitPrice(new BigDecimal("10.00"))
                .currentStock(50)
                .reorderStock(20)
                .deleted(false)
                .build();
        inventoryItemRepository.save(employeeItem);
        String employeeToken = getEmployeeAuthToken();

        // When
        var mvcResult = mockMvc.perform(get(ITEMS_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<InventoryItemDto> apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then
        assertAll(() -> assertThat(apiResponse).isNotEmpty(),
                () -> assertThat(apiResponse).allMatch(dto -> dto.getId() != null),
                () -> assertThat(apiResponse).extracting(InventoryItemDto::getName).contains("Employee Item"));
    }

    @Test
    @DisplayName("GET /api/v1/minibar/items -> Returns list of items for ADMIN")
    void getAllItems_whenAuthenticatedAsAdmin_thenReturnsItemList() throws Exception {
        // Given
        InventoryItem item1 = InventoryItem.builder()
                .name("Coca Cola")
                .category("Beverages")
                .unitOfMeasurement("bottle")
                .unitPrice(new BigDecimal("3.50"))
                .currentStock(100)
                .reorderStock(20)
                .deleted(false)
                .build();

        InventoryItem item2 = InventoryItem.builder()
                .name("Pringles")
                .category("Snacks")
                .unitOfMeasurement("can")
                .unitPrice(new BigDecimal("4.00"))
                .currentStock(50)
                .reorderStock(10)
                .deleted(false)
                .build();

        inventoryItemRepository.saveAll(List.of(item1, item2));
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc.perform(get(ITEMS_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<InventoryItemDto> apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});

        // Then
        assertAll(() -> assertThat(apiResponse).hasSize(2),
                () -> assertThat(apiResponse).extracting(InventoryItemDto::getName)
                        .containsExactlyInAnyOrder("Coca Cola", "Pringles"),
                () -> assertThat(apiResponse).allMatch(dto -> dto.getId() != null));
    }

    // ================== GET /items/{id} tests ==================

    @Test
    @DisplayName("GET /api/v1/minibar/items/{id} -> Returns item for ADMIN")
    void getItemById_whenAuthenticatedAsAdmin_thenReturnsItem() throws Exception {
        // Given
        InventoryItem item = InventoryItem.builder()
                .name("Sprite")
                .category("Beverages")
                .unitOfMeasurement("can")
                .unitPrice(new BigDecimal("2.50"))
                .currentStock(75)
                .reorderStock(15)
                .deleted(false)
                .build();
        item = inventoryItemRepository.save(item);
        Long itemId = item.getId();
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult =
                mockMvc.perform(get(ITEMS_URI + "/" + itemId).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        InventoryItemDto apiResponse = objectMapper.readValue(responseBody, InventoryItemDto.class);

        // Then
        assertAll(() -> assertThat(apiResponse.getId()).isEqualTo(itemId),
                () -> assertThat(apiResponse.getName()).isEqualTo("Sprite"),
                () -> assertThat(apiResponse.getCategory()).isEqualTo("Beverages"),
                () -> assertThat(apiResponse.getCurrentStock()).isEqualTo(75));
    }

    @Test
    @DisplayName("GET /api/v1/minibar/items/{id} -> Returns 404 when item not found")
    void getItemById_whenItemNotFound_thenNotFound() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When/Then
        mockMvc.perform(get(ITEMS_URI + "/99999").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ================== POST /items tests ==================

    @Test
    @DisplayName("POST /api/v1/minibar/items -> Creates item for ADMIN")
    void createItem_whenAuthenticatedAsAdmin_thenCreatesItem() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();
        InventoryItemRequestDto requestDto = InventoryItemRequestDto.builder()
                .name("Red Bull")
                .category("Energy Drinks")
                .unitOfMeasurement("can")
                .unitPrice(new BigDecimal("5.00"))
                .currentStock(200)
                .reorderStock(50)
                .build();

        // When
        var mvcResult = mockMvc
                .perform(post(ITEMS_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        InventoryItemDto apiResponse = objectMapper.readValue(responseBody, InventoryItemDto.class);

        // Then
        assertAll(() -> assertThat(apiResponse.getId()).isNotNull(),
                () -> assertThat(apiResponse.getName()).isEqualTo("Red Bull"),
                () -> assertThat(apiResponse.getCategory()).isEqualTo("Energy Drinks"),
                () -> assertThat(apiResponse.getCurrentStock()).isEqualTo(200));

        // Verify persisted
        assertThat(inventoryItemRepository.findById(apiResponse.getId())).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/minibar/items -> Returns 400 for invalid data")
    void createItem_whenInvalidData_thenBadRequest() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();
        InventoryItemRequestDto requestDto = InventoryItemRequestDto.builder()
                .name("") // Invalid: blank name
                .unitPrice(new BigDecimal("-1.00")) // Invalid: negative price
                .currentStock(-5) // Invalid: negative stock
                .build();

        // When/Then
        mockMvc.perform(post(ITEMS_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/minibar/items -> Returns 400 for duplicate name")
    void createItem_whenDuplicateName_thenBadRequest() throws Exception {
        // Given
        InventoryItem existingItem = InventoryItem.builder()
                .name("Duplicate Item")
                .category("Test")
                .unitOfMeasurement("unit")
                .unitPrice(new BigDecimal("5.00"))
                .currentStock(10)
                .reorderStock(5)
                .deleted(false)
                .build();
        inventoryItemRepository.save(existingItem);

        String adminToken = getAdminAuthToken();
        InventoryItemRequestDto requestDto = InventoryItemRequestDto.builder()
                .name("Duplicate Item") // Same name as existing item
                .category("Test")
                .unitOfMeasurement("unit")
                .unitPrice(new BigDecimal("6.00"))
                .currentStock(20)
                .reorderStock(10)
                .build();

        // When/Then
        mockMvc.perform(post(ITEMS_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    // ================== PUT /items/{id} tests ==================

    @Test
    @DisplayName("PUT /api/v1/minibar/items/{id} -> Updates item for ADMIN")
    void updateItem_whenAuthenticatedAsAdmin_thenUpdatesItem() throws Exception {
        // Given
        InventoryItem existingItem = InventoryItem.builder()
                .name("Old Name")
                .category("Old Category")
                .unitOfMeasurement("unit")
                .unitPrice(new BigDecimal("1.00"))
                .currentStock(10)
                .reorderStock(5)
                .deleted(false)
                .build();
        existingItem = inventoryItemRepository.save(existingItem);

        String adminToken = getAdminAuthToken();
        InventoryItemRequestDto updateDto = InventoryItemRequestDto.builder()
                .name("Updated Name")
                .category("Updated Category")
                .unitOfMeasurement("new unit")
                .unitPrice(new BigDecimal("10.00"))
                .currentStock(100)
                .reorderStock(25)
                .build();

        // When
        var mvcResult = mockMvc.perform(
                put(ITEMS_URI + "/" + existingItem.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        InventoryItemDto apiResponse = objectMapper.readValue(responseBody, InventoryItemDto.class);

        // Then
        assertAll(() -> assertThat(apiResponse.getName()).isEqualTo("Updated Name"),
                () -> assertThat(apiResponse.getCategory()).isEqualTo("Updated Category"),
                () -> assertThat(apiResponse.getCurrentStock()).isEqualTo(100));
    }

    @Test
    @DisplayName("PUT /api/v1/minibar/items/{id} -> Returns 400 when updating to duplicate name")
    void updateItem_whenDuplicateName_thenBadRequest() throws Exception {
        // Given
        InventoryItem item1 = InventoryItem.builder()
                .name("Item One")
                .category("Test")
                .unitOfMeasurement("unit")
                .unitPrice(new BigDecimal("5.00"))
                .currentStock(10)
                .reorderStock(5)
                .deleted(false)
                .build();
        inventoryItemRepository.save(item1);

        InventoryItem item2 = InventoryItem.builder()
                .name("Item Two")
                .category("Test")
                .unitOfMeasurement("unit")
                .unitPrice(new BigDecimal("6.00"))
                .currentStock(20)
                .reorderStock(10)
                .deleted(false)
                .build();
        item2 = inventoryItemRepository.save(item2);

        String adminToken = getAdminAuthToken();
        InventoryItemRequestDto updateDto = InventoryItemRequestDto.builder()
                .name("Item One") // Trying to rename to existing item's name
                .category("Test")
                .unitOfMeasurement("unit")
                .unitPrice(new BigDecimal("7.00"))
                .currentStock(30)
                .reorderStock(15)
                .build();

        // When/Then
        mockMvc.perform(put(ITEMS_URI + "/" + item2.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isBadRequest());
    }

    // ================== DELETE /items/{id} tests ==================

    @Test
    @DisplayName("DELETE /api/v1/minibar/items/{id} -> Deletes item for ADMIN")
    void deleteItem_whenAuthenticatedAsAdmin_thenDeletesItem() throws Exception {
        // Given
        InventoryItem item = InventoryItem.builder()
                .name("To Be Deleted")
                .category("Test")
                .unitOfMeasurement("unit")
                .unitPrice(new BigDecimal("1.00"))
                .currentStock(1)
                .reorderStock(1)
                .deleted(false)
                .build();
        item = inventoryItemRepository.save(item);
        Long itemId = item.getId();

        String adminToken = getAdminAuthToken();

        // When
        mockMvc.perform(delete(ITEMS_URI + "/" + itemId).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Then - Verify item is soft deleted (still exists in DB but marked as deleted)
        InventoryItem deletedItem = inventoryItemRepository.findById(itemId).orElseThrow();
        assertAll(() -> assertThat(deletedItem.getDeleted()).isTrue(),
                () -> assertThat(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).isEmpty());
    }

    // ================== POST /rooms/{roomId}/consume tests ==================

    @Test
    @DisplayName("POST /api/v1/minibar/rooms/{roomId}/consume -> Returns 403 for unauthenticated user")
    void recordConsumption_whenUnauthenticated_thenForbidden() throws Exception {
        // Given
        MinibarConsumptionRequestDto requestDto = MinibarConsumptionRequestDto.builder().itemId(1L).quantity(1).build();

        // When/Then
        mockMvc.perform(post(MINIBAR_BASE_URI + "/rooms/1/consume").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/minibar/rooms/{roomId}/consume -> Works for EMPLOYEE and ADMIN with active booking")
    void recordConsumption_whenAuthenticatedAsEmployee_thenRecordsConsumption() throws Exception {
        // Given - Create room with active booking
        RoomType roomType = RoomType.builder()
                .name("Standard")
                .description("Standard room")
                .capacity(2)
                .basePrice(new BigDecimal("100.00"))
                .singleBed(1)
                .doubleBed(0)
                .build();
        roomTypeRepository.save(roomType);

        Room room = Room.builder()
                .roomNumber("101")
                .floor(1)
                .status(RoomStatus.OCCUPIED)
                .active(true)
                .roomType(roomType)
                .svgPoints(List.of(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .build();
        room = roomRepository.save(room);

        Guest guest = Guest.builder()
                .firstName("John")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john.guest@example.com")
                .phone("+1234567890")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        guestRepository.save(guest);

        Booking booking = Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(3))
                .numAdults(1)
                .numChildren(0)
                .status(BookingStatus.CHECKED_IN)
                .createdManually(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .guest(guest)
                .rooms(Set.of(room))
                .build();
        bookingRepository.save(booking);

        InventoryItem item = InventoryItem.builder()
                .name("Minibar Coke")
                .category("Beverages")
                .unitOfMeasurement("bottle")
                .unitPrice(new BigDecimal("5.00"))
                .currentStock(50)
                .reorderStock(10)
                .deleted(false)
                .build();
        item = inventoryItemRepository.save(item);

        String employeeToken = getEmployeeAuthToken();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(item.getId()).quantity(2).build();

        // When
        mockMvc.perform(post(MINIBAR_BASE_URI + "/rooms/" + room.getId() + "/consume")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk());

        // Then - Verify stock decreased
        InventoryItem updatedItem = inventoryItemRepository.findById(item.getId()).orElseThrow();
        assertThat(updatedItem.getCurrentStock()).isEqualTo(48);
    }

    @Test
    @DisplayName("POST /api/v1/minibar/rooms/{roomId}/consume -> Works for ADMIN with active booking")
    void recordConsumption_whenAuthenticatedAsAdmin_thenRecordsConsumption() throws Exception {
        // Given - Create room with active booking
        RoomType roomType = RoomType.builder()
                .name("Premium")
                .description("Premium room")
                .capacity(2)
                .basePrice(new BigDecimal("200.00"))
                .singleBed(0)
                .doubleBed(1)
                .build();
        roomTypeRepository.save(roomType);

        Room room = Room.builder()
                .roomNumber("301")
                .floor(3)
                .status(RoomStatus.OCCUPIED)
                .active(true)
                .roomType(roomType)
                .svgPoints(List.of(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .build();
        room = roomRepository.save(room);

        Guest guest = Guest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1985, 5, 5))
                .email("jane.doe@example.com")
                .phone("+9876543210")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        guestRepository.save(guest);

        Booking booking = Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(5))
                .numAdults(2)
                .numChildren(0)
                .status(BookingStatus.CHECKED_IN)
                .createdManually(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .guest(guest)
                .rooms(Set.of(room))
                .build();
        bookingRepository.save(booking);

        InventoryItem item = InventoryItem.builder()
                .name("Admin Test Cola")
                .category("Beverages")
                .unitOfMeasurement("bottle")
                .unitPrice(new BigDecimal("5.00"))
                .currentStock(100)
                .reorderStock(10)
                .deleted(false)
                .build();
        item = inventoryItemRepository.save(item);
        Long itemId = item.getId();

        String adminToken = getAdminAuthToken();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(3).build();

        // When
        mockMvc.perform(post(MINIBAR_BASE_URI + "/rooms/" + room.getId() + "/consume")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk());

        // Then - Verify stock decreased
        InventoryItem updatedItem = inventoryItemRepository.findById(itemId).orElseThrow();
        assertThat(updatedItem.getCurrentStock()).isEqualTo(97);
    }

    // ================== POST /rooms/{roomId}/restock tests ==================

    // ================== POST /rooms/{roomId}/consume with allowRecentBooking tests ==================

    @Test
    @DisplayName("POST /api/v1/minibar/rooms/{roomId}/consume?allowRecentBooking=true -> Works with recently checked-out booking")
    void recordConsumption_withAllowRecentBooking_success() throws Exception {
        // Given - Create room with recently checked-out booking
        RoomType roomType = RoomType.builder()
                .name("Suite")
                .description("Suite room")
                .capacity(4)
                .basePrice(new BigDecimal("300.00"))
                .singleBed(0)
                .doubleBed(2)
                .build();
        roomTypeRepository.save(roomType);

        Room room = Room.builder()
                .roomNumber("401")
                .floor(4)
                .status(RoomStatus.DIRTY) // Room is dirty after checkout
                .active(true)
                .roomType(roomType)
                .svgPoints(List.of(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .build();
        room = roomRepository.save(room);

        Guest guest = Guest.builder()
                .firstName("Bob")
                .lastName("CheckedOut")
                .dateOfBirth(LocalDate.of(1980, 3, 15))
                .email("bob.checkedout@example.com")
                .phone("+1112223333")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        guestRepository.save(guest);

        // Booking that was checked out today
        Booking booking = Booking.builder()
                .checkInDate(LocalDate.now().minusDays(2))
                .checkOutDate(LocalDate.now())
                .numAdults(2)
                .numChildren(0)
                .status(BookingStatus.CHECKED_OUT)
                .createdManually(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .guest(guest)
                .rooms(Set.of(room))
                .build();
        bookingRepository.save(booking);

        InventoryItem item = InventoryItem.builder()
                .name("Cleaning Minibar Item")
                .category("Beverages")
                .unitOfMeasurement("bottle")
                .unitPrice(new BigDecimal("6.00"))
                .currentStock(30)
                .reorderStock(10)
                .deleted(false)
                .build();
        item = inventoryItemRepository.save(item);

        String employeeToken = getEmployeeAuthToken();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(item.getId()).quantity(2).build();

        // When
        mockMvc.perform(post(MINIBAR_BASE_URI + "/rooms/" + room.getId() + "/consume?allowRecentBooking=true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk());

        // Then - Verify stock decreased
        InventoryItem updatedItem = inventoryItemRepository.findById(item.getId()).orElseThrow();
        assertThat(updatedItem.getCurrentStock()).isEqualTo(28);
    }

    @Test
    @DisplayName("POST /api/v1/minibar/rooms/{roomId}/consume?allowRecentBooking=true -> Returns 400 when no recent booking")
    void recordConsumption_withAllowRecentBooking_noRecentBooking_badRequest() throws Exception {
        // Given - Create room with no booking
        RoomType roomType = RoomType.builder()
                .name("Empty Suite")
                .description("Empty suite room")
                .capacity(4)
                .basePrice(new BigDecimal("350.00"))
                .singleBed(0)
                .doubleBed(2)
                .build();
        roomTypeRepository.save(roomType);

        Room room = Room.builder()
                .roomNumber("501")
                .floor(5)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .roomType(roomType)
                .svgPoints(List.of(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .build();
        room = roomRepository.save(room);

        InventoryItem item = InventoryItem.builder()
                .name("No Booking Item")
                .category("Beverages")
                .unitOfMeasurement("bottle")
                .unitPrice(new BigDecimal("4.00"))
                .currentStock(25)
                .reorderStock(5)
                .deleted(false)
                .build();
        item = inventoryItemRepository.save(item);

        String employeeToken = getEmployeeAuthToken();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(item.getId()).quantity(1).build();

        // When/Then
        mockMvc.perform(post(MINIBAR_BASE_URI + "/rooms/" + room.getId() + "/consume?allowRecentBooking=true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    // ================== POST /rooms/{roomId}/restock tests ==================

    @Test
    @DisplayName("POST /api/v1/minibar/rooms/{roomId}/restock -> Works for EMPLOYEE and ADMIN")
    void restockItem_whenAuthenticatedAsEmployee_thenRestocksItem() throws Exception {
        // Given
        RoomType roomType = RoomType.builder()
                .name("Deluxe")
                .description("Deluxe room")
                .capacity(2)
                .basePrice(new BigDecimal("150.00"))
                .singleBed(0)
                .doubleBed(1)
                .build();
        roomTypeRepository.save(roomType);

        Room room = Room.builder()
                .roomNumber("201")
                .floor(2)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .roomType(roomType)
                .svgPoints(List.of(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .build();
        room = roomRepository.save(room);

        InventoryItem item = InventoryItem.builder()
                .name("Minibar Sprite")
                .category("Beverages")
                .unitOfMeasurement("can")
                .unitPrice(new BigDecimal("4.00"))
                .currentStock(20)
                .reorderStock(5)
                .deleted(false)
                .build();
        item = inventoryItemRepository.save(item);
        Long itemId = item.getId();

        String employeeToken = getEmployeeAuthToken();
        MinibarRestockRequestDto requestDto = MinibarRestockRequestDto.builder().itemId(itemId).quantity(30).build();

        // When
        var mvcResult = mockMvc.perform(post(MINIBAR_BASE_URI + "/rooms/" + room.getId() + "/restock")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        InventoryItemDto apiResponse = objectMapper.readValue(responseBody, InventoryItemDto.class);

        // Then
        assertAll(() -> assertThat(apiResponse.getCurrentStock()).isEqualTo(50), () -> {
            InventoryItem updatedItem = inventoryItemRepository.findById(itemId).orElseThrow();
            assertThat(updatedItem.getCurrentStock()).isEqualTo(50);
        });
    }

    // ================== POST /rooms/{roomId}/consume-batch tests ==================

    @Test
    @DisplayName("POST /api/v1/minibar/rooms/{roomId}/consume-batch -> Records multiple items atomically")
    void recordBatchConsumption_withMultipleItems_recordsAtomically() throws Exception {
        // Given - Create room with active booking
        RoomType roomType = RoomType.builder()
                .name("Batch Test Room Type")
                .description("Room for batch testing")
                .capacity(2)
                .basePrice(new BigDecimal("120.00"))
                .singleBed(1)
                .doubleBed(0)
                .build();
        roomTypeRepository.save(roomType);

        Room room = Room.builder()
                .roomNumber("601")
                .floor(6)
                .status(RoomStatus.OCCUPIED)
                .active(true)
                .roomType(roomType)
                .svgPoints(List.of(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .build();
        room = roomRepository.save(room);

        Guest guest = Guest.builder()
                .firstName("Batch")
                .lastName("Tester")
                .dateOfBirth(LocalDate.of(1992, 7, 20))
                .email("batch.tester@example.com")
                .phone("+5556667777")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        guestRepository.save(guest);

        Booking booking = Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2))
                .numAdults(1)
                .numChildren(0)
                .status(BookingStatus.CHECKED_IN)
                .createdManually(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .guest(guest)
                .rooms(Set.of(room))
                .build();
        bookingRepository.save(booking);

        InventoryItem item1 = InventoryItem.builder()
                .name("Batch Item 1")
                .category("Beverages")
                .unitOfMeasurement("bottle")
                .unitPrice(new BigDecimal("5.00"))
                .currentStock(100)
                .reorderStock(10)
                .deleted(false)
                .build();
        item1 = inventoryItemRepository.save(item1);

        InventoryItem item2 = InventoryItem.builder()
                .name("Batch Item 2")
                .category("Snacks")
                .unitOfMeasurement("bag")
                .unitPrice(new BigDecimal("3.00"))
                .currentStock(50)
                .reorderStock(10)
                .deleted(false)
                .build();
        item2 = inventoryItemRepository.save(item2);

        String employeeToken = getEmployeeAuthToken();
        MinibarBatchConsumptionRequestDto requestDto = MinibarBatchConsumptionRequestDto.builder()
                .items(List.of(MinibarConsumptionRequestDto.builder().itemId(item1.getId()).quantity(2).build(),
                        MinibarConsumptionRequestDto.builder().itemId(item2.getId()).quantity(3).build()))
                .build();

        // When
        mockMvc.perform(post(MINIBAR_BASE_URI + "/rooms/" + room.getId() + "/consume-batch?allowRecentBooking=true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk());

        // Then - Verify both stocks decreased
        InventoryItem updatedItem1 = inventoryItemRepository.findById(item1.getId()).orElseThrow();
        InventoryItem updatedItem2 = inventoryItemRepository.findById(item2.getId()).orElseThrow();
        assertAll(() -> assertThat(updatedItem1.getCurrentStock()).isEqualTo(98), // 100 - 2
                () -> assertThat(updatedItem2.getCurrentStock()).isEqualTo(47)); // 50 - 3
    }

    @Test
    @DisplayName("POST /api/v1/minibar/rooms/{roomId}/consume-batch -> Rolls back all changes when one item has insufficient stock")
    void recordBatchConsumption_withInsufficientStock_rollsBack() throws Exception {
        // Given - Create room with active booking
        RoomType roomType = RoomType.builder()
                .name("Rollback Test Room Type")
                .description("Room for rollback testing")
                .capacity(2)
                .basePrice(new BigDecimal("130.00"))
                .singleBed(1)
                .doubleBed(0)
                .build();
        roomTypeRepository.save(roomType);

        Room room = Room.builder()
                .roomNumber("602")
                .floor(6)
                .status(RoomStatus.OCCUPIED)
                .active(true)
                .roomType(roomType)
                .svgPoints(List.of(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .build();
        room = roomRepository.save(room);

        Guest guest = Guest.builder()
                .firstName("Rollback")
                .lastName("Tester")
                .dateOfBirth(LocalDate.of(1988, 11, 30))
                .email("rollback.tester@example.com")
                .phone("+8889990000")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        guestRepository.save(guest);

        Booking booking = Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2))
                .numAdults(1)
                .numChildren(0)
                .status(BookingStatus.CHECKED_IN)
                .createdManually(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .guest(guest)
                .rooms(Set.of(room))
                .build();
        bookingRepository.save(booking);

        InventoryItem item1 = InventoryItem.builder()
                .name("Rollback Item 1")
                .category("Beverages")
                .unitOfMeasurement("bottle")
                .unitPrice(new BigDecimal("5.00"))
                .currentStock(100)
                .reorderStock(10)
                .deleted(false)
                .build();
        item1 = inventoryItemRepository.save(item1);

        InventoryItem item2 = InventoryItem.builder()
                .name("Rollback Item 2")
                .category("Snacks")
                .unitOfMeasurement("bag")
                .unitPrice(new BigDecimal("3.00"))
                .currentStock(2) // Only 2 in stock
                .reorderStock(10)
                .deleted(false)
                .build();
        item2 = inventoryItemRepository.save(item2);
        Long item1Id = item1.getId();
        Long item2Id = item2.getId();

        String employeeToken = getEmployeeAuthToken();
        MinibarBatchConsumptionRequestDto requestDto = MinibarBatchConsumptionRequestDto.builder()
                .items(List.of(MinibarConsumptionRequestDto.builder().itemId(item1Id).quantity(2).build(),
                        MinibarConsumptionRequestDto.builder().itemId(item2Id).quantity(10).build())) // Requesting 10
                                                                                                      // but only 2
                                                                                                      // available
                .build();

        // When - Should fail due to insufficient stock
        mockMvc.perform(post(MINIBAR_BASE_URI + "/rooms/" + room.getId() + "/consume-batch?allowRecentBooking=true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());

        // Then - Verify no stocks changed (transaction rolled back)
        InventoryItem unchangedItem1 = inventoryItemRepository.findById(item1Id).orElseThrow();
        InventoryItem unchangedItem2 = inventoryItemRepository.findById(item2Id).orElseThrow();
        assertAll(() -> assertThat(unchangedItem1.getCurrentStock()).isEqualTo(100), // Unchanged
                () -> assertThat(unchangedItem2.getCurrentStock()).isEqualTo(2)); // Unchanged
    }

    @Test
    @DisplayName("POST /api/v1/minibar/rooms/{roomId}/consume-batch -> Returns 400 for empty items list")
    void recordBatchConsumption_withEmptyItems_badRequest() throws Exception {
        // Given
        String employeeToken = getEmployeeAuthToken();
        MinibarBatchConsumptionRequestDto requestDto = MinibarBatchConsumptionRequestDto.builder()
                .items(List.of()) // Empty list
                .build();

        // When/Then
        mockMvc.perform(post(MINIBAR_BASE_URI + "/rooms/1/consume-batch")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/minibar/rooms/{roomId}/consume-batch -> Returns 403 for unauthenticated user")
    void recordBatchConsumption_whenUnauthenticated_thenForbidden() throws Exception {
        // Given
        MinibarBatchConsumptionRequestDto requestDto = MinibarBatchConsumptionRequestDto.builder()
                .items(List.of(MinibarConsumptionRequestDto.builder().itemId(1L).quantity(1).build()))
                .build();

        // When/Then
        mockMvc.perform(post(MINIBAR_BASE_URI + "/rooms/1/consume-batch").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/minibar/rooms/{roomId}/consume-batch -> Aggregates duplicate item IDs")
    void recordBatchConsumption_withDuplicateItemIds_aggregatesQuantities() throws Exception {
        // Given - Create room with active booking
        RoomType roomType = RoomType.builder()
                .name("Aggregate Test Room Type")
                .description("Room for aggregate testing")
                .capacity(2)
                .basePrice(new BigDecimal("140.00"))
                .singleBed(1)
                .doubleBed(0)
                .build();
        roomTypeRepository.save(roomType);

        Room room = Room.builder()
                .roomNumber("603")
                .floor(6)
                .status(RoomStatus.OCCUPIED)
                .active(true)
                .roomType(roomType)
                .svgPoints(List.of(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .build();
        room = roomRepository.save(room);

        Guest guest = Guest.builder()
                .firstName("Aggregate")
                .lastName("Tester")
                .dateOfBirth(LocalDate.of(1995, 4, 10))
                .email("aggregate.tester@example.com")
                .phone("+3334445555")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        guestRepository.save(guest);

        Booking booking = Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2))
                .numAdults(1)
                .numChildren(0)
                .status(BookingStatus.CHECKED_IN)
                .createdManually(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .guest(guest)
                .rooms(Set.of(room))
                .build();
        bookingRepository.save(booking);

        InventoryItem item = InventoryItem.builder()
                .name("Aggregate Test Item")
                .category("Beverages")
                .unitOfMeasurement("bottle")
                .unitPrice(new BigDecimal("5.00"))
                .currentStock(100)
                .reorderStock(10)
                .deleted(false)
                .build();
        item = inventoryItemRepository.save(item);
        Long itemId = item.getId();

        String employeeToken = getEmployeeAuthToken();
        // Same item ID multiple times
        MinibarBatchConsumptionRequestDto requestDto = MinibarBatchConsumptionRequestDto.builder()
                .items(List.of(MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(2).build(),
                        MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(3).build(),
                        MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(5).build())) // Total: 10
                .build();

        // When
        mockMvc.perform(post(MINIBAR_BASE_URI + "/rooms/" + room.getId() + "/consume-batch?allowRecentBooking=true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk());

        // Then - Verify total quantity was deducted
        InventoryItem updatedItem = inventoryItemRepository.findById(itemId).orElseThrow();
        assertThat(updatedItem.getCurrentStock()).isEqualTo(90); // 100 - 10
    }
}
