package tuwien.taskinn.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemDto {

    private Long id;

    private String name;

    private String category;

    private String unitOfMeasurement;

    private BigDecimal unitPrice;

    private Integer currentStock;

    private Integer reorderStock;

    private Instant createdAt;

    private Instant updatedAt;
}
