package tuwien.taskinn.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tuwien.taskinn.dto.response.InvoiceLineItemDto;
import tuwien.taskinn.dto.response.InvoicePreviewDto;
import tuwien.taskinn.entity.*;
import tuwien.taskinn.enums.PaymentStatus;
import tuwien.taskinn.repo.*;
import tuwien.taskinn.services.InvoiceService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public InvoicePreviewDto calculateInvoicePreview(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found with ID: " + bookingId));

        List<InvoiceLineItemDto> items = new ArrayList<>();

        long nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        if (nights < 1)
            nights = 1;

        for (Room room : booking.getRooms()) {
            BigDecimal roomTotal = room.getEffectiveBasePrice().multiply(BigDecimal.valueOf(nights));
            items.add(InvoiceLineItemDto.builder()
                    .description("Room Charge - " + room.getRoomType().getName() + " (" + nights + " nights)")
                    .quantity(1)
                    .unitPrice(roomTotal)
                    .total(roomTotal)
                    .category("ROOM")
                    .build());
        }

        boolean earlyCheckIn = booking.getEarlyCheckIn();
        boolean lateCheckOut = booking.getLateCheckOut();

        if (earlyCheckIn) {
            BigDecimal earlyCheckInFee = BigDecimal.valueOf(20L * booking.getRooms().size());
            items.add(InvoiceLineItemDto.builder()
                    .description("Early Check-In Fee")
                    .quantity(1)
                    .unitPrice(earlyCheckInFee)
                    .total(earlyCheckInFee)
                    .category("SERVICE")
                    .build());
        }

        if (lateCheckOut) {
            BigDecimal lateCheckOutFee = BigDecimal.valueOf(20L * booking.getRooms().size());
            items.add(InvoiceLineItemDto.builder()
                    .description("Late Check-Out Fee")
                    .quantity(1)
                    .unitPrice(lateCheckOutFee)
                    .total(lateCheckOutFee)
                    .category("SERVICE")
                    .build());
        }

        Map<InventoryItem, Integer> minibarAggregated = booking.getMinibarConsumptions()
                .stream()
                .collect(Collectors.groupingBy(MinibarConsumption::getInventoryItem,
                        Collectors.summingInt(MinibarConsumption::getQuantity)));

        for (var entry : minibarAggregated.entrySet()) {
            InventoryItem item = entry.getKey();
            int qty = entry.getValue();
            BigDecimal total = item.getUnitPrice().multiply(BigDecimal.valueOf(qty));

            items.add(InvoiceLineItemDto.builder()
                    .description("Minibar: " + item.getName())
                    .quantity(qty)
                    .unitPrice(item.getUnitPrice())
                    .total(total)
                    .category("MINIBAR")
                    .build());
        }

        BigDecimal totalAmount =
                items.stream().map(InvoiceLineItemDto::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal alreadyPaid = booking.getPayments()
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        InvoicePreviewDto preview = InvoicePreviewDto.builder()
                .bookingId(booking.getId())
                .guestName(booking.getGuest().getFirstName() + " " + booking.getGuest().getLastName())
                .items(items)
                .totalAmount(totalAmount)
                .alreadyPaid(alreadyPaid)
                .remainingBalance(totalAmount.subtract(alreadyPaid))
                .build();

        log.info("Calculated invoice preview for booking ID {}: {}", bookingId, preview);

        return preview;
    }

    @Override
    public void addPayment(Long bookingId, Double amount) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found with ID: " + bookingId));

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(BigDecimal.valueOf(amount))
                .status(PaymentStatus.SUCCEEDED)
                .createdAt(Instant.now())
                .build();

        paymentRepository.save(payment);

        log.info("Added payment of {} for booking ID {}", amount, bookingId);
    }
}
