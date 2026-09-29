package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tuwien.taskinn.dto.request.InventoryItemRequestDto;
import tuwien.taskinn.dto.request.MinibarConsumptionRequestDto;
import tuwien.taskinn.dto.request.MinibarRestockRequestDto;
import tuwien.taskinn.dto.response.InventoryItemDto;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.InventoryItem;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.mapper.InventoryItemMapper;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.InventoryItemRepository;
import tuwien.taskinn.repo.MinibarConsumptionRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.services.impl.MinibarServiceImpl;
import tuwien.taskinn.services.validator.InventoryItemValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("MinibarServiceImpl Unit Tests")
public class MinibarServiceImplTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private MinibarConsumptionRepository minibarConsumptionRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private InventoryItemMapper inventoryItemMapper;

    @Mock
    private InventoryItemValidator inventoryItemValidator;

    @InjectMocks
    private MinibarServiceImpl minibarService;

    @Test
    @DisplayName("getAllItems should return list of non-deleted mapped items")
    void getAllItems_whenItemsExist_returnsMappedActiveItems() {
        // Given
        InventoryItem item1 = InventoryItem.builder().id(1L).name("Coca Cola").deleted(false).build();
        InventoryItem item2 = InventoryItem.builder().id(2L).name("Sprite").deleted(false).build();
        InventoryItemDto dto1 = InventoryItemDto.builder().id(1L).name("Coca Cola").build();
        InventoryItemDto dto2 = InventoryItemDto.builder().id(2L).name("Sprite").build();

        when(inventoryItemRepository.findByDeletedFalse()).thenReturn(List.of(item1, item2));
        when(inventoryItemMapper.toDto(item1)).thenReturn(dto1);
        when(inventoryItemMapper.toDto(item2)).thenReturn(dto2);

        // When
        List<InventoryItemDto> result = minibarService.getAllActiveItems();

        // Then
        assertAll("getAllItems returns non-deleted mapped items", () -> assertThat(result).hasSize(2),
                () -> assertThat(result).extracting(InventoryItemDto::getName).containsExactly("Coca Cola", "Sprite"),
                () -> verify(inventoryItemRepository, times(1)).findByDeletedFalse());
    }

    @Test
    @DisplayName("getItemById should return item when found and not deleted")
    void getItemById_whenItemExists_returnsItem() {
        // Given
        Long itemId = 1L;
        InventoryItem item = InventoryItem.builder().id(itemId).name("Coca Cola").deleted(false).build();
        InventoryItemDto dto = InventoryItemDto.builder().id(itemId).name("Coca Cola").build();

        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(item));
        when(inventoryItemMapper.toDto(item)).thenReturn(dto);

        // When
        InventoryItemDto result = minibarService.getItemById(itemId);

        // Then
        assertAll("getItemById returns non-deleted item", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getId()).isEqualTo(itemId),
                () -> assertThat(result.getName()).isEqualTo("Coca Cola"));
    }

    @Test
    @DisplayName("getItemById should throw NoSuchElementException when item not found or deleted")
    void getItemById_whenItemNotFound_throwsException() {
        // Given
        Long itemId = 999L;
        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.getItemById(itemId));
    }

    @Test
    @DisplayName("createItem should save and return mapped item")
    void createItem_whenValidDto_savesAndReturnsMappedItem() {
        // Given
        InventoryItemRequestDto requestDto = InventoryItemRequestDto.builder()
                .name("Coca Cola")
                .unitPrice(new BigDecimal("3.50"))
                .currentStock(100)
                .build();
        InventoryItem entity = InventoryItem.builder().name("Coca Cola").deleted(false).build();
        InventoryItem savedEntity = InventoryItem.builder().id(1L).name("Coca Cola").deleted(false).build();
        InventoryItemDto responseDto = InventoryItemDto.builder().id(1L).name("Coca Cola").build();

        doNothing().when(inventoryItemValidator).validateItemNameUniqueness(null, "Coca Cola");
        when(inventoryItemMapper.toEntity(requestDto)).thenReturn(entity);
        when(inventoryItemRepository.save(entity)).thenReturn(savedEntity);
        when(inventoryItemMapper.toDto(savedEntity)).thenReturn(responseDto);

        // When
        InventoryItemDto result = minibarService.createItem(requestDto);

        // Then
        assertAll("createItem validates, saves and returns item", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getId()).isEqualTo(1L),
                () -> verify(inventoryItemValidator, times(1)).validateItemNameUniqueness(null, "Coca Cola"),
                () -> verify(inventoryItemRepository, times(1)).save(entity));
    }

    @Test
    @DisplayName("createItem should throw BusinessValidationException when name already exists")
    void createItem_whenDuplicateName_throwsException() {
        // Given
        InventoryItemRequestDto requestDto = InventoryItemRequestDto.builder()
                .name("Coca Cola")
                .unitPrice(new BigDecimal("3.50"))
                .currentStock(100)
                .build();

        doThrow(new BusinessValidationException("name", "An inventory item with the name 'Coca Cola' already exists"))
                .when(inventoryItemValidator)
                .validateItemNameUniqueness(null, "Coca Cola");

        // When/Then
        assertThrows(BusinessValidationException.class, () -> minibarService.createItem(requestDto));
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateItem should update and return mapped item when found")
    void updateItem_whenItemExists_updatesAndReturnsItem() {
        // Given
        Long itemId = 1L;
        InventoryItemRequestDto requestDto = InventoryItemRequestDto.builder()
                .name("Updated Name")
                .unitPrice(new BigDecimal("5.00"))
                .currentStock(200)
                .build();
        InventoryItem existingItem = InventoryItem.builder().id(itemId).name("Old Name").deleted(false).build();
        InventoryItem updatedItem = InventoryItem.builder().id(itemId).name("Updated Name").deleted(false).build();
        InventoryItemDto responseDto = InventoryItemDto.builder().id(itemId).name("Updated Name").build();

        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(existingItem));
        doNothing().when(inventoryItemValidator).validateItemNameUniqueness(itemId, "Updated Name");
        when(inventoryItemRepository.save(existingItem)).thenReturn(updatedItem);
        when(inventoryItemMapper.toDto(updatedItem)).thenReturn(responseDto);

        // When
        InventoryItemDto result = minibarService.updateItem(itemId, requestDto);

        // Then
        assertAll("updateItem validates, updates and returns item", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getName()).isEqualTo("Updated Name"),
                () -> verify(inventoryItemValidator, times(1)).validateItemNameUniqueness(itemId, "Updated Name"),
                () -> verify(inventoryItemMapper, times(1)).updateEntity(existingItem, requestDto),
                () -> verify(inventoryItemRepository, times(1)).save(existingItem));
    }

    @Test
    @DisplayName("updateItem should allow updating item with same name")
    void updateItem_whenSameName_allowsUpdate() {
        // Given
        Long itemId = 1L;
        InventoryItemRequestDto requestDto = InventoryItemRequestDto.builder()
                .name("Same Name")
                .unitPrice(new BigDecimal("5.00"))
                .currentStock(200)
                .build();
        InventoryItem existingItem = InventoryItem.builder().id(itemId).name("Same Name").deleted(false).build();
        InventoryItem updatedItem = InventoryItem.builder().id(itemId).name("Same Name").deleted(false).build();
        InventoryItemDto responseDto = InventoryItemDto.builder().id(itemId).name("Same Name").build();

        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(existingItem));
        doNothing().when(inventoryItemValidator).validateItemNameUniqueness(itemId, "Same Name");
        when(inventoryItemRepository.save(existingItem)).thenReturn(updatedItem);
        when(inventoryItemMapper.toDto(updatedItem)).thenReturn(responseDto);

        // When
        InventoryItemDto result = minibarService.updateItem(itemId, requestDto);

        // Then
        assertAll("updateItem allows same name", () -> assertThat(result).isNotNull(),
                () -> verify(inventoryItemValidator, times(1)).validateItemNameUniqueness(itemId, "Same Name"),
                () -> verify(inventoryItemRepository, times(1)).save(existingItem));
    }

    @Test
    @DisplayName("updateItem should throw BusinessValidationException when new name already exists")
    void updateItem_whenDuplicateName_throwsException() {
        // Given
        Long itemId = 1L;
        InventoryItemRequestDto requestDto = InventoryItemRequestDto.builder()
                .name("Existing Name")
                .unitPrice(new BigDecimal("5.00"))
                .currentStock(200)
                .build();
        InventoryItem existingItem = InventoryItem.builder().id(itemId).name("Old Name").deleted(false).build();

        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(existingItem));
        doThrow(new BusinessValidationException("name",
                "An inventory item with the name 'Existing Name' already exists")).when(inventoryItemValidator)
                .validateItemNameUniqueness(itemId, "Existing Name");

        // When/Then
        assertThrows(BusinessValidationException.class, () -> minibarService.updateItem(itemId, requestDto));
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateItem should throw NoSuchElementException when item not found or deleted")
    void updateItem_whenItemNotFound_throwsException() {
        // Given
        Long itemId = 999L;
        InventoryItemRequestDto requestDto = InventoryItemRequestDto.builder().name("Test").build();
        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.updateItem(itemId, requestDto));
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteItem should soft delete item when found")
    void deleteItem_whenItemExists_softDeletesItem() {
        // Given
        Long itemId = 1L;
        InventoryItem item = InventoryItem.builder().id(itemId).name("Coca Cola").deleted(false).build();
        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(item)).thenReturn(item);

        ArgumentCaptor<InventoryItem> itemCaptor = ArgumentCaptor.forClass(InventoryItem.class);

        // When
        minibarService.deleteItem(itemId);

        // Then
        verify(inventoryItemRepository, times(1)).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getDeleted()).isTrue();
        verify(inventoryItemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteItem should throw NoSuchElementException when item not found or already deleted")
    void deleteItem_whenItemNotFound_throwsException() {
        // Given
        Long itemId = 999L;
        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.deleteItem(itemId));
        verify(inventoryItemRepository, never()).delete(any());
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordConsumption should record consumption with active booking")
    void recordConsumption_whenActiveBookingExists_recordsConsumption() {
        // Given
        Long roomId = 1L;
        Long itemId = 2L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        InventoryItem item =
                InventoryItem.builder().id(itemId).name("Coca Cola").currentStock(100).deleted(false).build();
        Booking booking = Booking.builder().id(10L).build();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(2).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(inventoryItemRepository.findByIdInAndDeletedFalse(any())).thenReturn(List.of(item));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class)))
                .thenReturn(Optional.of(booking));

        // When
        minibarService.recordConsumption(roomId, requestDto, false);

        // Then
        assertAll("recordConsumption records and saves consumption",
                () -> assertThat(item.getCurrentStock()).isEqualTo(98),
                () -> verify(inventoryItemRepository).saveAll(anyList()),
                () -> verify(minibarConsumptionRepository).saveAll(anyList()));
    }

    @Test
    @DisplayName("recordConsumption should throw NoSuchElementException when room not found")
    void recordConsumption_whenRoomNotFound_throwsException() {
        // Given
        Long roomId = 999L;
        MinibarConsumptionRequestDto requestDto = MinibarConsumptionRequestDto.builder().itemId(1L).quantity(1).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.recordConsumption(roomId, requestDto, false));
        verify(minibarConsumptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("recordConsumption should throw NoSuchElementException when item not found")
    void recordConsumption_whenItemNotFound_throwsException() {
        // Given
        Long roomId = 1L;
        Long itemId = 999L;
        Room room = Room.builder().id(roomId).build();
        Booking booking = Booking.builder().id(10L).build();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(1).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class)))
                .thenReturn(Optional.of(booking));
        when(inventoryItemRepository.findByIdInAndDeletedFalse(any())).thenReturn(List.of()); // Item not found

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.recordConsumption(roomId, requestDto, false));
        verify(minibarConsumptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("recordConsumption should throw BusinessValidationException when no active booking")
    void recordConsumption_whenNoActiveBooking_throwsException() {
        // Given
        Long roomId = 1L;
        Long itemId = 2L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(1).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class))).thenReturn(Optional.empty());

        // When/Then
        assertThrows(BusinessValidationException.class,
                () -> minibarService.recordConsumption(roomId, requestDto, false));
        verify(minibarConsumptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("recordConsumption should throw BusinessValidationException when insufficient stock")
    void recordConsumption_whenInsufficientStock_throwsException() {
        // Given
        Long roomId = 1L;
        Long itemId = 2L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        InventoryItem item = InventoryItem.builder().id(itemId).name("Coca Cola").currentStock(1).build();
        Booking booking = Booking.builder().id(10L).build();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(5).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class)))
                .thenReturn(Optional.of(booking));
        when(inventoryItemRepository.findByIdInAndDeletedFalse(any())).thenReturn(List.of(item));

        // When/Then
        assertThrows(BusinessValidationException.class,
                () -> minibarService.recordConsumption(roomId, requestDto, false));
        verify(minibarConsumptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("restockItem should increase stock and return updated item")
    void restockItem_whenValidRequest_increasesStock() {
        // Given
        Long roomId = 1L;
        Long itemId = 2L;
        Room room = Room.builder().id(roomId).build();
        InventoryItem item =
                InventoryItem.builder().id(itemId).name("Coca Cola").currentStock(50).deleted(false).build();
        InventoryItem updatedItem =
                InventoryItem.builder().id(itemId).name("Coca Cola").currentStock(60).deleted(false).build();
        InventoryItemDto responseDto = InventoryItemDto.builder().id(itemId).currentStock(60).build();
        MinibarRestockRequestDto requestDto = MinibarRestockRequestDto.builder().itemId(itemId).quantity(10).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(item)).thenReturn(updatedItem);
        when(inventoryItemMapper.toDto(updatedItem)).thenReturn(responseDto);

        ArgumentCaptor<InventoryItem> itemCaptor = ArgumentCaptor.forClass(InventoryItem.class);

        // When
        InventoryItemDto result = minibarService.restockItem(roomId, requestDto);

        // Then
        assertAll("restockItem increases stock", () -> verify(inventoryItemRepository).save(itemCaptor.capture()),
                () -> assertThat(itemCaptor.getValue().getCurrentStock()).isEqualTo(60),
                () -> assertThat(result.getCurrentStock()).isEqualTo(60));
    }

    @Test
    @DisplayName("restockItem should throw NoSuchElementException when room not found")
    void restockItem_whenRoomNotFound_throwsException() {
        // Given
        Long roomId = 999L;
        MinibarRestockRequestDto requestDto = MinibarRestockRequestDto.builder().itemId(1L).quantity(10).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.restockItem(roomId, requestDto));
    }

    @Test
    @DisplayName("restockItem should throw NoSuchElementException when item not found or deleted")
    void restockItem_whenItemNotFound_throwsException() {
        // Given
        Long roomId = 1L;
        Long itemId = 999L;
        Room room = Room.builder().id(roomId).build();
        MinibarRestockRequestDto requestDto = MinibarRestockRequestDto.builder().itemId(itemId).quantity(10).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.restockItem(roomId, requestDto));
    }

    // ============== recordConsumption with allowRecentBooking=true Tests ==============

    @Test
    @DisplayName("recordConsumption with allowRecentBooking should record consumption with active booking")
    void recordConsumption_withAllowRecentAndActiveBooking_recordsSuccessfully() {
        // Given
        Long roomId = 1L;
        Long itemId = 2L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        InventoryItem item = InventoryItem.builder().id(itemId).name("Coca Cola").currentStock(100).build();
        Booking booking = Booking.builder().id(10L).status(BookingStatus.CHECKED_IN).build();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(2).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(inventoryItemRepository.findByIdInAndDeletedFalse(any())).thenReturn(List.of(item));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class)))
                .thenReturn(Optional.of(booking));

        // When
        minibarService.recordConsumption(roomId, requestDto, true);

        // Then
        assertAll("recordConsumption with allowRecentBooking records and saves consumption",
                () -> assertThat(item.getCurrentStock()).isEqualTo(98),
                () -> verify(inventoryItemRepository).saveAll(anyList()),
                () -> verify(minibarConsumptionRepository).saveAll(anyList()));
    }

    @Test
    @DisplayName("recordConsumption with allowRecentBooking should record consumption with recent checkout")
    void recordConsumption_withAllowRecentAndRecentCheckout_recordsSuccessfully() {
        // Given
        Long roomId = 1L;
        Long itemId = 2L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        InventoryItem item = InventoryItem.builder().id(itemId).name("Coca Cola").currentStock(100).build();
        Booking booking = Booking.builder()
                .id(10L)
                .status(BookingStatus.CHECKED_OUT)
                .checkOutDate(LocalDate.now()) // Checked out today
                .build();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(2).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(inventoryItemRepository.findByIdInAndDeletedFalse(any())).thenReturn(List.of(item));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class))).thenReturn(Optional.empty());
        when(bookingRepository.findMostRecentBookingForRoom(room)).thenReturn(Optional.of(booking));

        // When
        minibarService.recordConsumption(roomId, requestDto, true);

        // Then
        assertAll("recordConsumption with allowRecentBooking records with recent checkout",
                () -> assertThat(item.getCurrentStock()).isEqualTo(98),
                () -> verify(inventoryItemRepository).saveAll(anyList()),
                () -> verify(minibarConsumptionRepository).saveAll(anyList()));
    }

    @Test
    @DisplayName("recordConsumption with allowRecentBooking should throw exception for old checkout")
    void recordConsumption_withAllowRecentAndOldCheckout_throwsException() {
        // Given
        Long roomId = 1L;
        Long itemId = 2L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        Booking booking = Booking.builder()
                .id(10L)
                .status(BookingStatus.CHECKED_OUT)
                .checkOutDate(LocalDate.now().minusDays(3)) // Checked out 3 days ago
                .build();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(2).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class))).thenReturn(Optional.empty());
        when(bookingRepository.findMostRecentBookingForRoom(room)).thenReturn(Optional.of(booking));

        // When/Then
        assertThrows(BusinessValidationException.class,
                () -> minibarService.recordConsumption(roomId, requestDto, true));
        verify(minibarConsumptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("recordConsumption with allowRecentBooking should throw exception when no booking found")
    void recordConsumption_withAllowRecentAndNoBooking_throwsException() {
        // Given
        Long roomId = 1L;
        Long itemId = 2L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(2).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class))).thenReturn(Optional.empty());
        when(bookingRepository.findMostRecentBookingForRoom(room)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(BusinessValidationException.class,
                () -> minibarService.recordConsumption(roomId, requestDto, true));
        verify(minibarConsumptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("recordConsumption with allowRecentBooking should throw exception when insufficient stock")
    void recordConsumption_withAllowRecentAndInsufficientStock_throwsException() {
        // Given
        Long roomId = 1L;
        Long itemId = 2L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        InventoryItem item = InventoryItem.builder().id(itemId).name("Coca Cola").currentStock(1).build();
        Booking booking = Booking.builder().id(10L).status(BookingStatus.CHECKED_IN).build();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(5).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(inventoryItemRepository.findByIdInAndDeletedFalse(any())).thenReturn(List.of(item));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class)))
                .thenReturn(Optional.of(booking));

        // When/Then
        assertThrows(BusinessValidationException.class,
                () -> minibarService.recordConsumption(roomId, requestDto, true));
        verify(minibarConsumptionRepository, never()).saveAll(anyList());
    }

    // ============== recordBatchConsumption Tests ==============

    @Test
    @DisplayName("recordBatchConsumption should record multiple items atomically")
    void recordBatchConsumption_withMultipleItems_recordsAtomically() {
        // Given
        Long roomId = 1L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        Booking booking = Booking.builder().id(10L).status(BookingStatus.CHECKED_IN).build();

        InventoryItem item1 = InventoryItem.builder().id(1L).name("Coca Cola").currentStock(100).build();
        InventoryItem item2 = InventoryItem.builder().id(2L).name("Sprite").currentStock(50).build();

        List<MinibarConsumptionRequestDto> items =
                List.of(MinibarConsumptionRequestDto.builder().itemId(1L).quantity(2).build(),
                        MinibarConsumptionRequestDto.builder().itemId(2L).quantity(3).build());

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class)))
                .thenReturn(Optional.of(booking));
        when(inventoryItemRepository.findByIdInAndDeletedFalse(any())).thenReturn(List.of(item1, item2));

        // When
        minibarService.recordBatchConsumption(roomId, items, false);

        // Then
        assertAll("recordBatchConsumption records all items", () -> assertThat(item1.getCurrentStock()).isEqualTo(98),
                () -> assertThat(item2.getCurrentStock()).isEqualTo(47),
                () -> verify(inventoryItemRepository).saveAll(anyList()),
                () -> verify(minibarConsumptionRepository).saveAll(anyList()));
    }

    @Test
    @DisplayName("recordBatchConsumption should roll back all changes when one item has insufficient stock")
    void recordBatchConsumption_withInsufficientStock_throwsExceptionAndNoChanges() {
        // Given
        Long roomId = 1L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        Booking booking = Booking.builder().id(10L).status(BookingStatus.CHECKED_IN).build();

        InventoryItem item1 = InventoryItem.builder().id(1L).name("Coca Cola").currentStock(100).build();
        InventoryItem item2 = InventoryItem.builder().id(2L).name("Sprite").currentStock(1).build(); // Insufficient
                                                                                                     // stock

        List<MinibarConsumptionRequestDto> items =
                List.of(MinibarConsumptionRequestDto.builder().itemId(1L).quantity(2).build(),
                        MinibarConsumptionRequestDto.builder().itemId(2L).quantity(5).build()); // Requesting 5 but only
                                                                                                // 1 available

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class)))
                .thenReturn(Optional.of(booking));
        when(inventoryItemRepository.findByIdInAndDeletedFalse(any())).thenReturn(List.of(item1, item2));

        // When/Then
        assertThrows(BusinessValidationException.class,
                () -> minibarService.recordBatchConsumption(roomId, items, false));

        // Verify no changes were made (stock unchanged, nothing saved)
        assertAll("No changes made when validation fails", () -> assertThat(item1.getCurrentStock()).isEqualTo(100), // Unchanged
                () -> assertThat(item2.getCurrentStock()).isEqualTo(1), // Unchanged
                () -> verify(inventoryItemRepository, never()).saveAll(anyList()),
                () -> verify(minibarConsumptionRepository, never()).saveAll(anyList()));
    }

    @Test
    @DisplayName("recordBatchConsumption should handle duplicate item IDs by aggregating quantities")
    void recordBatchConsumption_withDuplicateItemIds_aggregatesQuantities() {
        // Given
        Long roomId = 1L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        Booking booking = Booking.builder().id(10L).status(BookingStatus.CHECKED_IN).build();

        InventoryItem item1 = InventoryItem.builder().id(1L).name("Coca Cola").currentStock(100).build();

        // Same item ID twice with different quantities
        List<MinibarConsumptionRequestDto> items =
                List.of(MinibarConsumptionRequestDto.builder().itemId(1L).quantity(2).build(),
                        MinibarConsumptionRequestDto.builder().itemId(1L).quantity(3).build()); // Total: 5

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class)))
                .thenReturn(Optional.of(booking));
        when(inventoryItemRepository.findByIdInAndDeletedFalse(any())).thenReturn(List.of(item1));

        // When
        minibarService.recordBatchConsumption(roomId, items, false);

        // Then
        assertAll("Duplicate item IDs are aggregated", () -> assertThat(item1.getCurrentStock()).isEqualTo(95), // 100 -
                                                                                                                // 5 =
                                                                                                                // 95
                () -> verify(inventoryItemRepository).saveAll(anyList()),
                () -> verify(minibarConsumptionRepository).saveAll(anyList()));
    }

    @Test
    @DisplayName("recordBatchConsumption should throw exception when item not found")
    void recordBatchConsumption_withItemNotFound_throwsException() {
        // Given
        Long roomId = 1L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        Booking booking = Booking.builder().id(10L).status(BookingStatus.CHECKED_IN).build();

        InventoryItem item1 = InventoryItem.builder().id(1L).name("Coca Cola").currentStock(100).build();

        List<MinibarConsumptionRequestDto> items =
                List.of(MinibarConsumptionRequestDto.builder().itemId(1L).quantity(2).build(),
                        MinibarConsumptionRequestDto.builder().itemId(999L).quantity(3).build()); // Non-existent item

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class)))
                .thenReturn(Optional.of(booking));
        when(inventoryItemRepository.findByIdInAndDeletedFalse(any())).thenReturn(List.of(item1)); // Only item1 exists

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.recordBatchConsumption(roomId, items, false));
        verify(minibarConsumptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("recordBatchConsumption should throw exception when room not found")
    void recordBatchConsumption_withRoomNotFound_throwsException() {
        // Given
        Long roomId = 999L;
        List<MinibarConsumptionRequestDto> items =
                List.of(MinibarConsumptionRequestDto.builder().itemId(1L).quantity(2).build());

        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.recordBatchConsumption(roomId, items, false));
        verify(minibarConsumptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("recordBatchConsumption should throw exception when no active booking")
    void recordBatchConsumption_withNoActiveBooking_throwsException() {
        // Given
        Long roomId = 1L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        List<MinibarConsumptionRequestDto> items =
                List.of(MinibarConsumptionRequestDto.builder().itemId(1L).quantity(2).build());

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class))).thenReturn(Optional.empty());

        // When/Then
        assertThrows(BusinessValidationException.class,
                () -> minibarService.recordBatchConsumption(roomId, items, false));
        verify(minibarConsumptionRepository, never()).saveAll(anyList());
    }

    // ============== recordBatchConsumption with allowRecentBooking=true Tests ==============

    @Test
    @DisplayName("recordBatchConsumption with allowRecentBooking should work with recently checked-out booking")
    void recordBatchConsumption_withAllowRecentAndRecentCheckout_recordsSuccessfully() {
        // Given
        Long roomId = 1L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        Booking booking = Booking.builder()
                .id(10L)
                .status(BookingStatus.CHECKED_OUT)
                .checkOutDate(LocalDate.now()) // Checked out today
                .build();

        InventoryItem item1 = InventoryItem.builder().id(1L).name("Coca Cola").currentStock(100).build();
        InventoryItem item2 = InventoryItem.builder().id(2L).name("Sprite").currentStock(50).build();

        List<MinibarConsumptionRequestDto> items =
                List.of(MinibarConsumptionRequestDto.builder().itemId(1L).quantity(2).build(),
                        MinibarConsumptionRequestDto.builder().itemId(2L).quantity(3).build());

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class))).thenReturn(Optional.empty());
        when(bookingRepository.findMostRecentBookingForRoom(room)).thenReturn(Optional.of(booking));
        when(inventoryItemRepository.findByIdInAndDeletedFalse(any())).thenReturn(List.of(item1, item2));

        // When
        minibarService.recordBatchConsumption(roomId, items, true);

        // Then
        assertAll("recordBatchConsumption with allowRecentBooking records all items",
                () -> assertThat(item1.getCurrentStock()).isEqualTo(98),
                () -> assertThat(item2.getCurrentStock()).isEqualTo(47),
                () -> verify(inventoryItemRepository).saveAll(anyList()),
                () -> verify(minibarConsumptionRepository).saveAll(anyList()));
    }

    @Test
    @DisplayName("recordBatchConsumption with allowRecentBooking should throw exception for old checkout")
    void recordBatchConsumption_withAllowRecentAndOldCheckout_throwsException() {
        // Given
        Long roomId = 1L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        Booking booking = Booking.builder()
                .id(10L)
                .status(BookingStatus.CHECKED_OUT)
                .checkOutDate(LocalDate.now().minusDays(3)) // Checked out 3 days ago
                .build();

        List<MinibarConsumptionRequestDto> items =
                List.of(MinibarConsumptionRequestDto.builder().itemId(1L).quantity(2).build());

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class))).thenReturn(Optional.empty());
        when(bookingRepository.findMostRecentBookingForRoom(room)).thenReturn(Optional.of(booking));

        // When/Then
        assertThrows(BusinessValidationException.class,
                () -> minibarService.recordBatchConsumption(roomId, items, true));
        verify(minibarConsumptionRepository, never()).saveAll(anyList());
    }

    // ============== Soft Delete Tests ==============

    @Test
    @DisplayName("getAllItems should not return deleted items")
    void getAllItems_whenDeletedActiveItemsExist_doesNotReturnThem() {
        // Given
        InventoryItem activeItem = InventoryItem.builder().id(1L).name("Coca Cola").deleted(false).build();
        InventoryItemDto activeDto = InventoryItemDto.builder().id(1L).name("Coca Cola").build();

        when(inventoryItemRepository.findByDeletedFalse()).thenReturn(List.of(activeItem));
        when(inventoryItemMapper.toDto(activeItem)).thenReturn(activeDto);

        // When
        List<InventoryItemDto> result = minibarService.getAllActiveItems();

        // Then
        assertAll("getAllItems returns only active items", () -> assertThat(result).hasSize(1),
                () -> assertThat(result).extracting(InventoryItemDto::getName).containsExactly("Coca Cola"));
    }

    @Test
    @DisplayName("createItem should allow creating item with same name as deleted item")
    void createItem_whenNameSameAsDeletedItem_savesSuccessfully() {
        // Given
        String itemName = "Deleted Item";
        InventoryItemRequestDto requestDto = InventoryItemRequestDto.builder()
                .name(itemName)
                .unitPrice(new BigDecimal("3.50"))
                .currentStock(100)
                .build();
        InventoryItem entity = InventoryItem.builder().name(itemName).deleted(false).build();
        InventoryItem savedEntity = InventoryItem.builder().id(1L).name(itemName).deleted(false).build();
        InventoryItemDto responseDto = InventoryItemDto.builder().id(1L).name(itemName).build();

        // Validator allows creation because deleted items are excluded
        doNothing().when(inventoryItemValidator).validateItemNameUniqueness(null, itemName);
        when(inventoryItemMapper.toEntity(requestDto)).thenReturn(entity);
        when(inventoryItemRepository.save(entity)).thenReturn(savedEntity);
        when(inventoryItemMapper.toDto(savedEntity)).thenReturn(responseDto);

        // When
        InventoryItemDto result = minibarService.createItem(requestDto);

        // Then
        assertAll("createItem allows name reuse after deletion", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getName()).isEqualTo(itemName),
                () -> verify(inventoryItemRepository, times(1)).save(entity));
    }

    @Test
    @DisplayName("getItemById should not return deleted item")
    void getItemById_whenItemDeleted_throwsException() {
        // Given
        Long itemId = 1L;
        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.getItemById(itemId));
    }

    @Test
    @DisplayName("updateItem should not update deleted item")
    void updateItem_whenItemDeleted_throwsException() {
        // Given
        Long itemId = 1L;
        InventoryItemRequestDto requestDto = InventoryItemRequestDto.builder().name("Updated").build();
        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.updateItem(itemId, requestDto));
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteItem should be idempotent - deleting already deleted item throws exception")
    void deleteItem_whenAlreadyDeleted_throwsException() {
        // Given
        Long itemId = 1L;
        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.deleteItem(itemId));
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordConsumption should not use deleted items")
    void recordConsumption_whenItemDeleted_throwsException() {
        // Given
        Long roomId = 1L;
        Long itemId = 2L;
        Room room = Room.builder().id(roomId).roomNumber("101").build();
        Booking booking = Booking.builder().id(10L).build();
        MinibarConsumptionRequestDto requestDto =
                MinibarConsumptionRequestDto.builder().itemId(itemId).quantity(2).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findCurrentBookingForRoom(eq(room), any(LocalDate.class)))
                .thenReturn(Optional.of(booking));
        when(inventoryItemRepository.findByIdInAndDeletedFalse(any())).thenReturn(List.of()); // No items found

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.recordConsumption(roomId, requestDto, false));
        verify(minibarConsumptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("restockItem should not restock deleted items")
    void restockItem_whenItemDeleted_throwsException() {
        // Given
        Long roomId = 1L;
        Long itemId = 2L;
        Room room = Room.builder().id(roomId).build();
        MinibarRestockRequestDto requestDto = MinibarRestockRequestDto.builder().itemId(itemId).quantity(10).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(inventoryItemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NoSuchElementException.class, () -> minibarService.restockItem(roomId, requestDto));
        verify(inventoryItemRepository, never()).save(any());
    }
}
