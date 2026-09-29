package tuwien.taskinn.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.MinibarConsumption;
import tuwien.taskinn.entity.Room;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Collection;
import java.time.Instant;

public interface MinibarConsumptionRepository extends JpaRepository<MinibarConsumption, Long> {

    /**
     * Find all minibar consumptions for a specific room
     *
     * @param room the room
     * @return list of minibar consumptions for the room
     */
    List<MinibarConsumption> findAllByRoom(Room room);

    /**
     * Find all minibar consumptions for a specific booking
     *
     * @param booking the booking
     * @return list of minibar consumptions for the booking
     */
    List<MinibarConsumption> findAllByBooking(Booking booking);

    /**
     * Find all minibar consumptions for a specific room and booking
     *
     * @param room the room
     * @param booking the booking
     * @return list of minibar consumptions
     */
    List<MinibarConsumption> findAllByRoomAndBooking(Room room, Booking booking);

    /**
     * Aggregates sales and revenue by inventory item within a timeframe.
     *
     * @param start inclusive start timestamp for filtering (can be null for no start limit)
     * @param end exclusive end timestamp for filtering (can be null for no end limit)
     * @return [0] = Item Name, [1] = Sum Quantity, [2] = Sum Revenue (qty * price)
     */
    @Query("""
                SELECT mc.inventoryItem.name,
                       SUM(mc.quantity),
                       SUM(mc.quantity * mc.inventoryItem.unitPrice)
                FROM MinibarConsumption mc
                WHERE (:start IS NULL OR mc.recordedAt >= :start)
                  AND (:end IS NULL OR mc.recordedAt < :end)
                GROUP BY mc.inventoryItem.name
            """)
    List<Object[]> findAggregatedStats(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Retrieves raw consumption data for specific items to generate line charts in the service layer.
     *
     * @param itemIds list of inventory item IDs to filter by
     * @param start inclusive start timestamp for filtering (can be null for no start limit)
     * @param end exclusive end timestamp for filtering (can be null for no end limit)
     * @return [0] = Item ID, [1] = RecordedAt, [2] = Quantity
     */
    @Query("""
                SELECT mc.inventoryItem.id, mc.recordedAt, mc.quantity
                FROM MinibarConsumption mc
                WHERE mc.inventoryItem.id IN :itemIds
                  AND (:start IS NULL OR mc.recordedAt >= :start)
                  AND (:end IS NULL OR mc.recordedAt < :end)
            """)
    List<Object[]> findConsumptionsForItems(@Param("itemIds") Collection<Long> itemIds, @Param("start") Instant start,
            @Param("end") Instant end);
}
