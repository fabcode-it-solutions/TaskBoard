package tuwien.taskinn.integrationtest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.Guest;
import tuwien.taskinn.entity.Payment;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.services.EmailService;

@SpringBootTest
@Disabled("This is a manual test that sends a real email. Enable only when needed.")
@DisplayName("Manual Email Sending Test")
public class ManualEmailSendingTest {

    @Autowired
    private EmailService emailService;

    @Value("${email.username}")
    private String emailAddressFromConfig;

    @Test
    @DisplayName("Should send a real booking confirmation email to a specified address")
    void sendRealBookingConfirmationEmail() {

        Booking sampleBooking = createSampleBooking(emailAddressFromConfig);

        assertDoesNotThrow(() -> {
            emailService.sendBookingConfirmationEmail(emailAddressFromConfig, sampleBooking);
        }, "Email sending should not throw any exceptions.");
    }

    private Booking createSampleBooking(String recipientEmail) {
        // We create detached entities. Since we're not saving them to a DB for this test,
        // we don't need real IDs.
        Guest guest = Guest.builder().id(1L).firstName("John").lastName("Doe").email(recipientEmail).build();

        RoomType deluxeType = RoomType.builder().id(1L).name("Deluxe King Room").capacity(2).build();
        RoomType suiteType = RoomType.builder().id(2L).name("Ocean View Suite").capacity(4).build();

        Room room301 = Room.builder().id(301L).roomNumber("301").roomType(deluxeType).build();
        Room room302 = Room.builder().id(302L).roomNumber("302").roomType(suiteType).build();

        Booking booking = Booking.builder()
                .id(12345L)
                .guest(guest)
                .checkInDate(LocalDate.now().plusDays(10))
                .checkOutDate(LocalDate.now().plusDays(14))
                .numAdults(2)
                .numChildren(1)
                .rooms(Set.of(room301, room302))
                .payments(Set.of(Payment.builder().amount(new BigDecimal("450.75")).build(),
                        Payment.builder().amount(new BigDecimal("225.25")).build()))
                .build();

        return booking;
    }
}
