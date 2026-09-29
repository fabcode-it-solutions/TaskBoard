package tuwien.taskinn.mapper;

import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.request.InventoryItemRequestDto;
import tuwien.taskinn.dto.response.InventoryItemDto;
import tuwien.taskinn.entity.InventoryItem;

@Component
public class InventoryItemMapper {

    public InventoryItemDto toDto(InventoryItem entity) {
        if (entity == null) {
            return null;
        }

        return InventoryItemDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .category(entity.getCategory())
                .unitOfMeasurement(entity.getUnitOfMeasurement())
                .unitPrice(entity.getUnitPrice())
                .currentStock(entity.getCurrentStock())
                .reorderStock(entity.getReorderStock())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public InventoryItem toEntity(InventoryItemRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return InventoryItem.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .unitOfMeasurement(dto.getUnitOfMeasurement())
                .unitPrice(dto.getUnitPrice())
                .currentStock(dto.getCurrentStock())
                .reorderStock(dto.getReorderStock())
                .deleted(false)
                .build();
    }

    public void updateEntity(InventoryItem entity, InventoryItemRequestDto dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setUnitOfMeasurement(dto.getUnitOfMeasurement());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setCurrentStock(dto.getCurrentStock());
        entity.setReorderStock(dto.getReorderStock());
    }
}
