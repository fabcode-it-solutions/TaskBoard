package tuwien.taskinn.services;

import tuwien.taskinn.dto.response.InvoicePreviewDto;
import tuwien.taskinn.entity.Booking;

public interface EmailService {

    /**
     * Sends a reset password email to the specified email address with the provided token. Uses the email account
     * configured in the application properties as the sender.
     *
     * @param email the recipient's email address
     * @param token the password reset token
     */
    void sendResetPasswordEmail(String email, String token);

    /**
     * Sends a booking confirmation email to the specified email address with the details of the booking. Contains
     * booking information such as dates, room type, and total price.
     *
     * @param email the recipient's email address
     * @param booking the booking entity containing booking details
     */
    void sendBookingConfirmationEmail(String email, Booking booking);

    /**
     * Sends an invoice email to the guest with the details of the invoice.
     *
     * @param invoice the invoice preview DTO containing invoice details
     */
    void sendInvoiceEmail(String toEmail, InvoicePreviewDto invoice);
}
