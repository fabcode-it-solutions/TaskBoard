package tuwien.taskinn.unittest.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import tuwien.taskinn.dto.request.InventoryItemRequestDto;
import tuwien.taskinn.dto.response.InventoryItemDto;
import tuwien.taskinn.entity.InventoryItem;
import tuwien.taskinn.mapper.InventoryItemMapper;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryItemMapper Unit Tests")
public class InventoryItemMapperTest {

    @InjectMocks
    private InventoryItemMapper inventoryItemMapper;

    @Test
    @DisplayName("toDto should correctly map InventoryItem entity to InventoryItemDto")
    void toDto_whenValidEntity_correctlyMapsToDto() {
        // Given
        Instant now = Instant.now();
        InventoryItem entity = InventoryItem.builder()
                .id(1L)
                .name("Coca Cola")
                .category("Beverages")
                .unitOfMeasurement("bottle")
                .unitPrice(new BigDecimal("3.50"))
                .currentStock(100)
                .reorderStock(20)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // When
        InventoryItemDto result = inventoryItemMapper.toDto(entity);

        // Then
        assertAll("InventoryItem DTO Mapping", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getId()).isEqualTo(1L),
                () -> assertThat(result.getName()).isEqualTo("Coca Cola"),
                () -> assertThat(result.getCategory()).isEqualTo("Beverages"),
                () -> assertThat(result.getUnitOfMeasurement()).isEqualTo("bottle"),
                () -> assertThat(result.getUnitPrice()).isEqualByComparingTo(new BigDecimal("3.50")),
                () -> assertThat(result.getCurrentStock()).isEqualTo(100),
                () -> assertThat(result.getReorderStock()).isEqualTo(20),
                () -> assertThat(result.getCreatedAt()).isEqualTo(now),
                () -> assertThat(result.getUpdatedAt()).isEqualTo(now));
    }

    @Test
    @DisplayName("toDto should return null when entity is null")
    void toDto_whenEntityIsNull_returnsNull() {
        // When
        InventoryItemDto result = inventoryItemMapper.toDto(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toEntity should correctly map InventoryItemRequestDto to InventoryItem entity")
    void toEntity_whenValidDto_correctlyMapsToEntity() {
        // Given
        InventoryItemRequestDto dto = InventoryItemRequestDto.builder()
                .name("Sprite")
                .category("Beverages")
                .unitOfMeasurement("can")
                .unitPrice(new BigDecimal("2.50"))
                .currentStock(50)
                .reorderStock(10)
                .build();

        // When
        InventoryItem result = inventoryItemMapper.toEntity(dto);

        // Then
        assertAll("InventoryItem Entity Mapping", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getId()).isNull(), () -> assertThat(result.getName()).isEqualTo("Sprite"),
                () -> assertThat(result.getCategory()).isEqualTo("Beverages"),
                () -> assertThat(result.getUnitOfMeasurement()).isEqualTo("can"),
                () -> assertThat(result.getUnitPrice()).isEqualByComparingTo(new BigDecimal("2.50")),
                () -> assertThat(result.getCurrentStock()).isEqualTo(50),
                () -> assertThat(result.getReorderStock()).isEqualTo(10));
    }

    @Test
    @DisplayName("toEntity should return null when dto is null")
    void toEntity_whenDtoIsNull_returnsNull() {
        // When
        InventoryItem result = inventoryItemMapper.toEntity(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("updateEntity should update all fields of existing entity")
    void updateEntity_whenValidDtoAndEntity_updatesAllFields() {
        // Given
        InventoryItem existingEntity = InventoryItem.builder()
                .id(1L)
                .name("Old Name")
                .category("Old Category")
                .unitOfMeasurement("old unit")
                .unitPrice(new BigDecimal("1.00"))
                .currentStock(10)
                .reorderStock(5)
                .build();

        InventoryItemRequestDto updateDto = InventoryItemRequestDto.builder()
                .name("New Name")
                .category("New Category")
                .unitOfMeasurement("new unit")
                .unitPrice(new BigDecimal("5.00"))
                .currentStock(200)
                .reorderStock(50)
                .build();

        // When
        inventoryItemMapper.updateEntity(existingEntity, updateDto);

        // Then
        assertAll("InventoryItem Entity Update", () -> assertThat(existingEntity.getId()).isEqualTo(1L),
                () -> assertThat(existingEntity.getName()).isEqualTo("New Name"),
                () -> assertThat(existingEntity.getCategory()).isEqualTo("New Category"),
                () -> assertThat(existingEntity.getUnitOfMeasurement()).isEqualTo("new unit"),
                () -> assertThat(existingEntity.getUnitPrice()).isEqualByComparingTo(new BigDecimal("5.00")),
                () -> assertThat(existingEntity.getCurrentStock()).isEqualTo(200),
                () -> assertThat(existingEntity.getReorderStock()).isEqualTo(50));
    }

    @Test
    @DisplayName("updateEntity should do nothing when entity is null")
    void updateEntity_whenEntityIsNull_doesNothing() {
        // Given
        InventoryItemRequestDto dto = InventoryItemRequestDto.builder().name("Test").build();

        // When/Then - should not throw
        inventoryItemMapper.updateEntity(null, dto);
    }

    @Test
    @DisplayName("updateEntity should do nothing when dto is null")
    void updateEntity_whenDtoIsNull_doesNothing() {
        // Given
        InventoryItem entity = InventoryItem.builder().id(1L).name("Original Name").build();

        // When
        inventoryItemMapper.updateEntity(entity, null);

        // Then - entity should remain unchanged
        assertThat(entity.getName()).isEqualTo("Original Name");
    }
}
