package tuwien.taskinn.services;

import tuwien.taskinn.dto.response.InvoicePreviewDto;

public interface InvoiceService {

    /**
     * Calculates an invoice preview for a given booking ID.
     *
     * @param bookingId the ID of the booking
     * @return the invoice preview DTO
     */
    InvoicePreviewDto calculateInvoicePreview(Long bookingId);

    /**
     * Adds a payment to the invoice associated with the given booking ID. This payment is assumed to be made in cash or
     * by card directly at the hotel.
     *
     * @param bookingId
     * @param amount
     */
    void addPayment(Long bookingId, Double amount);
}
