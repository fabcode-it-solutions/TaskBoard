package tuwien.taskinn.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tuwien.taskinn.dto.response.BookingSummaryResponseDto;
import tuwien.taskinn.dto.response.RoomForCheckinResponseDto;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.Room;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Retrieve the id, number and capacity of all rooms reserved for an existing booking.
     *
     * @param bookingId Unique identifier of the queried booking
     * @return List of DTOs containing information relevant to build the check-in form.
     */
    @Query("""
                SELECT new tuwien.taskinn.dto.response.RoomForCheckinResponseDto(r.id, r.roomNumber, rt.capacity)
                FROM Booking b
                JOIN b.rooms r
                JOIN r.roomType rt
                WHERE b.id = :bookingId
            """)
    List<RoomForCheckinResponseDto> findRoomsByBookingId(@Param("bookingId") Long bookingId);

    /**
     * Find all bookings for a specific room that have a check-out date in the future.
     *
     * @param room the room to check
     * @param date the current date
     * @return list of bookings
     */
    @Query("SELECT b FROM Booking b JOIN b.rooms r WHERE r = :room AND b.checkOutDate >= :date")
    List<Booking> findFutureBookingsForRoom(@Param("room") Room room, @Param("date") LocalDate date);

    @Query("SELECT new tuwien.taskinn.dto.response.BookingSummaryResponseDto(" + "b.id, "
            + "CONCAT(g.firstName, ' ', g.lastName), " + "b.checkInDate, " + "b.checkOutDate, " + "SIZE(b.rooms), "
            + "b.status) " + "FROM Booking b JOIN b.guest g " + "ORDER BY b.checkInDate DESC")
    List<BookingSummaryResponseDto> findAllProjectedBy();

    @Query("SELECT b FROM Booking b JOIN FETCH b.guest JOIN FETCH b.rooms WHERE b.status = 'CHECKED_IN' AND :today >= b.checkInDate AND :today <= b.checkOutDate")
    List<Booking> findAllCurrentBookings(@Param("today") LocalDate today);

    @Query("SELECT b FROM Booking b JOIN FETCH b.guest WHERE :room MEMBER OF b.rooms AND b.status = 'CHECKED_IN' AND :today >= b.checkInDate AND :today <= b.checkOutDate")
    Optional<Booking> findCurrentBookingForRoom(@Param("room") Room room, @Param("today") LocalDate today);

    /**
     * Count the number of bookings that need to be checked in on a specific date. A booking needs check-in if the
     * check-in date matches the specified date and the status is CONFIRMED.
     *
     * @param date the check-in date
     * @return the count of bookings needing check-in
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.checkInDate = :date
            AND b.status = 'CONFIRMED'
            """)
    Long countBookingsNeedingCheckInOnDate(@Param("date") LocalDate date);

    /**
     * Count the number of bookings that need to be checked out on a specific date. A booking needs check-out if the
     * check-out date matches the specified date and the status is CHECKED_IN.
     *
     * @param date the check-out date
     * @return the count of bookings needing check-out
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.checkOutDate = :date
            AND b.status = 'CHECKED_IN'
            """)
    Long countBookingsNeedingCheckOutOnDate(@Param("date") LocalDate date);

    /**
     * Find the most recent booking for a room (active or recently checked out). Used by cleaning staff to record
     * minibar consumption after guest checkout.
     *
     * @param room the room to check
     * @return the most recent booking, if any
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE :room MEMBER OF b.rooms
            AND b.status IN ('CHECKED_IN', 'CHECKED_OUT')
            ORDER BY b.checkOutDate DESC
            LIMIT 1
            """)
    Optional<Booking> findMostRecentBookingForRoom(@Param("room") Room room);

    @Query("""
                SELECT DISTINCT r.id FROM Booking b JOIN b.rooms r
                WHERE b.checkInDate = :day
                AND b.earlyCheckIn = TRUE
            """)
    List<Long> findAllEarlyCheckInRoomIdsOnDay(@Param("day") LocalDate day);

    @Query("""
                SELECT DISTINCT r.id FROM Booking b JOIN b.rooms r
                WHERE b.checkOutDate = :day
                AND b.lateCheckOut = TRUE
            """)
    List<Long> findAllLateCheckOutRoomIdsOnDay(@Param("day") LocalDate day);

    /**
     * Count all bookings grouped by their check-in day (full history).
     *
     * @return list of tuples ordered by day ascending
     */
    @Query("SELECT b.checkInDate, COUNT(b) FROM Booking b GROUP BY b.checkInDate ORDER BY b.checkInDate")
    List<Object[]> findAllBookingsCountByCheckInDay();

    /**
     * Count bookings grouped by their check-in day within an inclusive date range.
     *
     * @param from inclusive start date
     * @param to inclusive end date
     * @return list of tuples ordered by day ascending
     */
    @Query("SELECT b.checkInDate, COUNT(b) FROM Booking b " + "WHERE b.checkInDate BETWEEN :from AND :to "
            + "GROUP BY b.checkInDate ORDER BY b.checkInDate")
    List<Object[]> findBookingsCountByCheckInDayInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Count all bookings grouped by check-in month (full history). Uses database YEAR/MONTH extraction via
     * {@code FUNCTION('YEAR', ...)} / {@code FUNCTION('MONTH', ...)}.
     *
     * @return list of tuples ordered by year asc, month asc
     */
    @Query("""
            SELECT FUNCTION('YEAR', b.checkInDate), FUNCTION('MONTH', b.checkInDate), COUNT(b)
            FROM Booking b
            GROUP BY FUNCTION('YEAR', b.checkInDate), FUNCTION('MONTH', b.checkInDate)
            ORDER BY FUNCTION('YEAR', b.checkInDate), FUNCTION('MONTH', b.checkInDate)
            """)
    List<Object[]> countAllBookingsByCheckInMonth();

    /**
     * Count bookings grouped by check-in month within an inclusive date range.
     *
     * @param from inclusive start date
     * @param to inclusive end date
     * @return list of tuples ordered by year asc, month asc
     */
    @Query("""
            SELECT FUNCTION('YEAR', b.checkInDate), FUNCTION('MONTH', b.checkInDate), COUNT(b)
            FROM Booking b
            WHERE b.checkInDate BETWEEN :from AND :to
            GROUP BY FUNCTION('YEAR', b.checkInDate), FUNCTION('MONTH', b.checkInDate)
            ORDER BY FUNCTION('YEAR', b.checkInDate), FUNCTION('MONTH', b.checkInDate)
            """)
    List<Object[]> countBookingsByCheckInMonthInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Count all bookings grouped by room type (full history), ordered by most booked room type first.
     *
     * @return list of tuples ordered by count desc
     */
    @Query("""
            SELECT rt.name, COUNT(b)
            FROM Booking b JOIN b.rooms r JOIN r.roomType rt
            GROUP BY rt.name ORDER BY COUNT(b) DESC
            """)
    List<Object[]> countAllBookingsByRoomType();

    /**
     * Count bookings grouped by room type within an inclusive check-in date range, ordered by most booked type first.
     *
     * @param from inclusive start date
     * @param to inclusive end date
     * @return list of tuples ordered by count desc
     */
    @Query("""
            SELECT rt.name, COUNT(b)
            FROM Booking b JOIN b.rooms r JOIN r.roomType rt
            WHERE b.checkInDate BETWEEN :from AND :to
            GROUP BY rt.name ORDER BY COUNT(b) DESC
            """)
    List<Object[]> countBookingsByRoomTypeInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
