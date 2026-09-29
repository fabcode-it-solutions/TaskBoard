package tuwien.taskinn.services;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.stripe.model.checkout.Session;

import tuwien.taskinn.dto.request.CheckInGuestDto;
import tuwien.taskinn.dto.request.CreateOrUpdateBookingRequestDto;
import tuwien.taskinn.dto.response.*;
import tuwien.taskinn.dto.request.RoomOptionsRequestDto;

public interface BookingService {
    /**
     * Returns aggregated room type options for customers based on the provided request parameters. This method groups
     * available rooms by type and effective price, and provides recommendations for how many rooms of each option would
     * fulfill the guest requirements.
     *
     * @param roomOptionsRequestDto The request parameters containing check-in date, check-out date, number of rooms,
     *        number of adults, and number of children.
     * @return A list of {@link RoomTypeOptionDto} objects representing the available room type options with
     *         recommendations.
     */
    List<RoomTypeOptionDto> getRoomTypeOptions(RoomOptionsRequestDto roomOptionsRequestDto);

    /**
     * Returns a list with all the rooms bound to one booking.
     *
     * @param bookingId The id of the booking for which we are retrieving room data.
     * @return A list of {@link RoomForCheckinResponseDto} objects representing the room number and maximum capacity of
     *         every booked room.
     */
    List<RoomForCheckinResponseDto> getRoomsForCheckinByBookingId(Long bookingId);

    /**
     * Checks in a booking with the specified guests and files.
     *
     * @param bookingId The id of the booking for which we are checking in.
     * @param guestDtos The list of guests to check in.
     * @param files The map of identity card files for each guest.
     */
    void checkInBooking(Long bookingId, List<CheckInGuestDto> guestDtos, Map<String, MultipartFile> files);

    /**
     * Creates a new booking with the specified room type selections. Automatically selects specific rooms in close
     * proximity when multiple rooms are requested.
     *
     * @param requestDto The booking request containing guest info, dates, and room type selections
     * @return A response containing the booking ID and assigned room numbers
     *
     * @implNote This method is used for manual bookings by staff and creates a new booking witht the status CONFIRMED
     */
    CreateOrUpdateBookingResponseDto createBooking(CreateOrUpdateBookingRequestDto requestDto);

    /**
     * Updates an existing booking with the specified room type selections. Automatically selects specific rooms in
     * close proximity when multiple rooms are requested.
     *
     * @param bookingId The ID of the booking to update
     * @param requestDto The updated booking request containing guest info, dates, and room type selections
     */
    void updateBooking(Long bookingId, CreateOrUpdateBookingRequestDto requestDto);

    /**
     * Completes the check-out process for one booking.
     *
     * @param bookingId The id of the booking for which we are completing the check-out process.
     */
    void checkOutBooking(Long bookingId);

    /**
     * Retrieves detailed information about a specific booking, including the primary guest, assigned rooms, and
     * checked-in guests if applicable.
     *
     * @param bookingId The ID of the booking for which details are requested.
     * @return A DetailedBookingResponseDto containing comprehensive booking information.
     */
    DetailedBookingResponseDto getBookingDetails(Long bookingId);

    /**
     * Retrieves a summary of all bookings in the system.
     * <p>
     * This method is designed for overview pages and list views where displaying the full detail of every booking would
     * be inefficient. It returns a lightweight summary representation ({@link BookingSummaryResponseDto}) of each
     * booking to ensure high performance.
     *
     * @return a {@link List} of {@link BookingSummaryResponseDto} objects, typically ordered by the most recent
     *         check-in date. The list will be empty if no bookings are found; it will never be {@code null}.
     */
    List<BookingSummaryResponseDto> findAllBookings();

    /**
     * Get the count of bookings that need to be checked in today
     *
     * @return the number of bookings needing check-in today
     */
    Long getTodaysCheckInCount();

    /**
     * Get the count of bookings that need to be checked out today
     *
     * @return the number of bookings needing check-out today
     */
    Long getTodaysCheckOutCount();

    /**
     * Begins a booking process by creating a Stripe session.
     *
     * @param requestDto The booking request containing guest info, dates, and room type selections
     * @return A Stripe session object
     *
     * @implNote This method creates creates a new booking with status PENDING
     */
    Session beginBooking(CreateOrUpdateBookingRequestDto requestDto);

    /**
     *
     * @param bookingId The id of the booking to confirm
     *
     * @implNote This method changes the status of the booking to CONFIRMED
     */
    void confirmBooking(Long bookingId);

    /**
     * Cancels a booking by deleting it
     *
     * @implNote This method deletes the booking from the database
     *
     * @param bookingId The id of the booking to cancel
     */
    void cancelBooking(Long bookingId);

    /**
     * Retrieves booking details for a Stripe checkout session.
     *
     * @param sessionId Stripe checkout session ID
     * @return Booking details for the success page
     */
    CreateOrUpdateBookingResponseDto getBookingResponseBySessionId(String sessionId);

    /**
     * Checks if a booking can be checked out
     *
     * @param bookingId The id of the booking to check
     * @return True if the booking can be checked out, false otherwise
     */
    Boolean canCheckout(Long bookingId);

    /**
     * Retrieves IDs of rooms with a late checkout/early check-in on a given day.
     *
     * @param year Year of the given day
     * @param month Month of the given day
     * @param day Day of month of the given day
     * @return DTO containing IDs of rooms with a late checkout/early check-in on day
     */
    SpecialBookingConstraintsDto getSpecialBookingConstraints(Integer year, Integer month, Integer day);
}
