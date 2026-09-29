package tuwien.taskinn.services.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import tuwien.taskinn.dto.response.InvoicePreviewDto;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.Payment;
import tuwien.taskinn.services.EmailService;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${email.username}")
    private String fromEmail;

    @Value("${app.frontend.reset-password-url}")
    private String frontendResetUrl;

    @Override
    public void sendResetPasswordEmail(String toEmail, String token) {
        try {
            Context context = new Context();
            String resetUrl = frontendResetUrl + "?token=" + token;
            context.setVariable("resetUrl", resetUrl);

            String htmlContent = templateEngine.process("email/reset-password-template.html", context);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TaskInn - Password Reset Request");
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    @Override
    public void sendBookingConfirmationEmail(String email, Booking booking) {
        try {
            String guestName = booking.getGuest().getFirstName();

            BigDecimal totalPrice =
                    booking.getPayments().stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            long durationInNights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());

            Context context = new Context();
            context.setVariable("booking", booking);
            context.setVariable("guestName", guestName);
            context.setVariable("totalPrice", totalPrice);
            context.setVariable("hotelEmail", fromEmail);
            context.setVariable("adultsCount", booking.getNumAdults());
            context.setVariable("childrenCount", booking.getNumChildren());
            context.setVariable("durationInNights", durationInNights);
            context.setVariable("lateCheckOut", booking.getLateCheckOut());
            context.setVariable("earlyCheckIn", booking.getEarlyCheckIn());

            String htmlContent = templateEngine.process("email/booking-confirmation-template", context);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Your TaskInn Booking Confirmation #" + booking.getId());
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send booking confirmation email", e);
        }
    }

    @Override
    public void sendInvoiceEmail(String toEmail, InvoicePreviewDto invoice) {
        try {
            Context context = new Context();
            context.setVariable("invoice", invoice);
            context.setVariable("hotelEmail", fromEmail);

            String htmlContent = templateEngine.process("email/invoice-template", context);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your TaskInn Invoice #" + invoice.getBookingId());
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send invoice email", e);
        }
    }
}
