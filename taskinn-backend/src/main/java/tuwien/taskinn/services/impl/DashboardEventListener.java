package tuwien.taskinn.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.response.DashboardRoomDto;
import tuwien.taskinn.dto.response.MaintenanceTicketDto;
import tuwien.taskinn.events.BookingStateChangedEvent;
import tuwien.taskinn.events.CleaningTaskUpdatedEvent;
import tuwien.taskinn.events.MaintenanceTicketChangedEvent;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.services.MaintenanceService;
import tuwien.taskinn.services.RoomDashboardService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    private final RoomDashboardService roomDashboardService;
    private final BookingRepository bookingRepository;
    private final MaintenanceService maintenanceService;

    /**
     * Listens for BookingStateChangedEvent and pushes an update to the dashboard topic.
     */
    @EventListener
    public void handleBookingStateChange(BookingStateChangedEvent event) {
        log.info("Handling booking state change event for booking ID: {}", event.bookingId());

        bookingRepository.findById(event.bookingId()).ifPresent(booking -> {
            booking.getRooms().forEach(room -> {
                DashboardRoomDto updatedRoomDto = roomDashboardService.getUpdatedStateForRoom(room.getId());

                messagingTemplate.convertAndSend("/topic/dashboard-updates", updatedRoomDto);
                log.info("Broadcasted dashboard update for room: {}", updatedRoomDto.getRoomNumber());
            });
        });
    }

    /**
     * Listens for CleaningTaskUpdatedEvent.
     */
    @EventListener
    public void handleCleaningStateChange(CleaningTaskUpdatedEvent event) {
        log.info("Handling cleaning state change event for room ID: {}", event.roomId());

        DashboardRoomDto updatedRoomDto = roomDashboardService.getUpdatedStateForRoom(event.roomId());
        messagingTemplate.convertAndSend("/topic/dashboard-updates", updatedRoomDto);
        log.info("Broadcasted dashboard update for room: {}", updatedRoomDto.getRoomNumber());
    }

    /**
     * Listens for MaintenanceTicketChangedEvent.
     */
    @EventListener
    public void handleMaintenanceStateChange(MaintenanceTicketChangedEvent event) {
        log.info("Handling maintenance state change event for room ID: {}", event.roomId());

        DashboardRoomDto updatedRoomDto = roomDashboardService.getUpdatedStateForRoom(event.roomId());
        messagingTemplate.convertAndSend("/topic/dashboard-updates", updatedRoomDto);
        log.info("Broadcasted dashboard update for room: {}", updatedRoomDto.getRoomNumber());

        List<MaintenanceTicketDto> allTickets = maintenanceService.getAllMaintenanceTickets();
        messagingTemplate.convertAndSend("/topic/maintenance-updates", allTickets);
        log.info("Broadcasted maintenance board update with {} tickets", allTickets.size());
    }
}
