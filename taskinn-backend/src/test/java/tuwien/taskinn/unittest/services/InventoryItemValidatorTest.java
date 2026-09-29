package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.repo.InventoryItemRepository;
import tuwien.taskinn.services.validator.InventoryItemValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryItemValidator Unit Tests")
public class InventoryItemValidatorTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @InjectMocks
    private InventoryItemValidator inventoryItemValidator;

    @Test
    @DisplayName("Should not throw exception when creating item with unique name")
    void validateItemNameUniqueness_whenCreatingWithUniqueName_doesNotThrow() {
        // Given
        String itemName = "Coca Cola";
        when(inventoryItemRepository.existsActiveItemByName(itemName, null)).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> inventoryItemValidator.validateItemNameUniqueness(null, itemName));
        verify(inventoryItemRepository, times(1)).existsActiveItemByName(itemName, null);
    }

    @Test
    @DisplayName("Should throw exception when creating item with duplicate name")
    void validateItemNameUniqueness_whenCreatingWithDuplicateName_throwsException() {
        // Given
        String itemName = "Coca Cola";
        when(inventoryItemRepository.existsActiveItemByName(itemName, null)).thenReturn(true);

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> inventoryItemValidator.validateItemNameUniqueness(null, itemName));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("name"),
                () -> assertThat(exception.getMessage()).contains("Coca Cola"),
                () -> assertThat(exception.getMessage()).contains("already exists"));
    }

    @Test
    @DisplayName("Should not throw exception when updating item with same name")
    void validateItemNameUniqueness_whenUpdatingWithSameName_doesNotThrow() {
        // Given
        Long itemId = 1L;
        String itemName = "Coca Cola";
        when(inventoryItemRepository.existsActiveItemByName(itemName, itemId)).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> inventoryItemValidator.validateItemNameUniqueness(itemId, itemName));
        verify(inventoryItemRepository, times(1)).existsActiveItemByName(itemName, itemId);
    }

    @Test
    @DisplayName("Should not throw exception when updating item to a unique new name")
    void validateItemNameUniqueness_whenUpdatingToUniqueName_doesNotThrow() {
        // Given
        Long itemId = 1L;
        String newItemName = "Sprite";
        when(inventoryItemRepository.existsActiveItemByName(newItemName, itemId)).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> inventoryItemValidator.validateItemNameUniqueness(itemId, newItemName));
        verify(inventoryItemRepository, times(1)).existsActiveItemByName(newItemName, itemId);
    }

    @Test
    @DisplayName("Should throw exception when updating item to a name that already exists for another item")
    void validateItemNameUniqueness_whenUpdatingToDuplicateName_throwsException() {
        // Given
        Long itemId = 1L;
        String newItemName = "Sprite";
        when(inventoryItemRepository.existsActiveItemByName(newItemName, itemId)).thenReturn(true);

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> inventoryItemValidator.validateItemNameUniqueness(itemId, newItemName));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("name"),
                () -> assertThat(exception.getMessage()).contains("Sprite"),
                () -> assertThat(exception.getMessage()).contains("already exists"));
    }

    @Test
    @DisplayName("Should allow creating item with same name as deleted item")
    void validateItemNameUniqueness_whenNameExistsButDeleted_doesNotThrow() {
        // Given - A deleted item with name "Coca Cola" exists, but repository query excludes it
        String itemName = "Coca Cola";
        when(inventoryItemRepository.existsActiveItemByName(itemName, null)).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> inventoryItemValidator.validateItemNameUniqueness(null, itemName));
    }
}
