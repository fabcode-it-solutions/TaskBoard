package tuwien.taskinn.unittest.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tuwien.taskinn.dto.response.DashboardRoomDto;
import tuwien.taskinn.dto.response.MaintenanceTicketDto;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.enums.Priority;
import tuwien.taskinn.enums.TicketStatus;
import tuwien.taskinn.events.BookingStateChangedEvent;
import tuwien.taskinn.events.CleaningTaskUpdatedEvent;
import tuwien.taskinn.events.MaintenanceTicketChangedEvent;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.services.MaintenanceService;
import tuwien.taskinn.services.RoomDashboardService;
import tuwien.taskinn.services.impl.DashboardEventListener;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardEventListener Unit Tests")
public class DashboardEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RoomDashboardService roomDashboardService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MaintenanceService maintenanceService;

    @InjectMocks
    private DashboardEventListener dashboardEventListener;

    @Test
    @DisplayName("handleCleaningStateChange should broadcast dashboard update when event is received")
    void handleCleaningStateChange_whenEventReceived_broadcastsDashboardUpdate() {
        // Given
        Long roomId = 1L;
        RoomStatus newStatus = RoomStatus.DIRTY;
        CleaningTaskUpdatedEvent event = new CleaningTaskUpdatedEvent(roomId, newStatus);

        DashboardRoomDto mockDashboardDto =
                DashboardRoomDto.builder().roomNumber("101").roomId(roomId).status(newStatus).build();

        when(roomDashboardService.getUpdatedStateForRoom(roomId)).thenReturn(mockDashboardDto);

        ArgumentCaptor<DashboardRoomDto> dtoCaptor = ArgumentCaptor.forClass(DashboardRoomDto.class);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        // When
        dashboardEventListener.handleCleaningStateChange(event);

        // Then
        verify(roomDashboardService, times(1)).getUpdatedStateForRoom(roomId);
        verify(messagingTemplate, times(1)).convertAndSend(topicCaptor.capture(), dtoCaptor.capture());

        assertAll(() -> assertThat(topicCaptor.getValue()).isEqualTo("/topic/dashboard-updates"),
                () -> assertThat(dtoCaptor.getValue()).isEqualTo(mockDashboardDto),
                () -> assertThat(dtoCaptor.getValue().getRoomNumber()).isEqualTo("101"),
                () -> assertThat(dtoCaptor.getValue().getRoomId()).isEqualTo(roomId),
                () -> assertThat(dtoCaptor.getValue().getStatus()).isEqualTo(newStatus));
    }

    @Test
    @DisplayName("handleBookingStateChange should broadcast dashboard update for all rooms in booking")
    void handleBookingStateChange_whenBookingHasMultipleRooms_broadcastsUpdateForEachRoom() {
        // Given
        Long bookingId = 1L;
        BookingStateChangedEvent event = new BookingStateChangedEvent(bookingId, BookingStatus.CHECKED_IN);

        RoomType roomType = RoomType.builder().name("Standard").capacity(2).basePrice(BigDecimal.valueOf(100)).build();

        Room room1 = Room.builder().id(1L).roomNumber("101").roomType(roomType).status(RoomStatus.OCCUPIED).build();

        Room room2 = Room.builder().id(2L).roomNumber("102").roomType(roomType).status(RoomStatus.OCCUPIED).build();

        Booking booking = Booking.builder().id(bookingId).rooms(Set.of(room1, room2)).build();

        DashboardRoomDto dashboardDto1 =
                DashboardRoomDto.builder().roomId(1L).roomNumber("101").status(RoomStatus.OCCUPIED).build();

        DashboardRoomDto dashboardDto2 =
                DashboardRoomDto.builder().roomId(2L).roomNumber("102").status(RoomStatus.OCCUPIED).build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(roomDashboardService.getUpdatedStateForRoom(1L)).thenReturn(dashboardDto1);
        when(roomDashboardService.getUpdatedStateForRoom(2L)).thenReturn(dashboardDto2);

        // When
        dashboardEventListener.handleBookingStateChange(event);

        // Then
        verify(bookingRepository, times(1)).findById(bookingId);
        verify(roomDashboardService, times(1)).getUpdatedStateForRoom(1L);
        verify(roomDashboardService, times(1)).getUpdatedStateForRoom(2L);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/dashboard-updates"), any(DashboardRoomDto.class));
    }

    @Test
    @DisplayName("handleBookingStateChange should do nothing when booking is not found")
    void handleBookingStateChange_whenBookingNotFound_doesNothing() {
        // Given
        Long bookingId = 999L;
        BookingStateChangedEvent event = new BookingStateChangedEvent(bookingId, BookingStatus.CANCELLED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // When
        dashboardEventListener.handleBookingStateChange(event);

        // Then
        verify(bookingRepository, times(1)).findById(bookingId);
        verify(roomDashboardService, never()).getUpdatedStateForRoom(anyLong());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(DashboardRoomDto.class));
    }

    @Test
    @DisplayName("handleMaintenanceStateChange should broadcast dashboard update and maintenance update when event is received")
    void handleMaintenanceStateChange_whenEventReceived_broadcastsDashboardAndMaintenanceUpdate() {
        // Given
        Long roomId = 3L;
        MaintenanceTicketChangedEvent event = new MaintenanceTicketChangedEvent(roomId);

        DashboardRoomDto mockDashboardDto =
                DashboardRoomDto.builder().roomNumber("103").roomId(roomId).status(RoomStatus.OUT_OF_ORDER).build();

        MaintenanceTicketDto ticket1 = MaintenanceTicketDto.builder()
                .id(1L)
                .title("Broken AC")
                .roomId(roomId)
                .roomNumber("103")
                .priority(Priority.HIGH)
                .status(TicketStatus.OPEN)
                .build();

        MaintenanceTicketDto ticket2 = MaintenanceTicketDto.builder()
                .id(2L)
                .title("Leaky faucet")
                .roomId(4L)
                .roomNumber("104")
                .priority(Priority.MEDIUM)
                .status(TicketStatus.IN_PROGRESS)
                .build();

        List<MaintenanceTicketDto> allTickets = List.of(ticket1, ticket2);

        when(roomDashboardService.getUpdatedStateForRoom(roomId)).thenReturn(mockDashboardDto);
        when(maintenanceService.getAllMaintenanceTickets()).thenReturn(allTickets);

        ArgumentCaptor<DashboardRoomDto> dashboardDtoCaptor = ArgumentCaptor.forClass(DashboardRoomDto.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MaintenanceTicketDto>> ticketListCaptor = ArgumentCaptor.forClass(List.class);

        // When
        dashboardEventListener.handleMaintenanceStateChange(event);

        // Then
        verify(roomDashboardService, times(1)).getUpdatedStateForRoom(roomId);
        verify(maintenanceService, times(1)).getAllMaintenanceTickets();
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/dashboard-updates"),
                dashboardDtoCaptor.capture());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/maintenance-updates"),
                ticketListCaptor.capture());

        assertAll(() -> assertThat(dashboardDtoCaptor.getValue().getRoomNumber()).isEqualTo("103"),
                () -> assertThat(dashboardDtoCaptor.getValue().getRoomId()).isEqualTo(roomId),
                () -> assertThat(dashboardDtoCaptor.getValue().getStatus()).isEqualTo(RoomStatus.OUT_OF_ORDER),
                () -> assertThat(ticketListCaptor.getValue()).hasSize(2),
                () -> assertThat(ticketListCaptor.getValue().get(0).getTitle()).isEqualTo("Broken AC"),
                () -> assertThat(ticketListCaptor.getValue().get(1).getTitle()).isEqualTo("Leaky faucet"));
    }

    @Test
    @DisplayName("handleMaintenanceStateChange should broadcast empty ticket list when no tickets exist")
    void handleMaintenanceStateChange_whenNoTicketsExist_broadcastsEmptyList() {
        // Given
        Long roomId = 5L;
        MaintenanceTicketChangedEvent event = new MaintenanceTicketChangedEvent(roomId);

        DashboardRoomDto mockDashboardDto =
                DashboardRoomDto.builder().roomNumber("105").roomId(roomId).status(RoomStatus.AVAILABLE).build();

        when(roomDashboardService.getUpdatedStateForRoom(roomId)).thenReturn(mockDashboardDto);
        when(maintenanceService.getAllMaintenanceTickets()).thenReturn(List.of());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MaintenanceTicketDto>> ticketListCaptor = ArgumentCaptor.forClass(List.class);

        // When
        dashboardEventListener.handleMaintenanceStateChange(event);

        // Then
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/maintenance-updates"),
                ticketListCaptor.capture());
        assertThat(ticketListCaptor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("CleaningTaskUpdatedEvent should be created with correct room ID and status")
    void cleaningTaskUpdatedEvent_whenCreated_hasCorrectValues() {
        // Given
        Long roomId = 5L;
        RoomStatus status = RoomStatus.CLEANING;

        // When
        CleaningTaskUpdatedEvent event = new CleaningTaskUpdatedEvent(roomId, status);

        // Then
        assertAll(() -> assertThat(event.roomId()).isEqualTo(roomId),
                () -> assertThat(event.newRoomStatus()).isEqualTo(status));
    }

    @Test
    @DisplayName("CleaningTaskUpdatedEvent should support different room statuses")
    void cleaningTaskUpdatedEvent_whenCreatedWithDifferentStatuses_preservesStatus() {
        // Test with DIRTY status
        CleaningTaskUpdatedEvent dirtyEvent = new CleaningTaskUpdatedEvent(1L, RoomStatus.DIRTY);
        assertThat(dirtyEvent.newRoomStatus()).isEqualTo(RoomStatus.DIRTY);

        // Test with CLEANING status
        CleaningTaskUpdatedEvent cleaningEvent = new CleaningTaskUpdatedEvent(2L, RoomStatus.CLEANING);
        assertThat(cleaningEvent.newRoomStatus()).isEqualTo(RoomStatus.CLEANING);

        // Test with OUT_OF_ORDER status
        CleaningTaskUpdatedEvent outOfOrderEvent = new CleaningTaskUpdatedEvent(3L, RoomStatus.OUT_OF_ORDER);
        assertThat(outOfOrderEvent.newRoomStatus()).isEqualTo(RoomStatus.OUT_OF_ORDER);
    }
}
