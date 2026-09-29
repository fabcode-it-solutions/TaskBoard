package tuwien.taskinn.mapper;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.response.DetailedBookingResponseDto;
import tuwien.taskinn.dto.response.InvoiceLineItemDto;
import tuwien.taskinn.dto.response.InvoicePreviewDto;
import tuwien.taskinn.entity.*;
import tuwien.taskinn.enums.PaymentStatus;

/**
 * Mapper for converting between Booking entity and Booking DTOs.
 */
@Component
public class BookingMapper {

    /**
     * Converts a Booking entity into a detailed DTO for API responses. Assumes that all necessary related entities
     * (guest, rooms, payments, etc.) have been eagerly fetched.
     *
     * @param booking The source Booking entity.
     * @return A {@link DetailedBookingResponseDto} containing the mapped data.
     */
    public DetailedBookingResponseDto toDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        // --- Map Primary Guest ---
        DetailedBookingResponseDto.GuestInfo primaryGuestInfo = DetailedBookingResponseDto.GuestInfo.builder()
                .id(booking.getGuest().getId())
                .firstName(booking.getGuest().getFirstName())
                .lastName(booking.getGuest().getLastName())
                .dateOfBirth(booking.getGuest().getDateOfBirth())
                .email(booking.getGuest().getEmail())
                .phone(booking.getGuest().getPhone())
                .build();

        List<DetailedBookingResponseDto.RoomInfo> roomInfos = booking.getRooms()
                .stream()
                .map(room -> DetailedBookingResponseDto.RoomInfo.builder()
                        .id(room.getId())
                        .roomNumber(room.getRoomNumber())
                        .roomTypeName(room.getRoomType().getName())
                        .capacity(room.getEffectiveCapacity())
                        .build())
                .sorted(Comparator.comparing(DetailedBookingResponseDto.RoomInfo::getRoomNumber)) // Sort for consistent
                                                                                                  // order
                .collect(Collectors.toList());

        List<DetailedBookingResponseDto.CheckedInGuestInfo> checkedInGuestInfos =
                (booking.getCheckedInGuests() != null)
                        ? booking.getCheckedInGuests()
                                .stream()
                                .map(guest -> DetailedBookingResponseDto.CheckedInGuestInfo.builder()
                                        .id(guest.getId())
                                        .firstName(guest.getFirstName())
                                        .lastName(guest.getLastName())
                                        .build())
                                .collect(Collectors.toList())
                        : Collections.emptyList();

        BigDecimal totalPrice = (booking.getPayments() != null)
                ? booking.getPayments().stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        return DetailedBookingResponseDto.builder()
                .bookingId(booking.getId())
                .status(booking.getStatus())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numAdults(booking.getNumAdults())
                .numChildren(booking.getNumChildren())
                .totalPrice(totalPrice)
                .primaryGuest(primaryGuestInfo)
                .rooms(roomInfos)
                .checkedInGuests(checkedInGuestInfos.isEmpty() ? null : checkedInGuestInfos)
                .createdAt(booking.getCreatedAt())
                .earlyCheckIn(booking.getEarlyCheckIn())
                .lateCheckOut(booking.getLateCheckOut())
                .build();
    }

    /**
     * Converts a Booking entity into an InvoicePreviewDto for email generation. This method aggregates minibar items
     * and calculates the final balance.
     *
     * @param booking The source Booking entity with all related data loaded.
     * @return The populated InvoicePreviewDto.
     */
    public InvoicePreviewDto toInvoicePreviewDto(Booking booking) {
        if (booking == null) {
            return null;
        }

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

        if (booking.getMinibarConsumptions() != null) {
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
        }

        BigDecimal totalAmount =
                items.stream().map(InvoiceLineItemDto::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal alreadyPaid = BigDecimal.ZERO;
        if (booking.getPayments() != null) {
            alreadyPaid = booking.getPayments()
                    .stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return InvoicePreviewDto.builder()
                .bookingId(booking.getId())
                .guestName(booking.getGuest().getFirstName() + " " + booking.getGuest().getLastName())
                .items(items)
                .totalAmount(totalAmount)
                .alreadyPaid(alreadyPaid)
                .remainingBalance(totalAmount.subtract(alreadyPaid))
                .build();
    }
}
