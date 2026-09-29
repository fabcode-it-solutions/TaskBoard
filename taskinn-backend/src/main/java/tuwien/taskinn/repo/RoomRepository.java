package tuwien.taskinn.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("""
            SELECT DISTINCT r FROM Room r
            JOIN FETCH r.roomType rt
            WHERE r.status = 'AVAILABLE'
            AND rt.capacity >= :totalGuests
            AND r.id NOT IN (
                SELECT br.id FROM Booking b
                JOIN b.rooms br
                WHERE b.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')
                AND b.checkInDate < :checkOutDate
                AND b.checkOutDate > :checkInDate
            )
            """)
    List<Room> findAvailableRoomsByDateRangeAndCapacity(@Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate, @Param("totalGuests") Integer totalGuests);

    @Query("""
            SELECT DISTINCT r FROM Room r
            JOIN FETCH r.roomType rt
            WHERE r.status != 'OUT_OF_ORDER'
            AND r.active = true
            AND r.id NOT IN (
                SELECT br.id FROM Booking b
                JOIN b.rooms br
                WHERE b.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')
                AND b.checkInDate < :checkOutDate
                AND b.checkOutDate > :checkInDate
            )
            ORDER BY r.roomNumber
            """)
    List<Room> findAvailableRoomsByDateRange(@Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate);

    /**
     * Find the maximum floor number among all rooms
     *
     * @return the highest floor number
     */
    @Query("SELECT MAX(r.floor) FROM Room r")
    Integer getMaxFloor();

    /**
     * Find all rooms located on a specific floor
     *
     * @param floor the floor number
     * @return list of rooms on the specified floor
     */
    List<Room> findAllByFloor(Integer floor);

    /**
     * Find a room by its room number
     *
     * @param roomNumber the room number
     * @return an Optional containing the Room if found, otherwise empty
     */
    Optional<Room> findByRoomNumber(String roomNumber);

    /**
     * Find all active rooms located on a specific floor
     *
     * @param floor the floor number
     * @return list of active rooms on the specified floor
     */
    List<Room> findAllByFloorAndActiveTrue(Integer floor);

    /**
     * Check if a room exists by its room number
     *
     * @param roomNumber the room number
     * @return true if a room with the given room number exists, otherwise false
     */
    boolean existsByRoomNumber(String roomNumber);

    /**
     * Finds a Room entity by its room number and fetch its associated RoomType and maintenance tickets.
     *
     * @param roomNumber the room number
     * @return an Optional containing the Room with its RoomType and maintenanceTickets loaded, or empty if not found
     */
    @Query("SELECT r FROM Room r " + "JOIN FETCH r.roomType " + "LEFT JOIN FETCH r.maintenanceTickets "
            + "WHERE r.roomNumber = :roomNumber")
    Optional<Room> findWithRoomTypeAndTicketsByRoomNumber(@Param("roomNumber") String roomNumber);

    /**
     * Check if an active room with the given room number exists, excluding a specific room ID. Used to validate
     * uniqueness of active room numbers during create/update operations.
     *
     * @param roomNumber the room number to check
     * @param excludeId the room ID to exclude from the check (for updates), use null for creates
     * @return true if an active room with the same number exists
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Room r " + "WHERE r.roomNumber = :roomNumber "
            + "AND r.active = true " + "AND (:excludeId IS NULL OR r.id != :excludeId)")
    boolean existsActiveRoomByNumber(@Param("roomNumber") String roomNumber, @Param("excludeId") Long excludeId);

    /**
     * Find an available room of a specific type within a date range
     *
     * @param roomType the room type
     * @param startDate the start date
     * @param endDate the end date
     * @param excludedRoomId the room ID to exclude from the search
     * @return an Optional containing the available Room if found, otherwise empty
     */
    @Query("SELECT r FROM Room r WHERE r.roomType = :roomType AND r.active = true AND r.id != :excludedRoomId AND r.id NOT IN ("
            + "SELECT r2.id FROM Booking b JOIN b.rooms r2 WHERE "
            + "(b.checkInDate < :endDate AND b.checkOutDate > :startDate)" + ")")
    List<Room> findActiveAvailableRooms(@Param("roomType") RoomType roomType, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, @Param("excludedRoomId") Long excludedRoomId);

    /**
     * Find all rooms that use a specific room type
     *
     * @param roomType the room type
     * @return list of rooms using the specified room type
     */
    List<Room> findAllByRoomType(RoomType roomType);

    @Query("SELECT r FROM Room r JOIN FETCH r.roomType WHERE r.active = true")
    List<Room> findAllActiveWithDetails();

    @Query("SELECT r FROM Room r JOIN FETCH r.roomType WHERE r.id = :roomId AND r.active = true")
    Optional<Room> findByIdWithDetails(@Param("roomId") Long roomId);

    /**
     * Count the number of occupied rooms on a specific date. A room is considered occupied if it has a booking where
     * the check-in date is on or before the specified date and the check-out date is after the specified date, and the
     * booking status is CONFIRMED or CHECKED_IN.
     *
     * @param date the date to check
     * @return the count of occupied rooms
     */
    @Query("""
            SELECT COUNT(DISTINCT r.id) FROM Booking b
            JOIN b.rooms r
            WHERE r.active = true
            AND b.checkInDate <= :date
            AND b.checkOutDate >= :date
            AND b.status IN ('CONFIRMED', 'CHECKED_IN')
            """)
    Long countOccupiedRoomsOnDate(@Param("date") LocalDate date);

    /**
     * Count the total number of active rooms
     *
     * @return the count of active rooms
     */
    @Query("SELECT COUNT(r) FROM Room r WHERE r.active = true")
    Long countActiveRooms();

    /**
     * Get all distinct floor numbers that have active rooms, ordered by floor number
     *
     * @return list of floor numbers
     */
    @Query("SELECT DISTINCT r.floor FROM Room r WHERE r.active = true ORDER BY r.floor")
    List<Integer> findDistinctFloors();

    /**
     * Count the total number of active rooms on a specific floor
     *
     * @param floor the floor number
     * @return the count of active rooms on the floor
     */
    @Query("SELECT COUNT(r) FROM Room r WHERE r.active = true AND r.floor = :floor")
    Long countActiveRoomsByFloor(@Param("floor") Integer floor);

    /**
     * Count the number of occupied rooms on a specific floor on a specific date. A room is considered occupied if it
     * has a booking where the check-in date is on or before the specified date and the check-out date is after the
     * specified date, and the booking status is CONFIRMED or CHECKED_IN.
     *
     * @param floor the floor number
     * @param date the date to check
     * @return the count of occupied rooms on the floor
     */
    @Query("""
            SELECT COUNT(DISTINCT r.id) FROM Booking b
            JOIN b.rooms r
            WHERE r.active = true
            AND r.floor = :floor
            AND b.checkInDate <= :date
            AND b.checkOutDate > :date
            AND b.status IN ('CONFIRMED', 'CHECKED_IN')
            """)
    Long countOccupiedRoomsByFloorOnDate(@Param("floor") Integer floor, @Param("date") LocalDate date);
}
