package tuwien.taskinn.datagenerator;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.MinibarConsumption;
import tuwien.taskinn.entity.Payment;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.enums.PaymentStatus;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.PaymentRepository;

@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class PaymentDataGenerator {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Generates payments for bookings that are not created manually. 1. Initial payment: Room price * nights + bonus
     * addons (20 per room). 2. Final payment (if checked out): Minibar consumption.
     */
    @Transactional
    public void generateData() {
        if (paymentRepository.count() > 0) {
            log.info("Payment data already exists. Skipping generation.");
            return;
        }

        List<Booking> bookings = bookingRepository.findAll();

        log.info("Generating payment and invoice data for {} bookings...", bookings.size());

        for (Booking booking : bookings) {
            if (Boolean.TRUE.equals(booking.getCreatedManually())) {
                continue;
            }

            Set<Payment> payments = new HashSet<>();

            long nights = Math.max(1, ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate()));
            int roomCount = booking.getRooms().size();

            BigDecimal roomsPricePerNight = booking.getRooms()
                    .stream()
                    .map(Room::getEffectiveBasePrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalRoomCost = roomsPricePerNight.multiply(BigDecimal.valueOf(nights));

            BigDecimal fees = BigDecimal.ZERO;
            BigDecimal feePerRoom = new BigDecimal("20.00");

            if (Boolean.TRUE.equals(booking.getEarlyCheckIn())) {
                fees = fees.add(feePerRoom.multiply(BigDecimal.valueOf(roomCount)));
            }
            if (Boolean.TRUE.equals(booking.getLateCheckOut())) {
                fees = fees.add(feePerRoom.multiply(BigDecimal.valueOf(roomCount)));
            }

            BigDecimal initialPaymentAmount = totalRoomCost.add(fees);

            Payment initialPayment = Payment.builder()
                    .booking(booking)
                    .amount(initialPaymentAmount)
                    .status(PaymentStatus.SUCCEEDED)
                    .build();

            paymentRepository.save(initialPayment);
            payments.add(initialPayment);

            BigDecimal minibarTotal = BigDecimal.ZERO;
            if (booking.getMinibarConsumptions() != null) {
                for (MinibarConsumption mc : booking.getMinibarConsumptions()) {
                    BigDecimal itemTotal =
                            mc.getInventoryItem().getUnitPrice().multiply(BigDecimal.valueOf(mc.getQuantity()));
                    minibarTotal = minibarTotal.add(itemTotal);
                }
            }

            BigDecimal totalDue = initialPaymentAmount.add(minibarTotal);
            BigDecimal totalPaid = initialPaymentAmount;

            if (booking.getStatus() == BookingStatus.CHECKED_OUT) {
                BigDecimal remainingBalance = totalDue.subtract(totalPaid);

                if (remainingBalance.compareTo(BigDecimal.ZERO) != 0) {
                    Payment finalPayment = Payment.builder()
                            .booking(booking)
                            .amount(remainingBalance)
                            .status(PaymentStatus.SUCCEEDED)
                            .build();

                    paymentRepository.save(finalPayment);
                    payments.add(finalPayment);
                }
            }

            booking.setPayments(payments);
            bookingRepository.save(booking);
        }

        log.info("Payment generation complete.");
    }
}
