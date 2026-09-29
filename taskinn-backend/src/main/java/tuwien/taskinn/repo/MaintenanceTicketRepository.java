package tuwien.taskinn.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import tuwien.taskinn.entity.MaintenanceTicket;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.TicketStatus;

import java.util.List;
import java.util.Set;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {

    /**
     * Find maintenance tickets by room ID
     *
     * @param roomId the ID of the room
     * @return list of MaintenanceTicket
     */
    List<MaintenanceTicket> findByRoomId(Long roomId);

    @Query("SELECT t.room.id FROM MaintenanceTicket t WHERE t.status IN :statuses")
    Set<Long> findRoomIdsWithOpenTickets(@Param("statuses") List<TicketStatus> statuses);

    boolean existsByRoomAndStatusIn(Room room, List<TicketStatus> statuses);

    /**
     * Find all maintenance tickets with status OPEN
     *
     * @return list of MaintenanceTicket with status OPEN
     */
    @Query("SELECT mt FROM MaintenanceTicket mt WHERE mt.status = 'OPEN'")
    List<MaintenanceTicket> findMaintenanceTicketsByStatus_Open();

    /**
     * Count high priority tickets that need attention. Counts tickets with priority HIGH or URGENT and status OPEN or
     * IN_PROGRESS.
     *
     * @return count of high priority tickets needing attention
     */
    @Query("""
            SELECT COUNT(mt) FROM MaintenanceTicket mt
            WHERE mt.priority IN ('HIGH', 'URGENT')
            AND mt.status IN ('OPEN', 'IN_PROGRESS')
            """)
    Long countHighPriorityTicketsNeedingAttention();
}
