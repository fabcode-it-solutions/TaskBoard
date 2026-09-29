package tuwien.taskinn.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import tuwien.taskinn.dto.request.InventoryItemRequestDto;
import tuwien.taskinn.dto.request.MinibarConsumptionRequestDto;
import tuwien.taskinn.dto.request.MinibarRestockRequestDto;
import tuwien.taskinn.dto.response.InventoryItemDto;
import tuwien.taskinn.dto.response.InventoryItemSelectionResponseDto;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.InventoryItem;
import tuwien.taskinn.entity.MinibarConsumption;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.mapper.InventoryItemMapper;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.InventoryItemRepository;
import tuwien.taskinn.repo.MinibarConsumptionRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.services.MinibarService;
import tuwien.taskinn.services.validator.InventoryItemValidator;

@Service
@RequiredArgsConstructor
public class MinibarServiceImpl implements MinibarService {

    private final InventoryItemRepository inventoryItemRepository;
    private final MinibarConsumptionRepository minibarConsumptionRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final InventoryItemMapper inventoryItemMapper;
    private final InventoryItemValidator inventoryItemValidator;

    @Override
    public List<InventoryItemDto> getAllActiveItems() {
        return inventoryItemRepository.findByDeletedFalse()
                .stream()
                .map(inventoryItemMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public InventoryItemDto getItemById(Long id) {
        InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Inventory item not found: " + id));
        return inventoryItemMapper.toDto(item);
    }

    @Override
    @Transactional
    public InventoryItemDto createItem(InventoryItemRequestDto dto) {
        // Validate that item name is unique among non-deleted items
        inventoryItemValidator.validateItemNameUniqueness(null, dto.getName());

        InventoryItem item = inventoryItemMapper.toEntity(dto);
        InventoryItem savedItem = inventoryItemRepository.save(item);
        return inventoryItemMapper.toDto(savedItem);
    }

    @Override
    @Transactional
    public InventoryItemDto updateItem(Long id, InventoryItemRequestDto dto) {
        InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Inventory item not found: " + id));

        // Validate that item name is unique among non-deleted items
        inventoryItemValidator.validateItemNameUniqueness(id, dto.getName());

        inventoryItemMapper.updateEntity(item, dto);
        InventoryItem updatedItem = inventoryItemRepository.save(item);
        return inventoryItemMapper.toDto(updatedItem);
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Inventory item not found: " + id));
        item.setDeleted(true);
        inventoryItemRepository.save(item);
    }

    @Override
    @Transactional
    public void recordConsumption(Long roomId, MinibarConsumptionRequestDto dto, boolean allowRecentBooking) {
        recordBatchConsumption(roomId, List.of(dto), allowRecentBooking);
    }

    @Override
    @Transactional
    public InventoryItemDto restockItem(Long roomId, MinibarRestockRequestDto dto) {
        // Verify room exists
        roomRepository.findById(roomId).orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));

        InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalse(dto.getItemId())
                .orElseThrow(() -> new NoSuchElementException("Inventory item not found: " + dto.getItemId()));

        // Add to stock
        item.setCurrentStock(item.getCurrentStock() + dto.getQuantity());
        InventoryItem updatedItem = inventoryItemRepository.save(item);
        return inventoryItemMapper.toDto(updatedItem);
    }

    @Override
    @Transactional
    public void recordBatchConsumption(Long roomId, List<MinibarConsumptionRequestDto> items,
            boolean allowRecentBooking) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));

        Booking booking = findBookingForRoom(room, allowRecentBooking);

        processBatchConsumption(room, booking, items);
    }

    @Override
    public List<InventoryItemSelectionResponseDto> getAllItemsForSelection() {
        return inventoryItemRepository.findAllIdsAndNames();
    }

    private Booking findBookingForRoom(Room room, boolean allowRecentBooking) {
        LocalDate today = LocalDate.now();
        Optional<Booking> currentBooking = bookingRepository.findCurrentBookingForRoom(room, today);

        if (currentBooking.isPresent()) {
            return currentBooking.get();
        }

        if (!allowRecentBooking) {
            throw new BusinessValidationException("room", "No active booking found for room " + room.getRoomNumber());
        }

        Booking recentBooking = bookingRepository.findMostRecentBookingForRoom(room)
                .orElseThrow(() -> new BusinessValidationException("room",
                        "No recent booking found for room " + room.getRoomNumber()));

        if (recentBooking.getStatus() == BookingStatus.CHECKED_OUT
                && recentBooking.getCheckOutDate().isBefore(today.minusDays(1))) {
            throw new BusinessValidationException("room", "No recent booking found for room " + room.getRoomNumber());
        }

        return recentBooking;
    }

    @Override
    public boolean hasValidBookingForMinibar(Long roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return false;
        }

        LocalDate today = LocalDate.now();

        Optional<Booking> currentBooking = bookingRepository.findCurrentBookingForRoom(room, today);
        if (currentBooking.isPresent()) {
            return true;
        }

        Optional<Booking> recentBooking = bookingRepository.findMostRecentBookingForRoom(room);
        if (recentBooking.isPresent()) {
            Booking booking = recentBooking.get();
            if (booking.getStatus() == BookingStatus.CHECKED_OUT
                    && !booking.getCheckOutDate().isBefore(today.minusDays(1))) {
                return true;
            }
        }

        return false;
    }

    private void processBatchConsumption(Room room, Booking booking, List<MinibarConsumptionRequestDto> items) {
        Map<Long, Integer> aggregatedQuantities = new HashMap<>();
        for (MinibarConsumptionRequestDto dto : items) {
            aggregatedQuantities.merge(dto.getItemId(), dto.getQuantity(), Integer::sum);
        }

        Set<Long> itemIds = aggregatedQuantities.keySet();
        List<InventoryItem> inventoryItems = inventoryItemRepository.findByIdInAndDeletedFalse(itemIds);

        if (inventoryItems.size() != itemIds.size()) {
            Set<Long> foundIds = inventoryItems.stream().map(InventoryItem::getId).collect(Collectors.toSet());
            Set<Long> missingIds = itemIds.stream().filter(id -> !foundIds.contains(id)).collect(Collectors.toSet());
            throw new NoSuchElementException("Inventory items not found: " + missingIds);
        }

        Map<Long, InventoryItem> itemMap =
                inventoryItems.stream().collect(Collectors.toMap(InventoryItem::getId, item -> item));

        for (Map.Entry<Long, Integer> entry : aggregatedQuantities.entrySet()) {
            Long itemId = entry.getKey();
            Integer requestedQuantity = entry.getValue();
            InventoryItem item = itemMap.get(itemId);

            if (item.getCurrentStock() < requestedQuantity) {
                throw new BusinessValidationException("quantity", "Insufficient stock for item '" + item.getName()
                        + "'. Available: " + item.getCurrentStock() + ", requested: " + requestedQuantity);
            }
        }

        List<MinibarConsumption> consumptions = new ArrayList<>();
        Instant now = Instant.now();

        for (Map.Entry<Long, Integer> entry : aggregatedQuantities.entrySet()) {
            Long itemId = entry.getKey();
            Integer quantity = entry.getValue();
            InventoryItem item = itemMap.get(itemId);

            item.setCurrentStock(item.getCurrentStock() - quantity);

            MinibarConsumption consumption = MinibarConsumption.builder()
                    .quantity(quantity)
                    .recordedAt(now)
                    .room(room)
                    .booking(booking)
                    .inventoryItem(item)
                    .build();
            consumptions.add(consumption);
        }

        inventoryItemRepository.saveAll(inventoryItems);
        minibarConsumptionRepository.saveAll(consumptions);
    }
}
