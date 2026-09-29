package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tuwien.taskinn.dto.response.InvoicePreviewDto;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.Guest;
import tuwien.taskinn.entity.Payment;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.PaymentStatus;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.PaymentRepository;
import tuwien.taskinn.services.impl.InvoiceServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceService Unit Tests")
public class InvoiceServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    @Test
    @DisplayName("calculateInvoicePreview should return correct totals for simple booking")
    void calculateInvoicePreview_simpleBooking_returnsCorrectTotals() {
        // Arrange
        Long bookingId = 1L;
        RoomType rt = RoomType.builder().name("Single").basePrice(new BigDecimal("100")).build();
        Room room = Room.builder().roomType(rt).build();
        Guest guest = Guest.builder().firstName("John").lastName("Doe").build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .guest(guest)
                .rooms(Set.of(room))
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2)) // 2 nights
                .minibarConsumptions(Collections.emptySet())
                .payments(Collections.emptySet())
                .earlyCheckIn(false)
                .lateCheckOut(false)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        InvoicePreviewDto result = invoiceService.calculateInvoicePreview(bookingId);

        assertThat(result).isNotNull();
        assertThat(result.getBookingId()).isEqualTo(bookingId);
        assertThat(result.getGuestName()).isEqualTo("John Doe");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("200");
        assertThat(result.getAlreadyPaid()).isEqualByComparingTo("0");
        assertThat(result.getRemainingBalance()).isEqualByComparingTo("200");
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("calculateInvoicePreview should include fees for early check-in and late check-out")
    void calculateInvoicePreview_withFees_includesFeesInTotal() {
        Long bookingId = 1L;
        RoomType rt = RoomType.builder().name("Single").basePrice(new BigDecimal("100")).build();
        Room room = Room.builder().roomType(rt).build();
        Guest guest = Guest.builder().firstName("John").lastName("Doe").build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .guest(guest)
                .rooms(Set.of(room)) // 1 Room
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1)) // 1 night
                .minibarConsumptions(Collections.emptySet())
                .payments(Collections.emptySet())
                .earlyCheckIn(true) // Should add 20
                .lateCheckOut(true) // Should add 20
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        InvoicePreviewDto result = invoiceService.calculateInvoicePreview(bookingId);

        assertThat(result.getTotalAmount()).isEqualByComparingTo("140");
        assertThat(result.getItems()).hasSize(3);

        assertThat(result.getItems()).anyMatch(item -> item.getDescription().equals("Early Check-In Fee"));
        assertThat(result.getItems()).anyMatch(item -> item.getDescription().equals("Late Check-Out Fee"));
    }

    @Test
    @DisplayName("calculateInvoicePreview should subtract already paid amounts")
    void calculateInvoicePreview_withPayments_calculatesRemainingBalance() {
        Long bookingId = 1L;
        RoomType rt = RoomType.builder().name("Single").basePrice(new BigDecimal("100")).build();
        Room room = Room.builder().roomType(rt).build();
        Guest guest = Guest.builder().firstName("John").lastName("Doe").build();

        Payment payment = Payment.builder().amount(new BigDecimal("50")).status(PaymentStatus.SUCCEEDED).build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .guest(guest)
                .rooms(Set.of(room))
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .minibarConsumptions(Collections.emptySet())
                .payments(Set.of(payment))
                .earlyCheckIn(false)
                .lateCheckOut(false)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        InvoicePreviewDto result = invoiceService.calculateInvoicePreview(bookingId);

        assertThat(result.getTotalAmount()).isEqualByComparingTo("100");
        assertThat(result.getAlreadyPaid()).isEqualByComparingTo("50");
        assertThat(result.getRemainingBalance()).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("addPayment should save a new payment entity")
    void addPayment_whenValid_savesPayment() {
        Long bookingId = 1L;
        Double amount = 50.0;
        Booking booking = Booking.builder().id(bookingId).build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        invoiceService.addPayment(bookingId, amount);

        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getBooking()).isEqualTo(booking);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo("50");
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(savedPayment.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("calculateInvoicePreview should throw exception when booking not found")
    void calculateInvoicePreview_notFound_throwsException() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> invoiceService.calculateInvoicePreview(99L));
    }
}
