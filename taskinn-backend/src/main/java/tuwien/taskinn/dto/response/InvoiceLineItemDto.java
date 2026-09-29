package tuwien.taskinn.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InvoiceLineItemDto {
    private String description;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal total;
    private String category;
}
