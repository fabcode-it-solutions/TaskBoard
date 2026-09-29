package tuwien.taskinn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tuwien.taskinn.annotation.RequireRole;
import tuwien.taskinn.annotation.RequireRoles;
import tuwien.taskinn.dto.response.InvoicePreviewDto;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.services.InvoiceService;

@Validated
@RestController
@RequestMapping("/api/v1/invoice")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/{bookingId}")
    @RequireRoles({@RequireRole(Roles.EMPLOYEE), @RequireRole(Roles.ADMIN)})
    public ResponseEntity<InvoicePreviewDto> previewInvoice(@PathVariable Long bookingId) {
        return ResponseEntity.ok(invoiceService.calculateInvoicePreview(bookingId));
    }

    @PostMapping("/payment/{bookingId}")
    @RequireRoles({@RequireRole(Roles.EMPLOYEE), @RequireRole(Roles.ADMIN)})
    public ResponseEntity<Void> addPayment(@PathVariable Long bookingId, @RequestBody PaymentRequest paymentRequest) {
        log.info("Adding payment of {} for booking ID {}", paymentRequest.amount, bookingId);

        invoiceService.addPayment(bookingId, paymentRequest.amount);
        return ResponseEntity.ok().build();
    }

    record PaymentRequest(Double amount) {
    }
}
