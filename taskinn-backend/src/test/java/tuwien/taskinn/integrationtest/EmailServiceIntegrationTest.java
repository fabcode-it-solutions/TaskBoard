package tuwien.taskinn.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import tuwien.taskinn.dto.response.InvoiceLineItemDto;
import tuwien.taskinn.dto.response.InvoicePreviewDto;
import tuwien.taskinn.entity.*;
import tuwien.taskinn.services.EmailService;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("EmailService Integration Tests")
public class EmailServiceIntegrationTest {

    @Value("${app.frontend.reset-password-url}")
    private String frontendResetUrl;

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("testuser", "testpass"))
            .withPerMethodLifecycle(true);

    @Autowired
    private EmailService emailService;

    @Test
    @DisplayName("sendResetPasswordEmail should send a correctly formatted HTML email")
    void givenValidEmailAndToken_whenSendResetPasswordEmail_thenEmailIsSentCorrectly()
            throws MessagingException, IOException {

        String recipientEmail = "test@example.com";
        String token = "abc-123-def-456";

        emailService.sendResetPasswordEmail(recipientEmail, token);

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

        assertAll(() -> {
            assertThat(receivedMessages).hasSize(1);
            MimeMessage receivedMessage = receivedMessages[0];
            assertThat(receivedMessage.getSubject()).isEqualTo("TaskInn - Password Reset Request");
            assertThat(receivedMessage.getAllRecipients()[0].toString()).isEqualTo(recipientEmail);
            String body = (String) receivedMessage.getContent();
            assertThat(body).isNotNull();
            assertThat(body).contains("<div class=\"header\">TaskInn Password Reset</div>");
            assertThat(body).contains("href=\"" + frontendResetUrl + "?token=" + token + "\"");
            assertThat(body).contains("class=\"button\"");
        });
    }

    @Test
    @DisplayName("sendBookingConfirmationEmail should send a correctly rendered HTML email")
    void sendBookingConfirmationEmail_withValidBooking_sendsCorrectlyRenderedHtmlEmail()
            throws IOException, MessagingException {
        Booking sampleBooking = createSampleBooking("john.doe@test.com");
        String recipientEmail = sampleBooking.getGuest().getEmail();

        emailService.sendBookingConfirmationEmail(recipientEmail, sampleBooking);

        greenMail.waitForIncomingEmail(5000, 1);
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

        assertAll(() -> {
            assertThat(receivedMessages).hasSize(1);
            MimeMessage receivedMessage = receivedMessages[0];
            String body = (String) receivedMessage.getContent();

            assertThat(receivedMessage.getSubject()).isEqualTo("Your TaskInn Booking Confirmation #12345");
            assertThat(receivedMessage.getAllRecipients()[0].toString()).isEqualTo(recipientEmail);

            assertThat(body).contains("Hello <strong>John</strong>");
            assertThat(body).contains("<p>4 Nights</p>");
            assertThat(body).contains("<span>2</span> Adults");
            assertThat(body).contains("<span>, 1 Children</span>");

            assertThat(body).contains("<div class=\"room-info\">Deluxe King Room</div>");
            assertThat(body).contains("<span class=\"total-amount\">€ 676.00</span>");
        });
    }

    @Test
    @DisplayName("sendInvoiceEmail should send a correctly rendered HTML email")
    void sendInvoiceEmail_withValidInvoice_sendsCorrectlyRenderedHtmlEmail() throws IOException, MessagingException {
        String recipientEmail = "guest@test.com";
        InvoicePreviewDto invoice = createSampleInvoice();

        emailService.sendInvoiceEmail(recipientEmail, invoice);

        greenMail.waitForIncomingEmail(5000, 1);
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

        assertAll(() -> {
            assertThat(receivedMessages).hasSize(1);
            MimeMessage receivedMessage = receivedMessages[0];
            String body = (String) receivedMessage.getContent();

            // Verify email metadata
            assertThat(receivedMessage.getSubject()).isEqualTo("Your TaskInn Invoice #98765");
            assertThat(receivedMessage.getAllRecipients()[0].toString()).isEqualTo(recipientEmail);

            // Verify guest info in "Bill To" section
            assertThat(body).contains("Jane Smith");

            // Verify booking reference
            assertThat(body).contains("#<span>98765</span>");

            // Verify line items
            assertThat(body).contains("Deluxe Room - 3 nights");
            assertThat(body).contains("ROOM");
            assertThat(body).contains("Late Check-out");
            assertThat(body).contains("SERVICE");

            // Verify summary amounts
            assertThat(body).contains("€ 530.00"); // totalAmount
            assertThat(body).contains("- € 200.00"); // alreadyPaid
            assertThat(body).contains("€ 330.00"); // remainingBalance

            // Verify footer contains hotel email
            assertThat(body).contains("mailto:");
        });
    }

    /** A helper method to create a sample InvoicePreviewDto for testing. */
    private InvoicePreviewDto createSampleInvoice() {
        InvoiceLineItemDto roomCharge = InvoiceLineItemDto.builder()
                .description("Deluxe Room - 3 nights")
                .category("ROOM")
                .quantity(3)
                .unitPrice(new BigDecimal("150.00"))
                .total(new BigDecimal("450.00"))
                .build();

        InvoiceLineItemDto serviceCharge = InvoiceLineItemDto.builder()
                .description("Late Check-out")
                .category("SERVICE")
                .quantity(1)
                .unitPrice(new BigDecimal("80.00"))
                .total(new BigDecimal("80.00"))
                .build();

        return InvoicePreviewDto.builder()
                .bookingId(98765L)
                .guestName("Jane Smith")
                .items(List.of(roomCharge, serviceCharge))
                .subTotal(new BigDecimal("530.00"))
                .totalAmount(new BigDecimal("530.00"))
                .alreadyPaid(new BigDecimal("200.00"))
                .remainingBalance(new BigDecimal("330.00"))
                .build();
    }

    /** A helper method to create a realistic, fully-populated Booking entity for testing. */
    private Booking createSampleBooking(String recipientEmail) {
        Guest guest = Guest.builder().id(1L).firstName("John").lastName("Doe").email(recipientEmail).build();

        RoomType deluxeType = RoomType.builder().id(1L).name("Deluxe King Room").capacity(2).build();
        RoomType suiteType = RoomType.builder().id(2L).name("Ocean View Suite").capacity(4).build();

        Room room301 = Room.builder().id(301L).roomNumber("301").roomType(deluxeType).build();
        Room room302 = Room.builder().id(302L).roomNumber("302").roomType(suiteType).build();

        Booking booking = Booking.builder()
                .id(12345L)
                .guest(guest)
                .checkInDate(LocalDate.of(2025, 12, 1))
                .checkOutDate(LocalDate.of(2025, 12, 5)) // 4 nights
                .numAdults(2)
                .numChildren(1)
                .rooms(Set.of(room301, room302))
                .payments(Set.of(Payment.builder().amount(new BigDecimal("450.75")).build(),
                        Payment.builder().amount(new BigDecimal("225.25")).build()))
                .build();

        booking.getPayments().forEach(payment -> payment.setBooking(booking));

        return booking;
    }
}
