package tuwien.taskinn.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.entity.*;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.GuestRepository;
import tuwien.taskinn.repo.PaymentRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.repo.RoomTypeRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Invoice Controller Integration Tests")
public class InvoiceControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String INVOICE_BASE_URI = BASE_URI + "/invoice";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    // Repositories are inherited from BaseIntegrationTest, but listing for clarity
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private RoomTypeRepository roomTypeRepository;
    @Autowired
    private GuestRepository guestRepository;
    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("GET /invoice/{id} -> Returns correct invoice preview for admin")
    void previewInvoice_whenAuthorized_returnsPreview() throws Exception {
        Long bookingId = createTestBooking();
        String token = getAdminAuthToken();

        mockMvc.perform(get(INVOICE_BASE_URI + "/" + bookingId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.totalAmount").value(100.0))
                .andExpect(jsonPath("$.remainingBalance").value(100.0));
    }

    @Test
    @DisplayName("GET /invoice/{id} -> Fails with 403 for unauthorized user")
    void previewInvoice_whenUnauthorized_returnsForbidden() throws Exception {
        Long bookingId = createTestBooking();

        mockMvc.perform(get(INVOICE_BASE_URI + "/" + bookingId)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /payment/{id} -> Successfully adds a payment")
    void addPayment_whenAuthorized_savesPayment() throws Exception {
        Long bookingId = createTestBooking();
        String token = getAdminAuthToken();
        String requestJson = "{\"amount\": 50.0}";

        mockMvc.perform(
                post(INVOICE_BASE_URI + "/payment/" + bookingId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getPayments()).hasSize(1);
        Payment payment = booking.getPayments().iterator().next();
        assertThat(payment.getAmount()).isEqualByComparingTo("50.0");
    }

    private Long createTestBooking() {
        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .email("i@test.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build());
        RoomType rt = roomTypeRepository
                .save(RoomType.builder().name("Test").basePrice(new BigDecimal("100")).capacity(2).build());
        Room room = roomRepository.save(Room.builder()
                .roomNumber("101")
                .floor(1)
                .status(RoomStatus.OCCUPIED)
                .active(true)
                .roomType(rt)
                .svgPoints(List.of(new Point(0d, 0d), new Point(1d, 0d), new Point(1d, 1d), new Point(0d, 1d)))
                .build());

        Booking booking = bookingRepository.save(Booking.builder()
                .guest(guest)
                .rooms(Set.of(room))
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .numAdults(1)
                .status(BookingStatus.CHECKED_IN)
                .earlyCheckIn(false)
                .lateCheckOut(false)
                .build());

        return booking.getId();
    }
}
