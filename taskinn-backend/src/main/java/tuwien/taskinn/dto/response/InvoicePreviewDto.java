package tuwien.taskinn.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class InvoicePreviewDto {
    private Long bookingId;
    private String guestName;

    private List<InvoiceLineItemDto> items;

    private BigDecimal subTotal;
    private BigDecimal totalAmount;
    private BigDecimal alreadyPaid;
    private BigDecimal remainingBalance;
}
