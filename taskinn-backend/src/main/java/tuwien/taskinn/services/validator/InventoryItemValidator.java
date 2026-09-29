package tuwien.taskinn.services.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.repo.InventoryItemRepository;

@Component
@RequiredArgsConstructor
public class InventoryItemValidator {

    private final InventoryItemRepository inventoryItemRepository;

    /**
     * Validates that the item name is unique among active (non-deleted) items. Deleted items are excluded from this
     * check to allow item name reuse.
     *
     * @param itemId The ID of the item being updated (null if creating a new item).
     * @param name The item name to validate.
     * @throws BusinessValidationException if the name is already in use by another active item.
     */
    public void validateItemNameUniqueness(Long itemId, String name) {
        boolean exists = inventoryItemRepository.existsActiveItemByName(name, itemId);

        if (exists) {
            throw new BusinessValidationException("name",
                    "An inventory item with the name '" + name + "' already exists");
        }
    }
}
