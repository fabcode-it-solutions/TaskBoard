package tuwien.taskinn.services;

import tuwien.taskinn.dto.request.InventoryItemRequestDto;
import tuwien.taskinn.dto.request.MinibarConsumptionRequestDto;
import tuwien.taskinn.dto.request.MinibarRestockRequestDto;
import tuwien.taskinn.dto.response.InventoryItemDto;
import tuwien.taskinn.dto.response.InventoryItemSelectionResponseDto;

import java.util.List;

public interface MinibarService {

    /**
     * Get all inventory items
     *
     * @return list of all inventory items
     */
    List<InventoryItemDto> getAllActiveItems();

    /**
     * Get an inventory item by ID
     *
     * @param id the item ID
     * @return the inventory item
     */
    InventoryItemDto getItemById(Long id);

    /**
     * Create a new inventory item
     *
     * @param dto the creation DTO
     * @return the created inventory item
     */
    InventoryItemDto createItem(InventoryItemRequestDto dto);

    /**
     * Update an existing inventory item
     *
     * @param id the item ID
     * @param dto the update DTO
     * @return the updated inventory item
     */
    InventoryItemDto updateItem(Long id, InventoryItemRequestDto dto);

    /**
     * Delete an inventory item
     *
     * @param id the item ID
     */
    void deleteItem(Long id);

    /**
     * Record a minibar consumption for a room. Delegates to batch consumption internally.
     *
     * @param roomId the room ID
     * @param dto the consumption DTO
     * @param allowRecentBooking if true, allows consumption to be recorded against recently checked-out bookings
     *        (within 24 hours), useful for cleaning staff reporting consumption after checkout
     */
    void recordConsumption(Long roomId, MinibarConsumptionRequestDto dto, boolean allowRecentBooking);

    /**
     * Restock a minibar item
     *
     * @param roomId the room ID (for context/logging purposes)
     * @param dto the restock DTO
     * @return the updated inventory item
     */
    InventoryItemDto restockItem(Long roomId, MinibarRestockRequestDto dto);

    /**
     * Record multiple minibar consumptions atomically for a room. All items are validated before any changes are made,
     * ensuring atomic operation.
     *
     * @param roomId the room ID
     * @param items list of consumption DTOs
     * @param allowRecentBooking if true, allows consumption to be recorded against recently checked-out bookings
     *        (within 24 hours), useful for cleaning staff reporting consumption after checkout
     */
    void recordBatchConsumption(Long roomId, List<MinibarConsumptionRequestDto> items, boolean allowRecentBooking);

    /**
     * Retrieves a lightweight list of all items (active and deleted) for selection dropdowns.
     */
    List<InventoryItemSelectionResponseDto> getAllItemsForSelection();

    /**
     * Check if minibar reporting is allowed for a room based on booking status. Returns true if the room has an active
     * CHECKED_IN booking or a recently checked-out booking (within 24 hours).
     *
     * @param roomId the room ID
     * @return true if minibar reporting is allowed, false otherwise
     */
    boolean hasValidBookingForMinibar(Long roomId);
}
