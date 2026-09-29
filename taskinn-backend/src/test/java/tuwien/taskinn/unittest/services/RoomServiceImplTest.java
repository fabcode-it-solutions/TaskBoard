package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.MaintenanceTicket;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.enums.TicketStatus;
import tuwien.taskinn.exceptions.RoomConflictException;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.MaintenanceTicketRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.services.impl.RoomServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomServiceImpl Unit Tests")
public class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private MaintenanceTicketRepository maintenanceTicketRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    @Test
    @DisplayName("deactivateRoomPermanently should set active to false when no conflicts exist")
    void deactivateRoomPermanently_whenNoConflicts_setsActiveToFalse() {
        Long roomId = 1L;
        Room mockRoom = Room.builder().id(roomId).active(true).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
        when(bookingRepository.findFutureBookingsForRoom(any(Room.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(maintenanceTicketRepository.findByRoomId(roomId)).thenReturn(Collections.emptyList());

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);

        roomService.deactivateRoomPermanently(roomId);

        verify(roomRepository).save(roomCaptor.capture());
        Room savedRoom = roomCaptor.getValue();

        assertAll("Verify successful room deactivation", () -> assertThat(savedRoom.isActive()).isFalse(),
                () -> verify(roomRepository, times(1)).save(any(Room.class)));
    }

    @Test
    @DisplayName("deactivateRoomPermanently should reallocate bookings and succeed if replacements are found")
    void deactivateRoomPermanently_whenReplacementsExist_reallocatesAndSucceeds() {
        Long roomId = 1L;
        RoomType mockRoomType = RoomType.builder().id(10L).name("Standard").build();
        Room roomToDisable =
                Room.builder().id(roomId).active(true).roomType(mockRoomType).status(RoomStatus.AVAILABLE).build();
        Room replacementRoom =
                Room.builder().id(2L).active(true).roomType(mockRoomType).status(RoomStatus.AVAILABLE).build();

        Booking conflictingBooking = Booking.builder()
                .id(100L)
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(10))
                .rooms(new java.util.HashSet<>(Set.of(roomToDisable)))
                .status(BookingStatus.CONFIRMED)
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(roomToDisable));
        when(bookingRepository.findFutureBookingsForRoom(any(Room.class), any(LocalDate.class)))
                .thenReturn(List.of(conflictingBooking));
        when(roomRepository.findActiveAvailableRooms(any(), any(), any(), any())).thenReturn(List.of(replacementRoom));

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);

        roomService.deactivateRoomPermanently(roomId);

        assertAll("Verify successful reallocation and deactivation",
                () -> verify(bookingRepository, times(1)).save(bookingCaptor.capture()), () -> {
                    Booking savedBooking = bookingCaptor.getValue();
                    assertThat(savedBooking.getRooms()).contains(replacementRoom);
                    assertThat(savedBooking.getRooms()).doesNotContain(roomToDisable);
                }, () -> verify(roomRepository, times(1)).save(roomCaptor.capture()),
                () -> assertThat(roomCaptor.getValue().isActive()).isFalse());
    }

    @Test
    @DisplayName("deactivateRoomPermanently should throw RoomConflictException if no replacement is found")
    void deactivateRoomPermanently_whenNoReplacementExists_throwsRoomConflictException() {
        Long roomId = 1L;
        RoomType mockRoomType = RoomType.builder().id(10L).name("Standard").build();
        Room roomToDisable =
                Room.builder().id(roomId).active(true).roomType(mockRoomType).status(RoomStatus.AVAILABLE).build();
        Booking conflictingBooking =
                Booking.builder().id(100L).rooms(Set.of(roomToDisable)).status(BookingStatus.CONFIRMED).build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(roomToDisable));
        when(bookingRepository.findFutureBookingsForRoom(any(Room.class), any(LocalDate.class)))
                .thenReturn(List.of(conflictingBooking));
        when(roomRepository.findActiveAvailableRooms(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        assertThrows(RoomConflictException.class, () -> roomService.deactivateRoomPermanently(roomId));

        assertAll("Verify no data was changed on conflict", () -> verify(roomRepository, never()).save(any(Room.class)),
                () -> verify(bookingRepository, never()).save(any(Booking.class)));
    }

    @Test
    @DisplayName("deactivateRoomPermanently should throw IllegalArgumentException when room not found")
    void deactivateRoomPermanently_whenRoomNotFound_throwsIllegalArgumentException() {
        Long nonExistentRoomId = 999L;
        when(roomRepository.findById(nonExistentRoomId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> roomService.deactivateRoomPermanently(nonExistentRoomId));

        assertAll("Verify no repository interactions on not found",
                () -> verify(bookingRepository, never()).findFutureBookingsForRoom(any(), any()),
                () -> verify(roomRepository, never()).save(any(Room.class)));
    }

    @Test
    @DisplayName("deactivateRoomPermanently should find and close open maintenance tickets for the room")
    void deactivateRoomPermanently_whenOpenTicketsExist_closesThem() {
        Long roomId = 1L;
        Room mockRoom = Room.builder().id(roomId).active(true).build();

        MaintenanceTicket openTicket = MaintenanceTicket.builder()
                .id(101L)
                .title("Leaky Faucet")
                .status(TicketStatus.OPEN)
                .description("Original description.")
                .build();

        MaintenanceTicket inProgressTicket = MaintenanceTicket.builder()
                .id(102L)
                .title("Broken TV")
                .status(TicketStatus.IN_PROGRESS)
                .description("Remote is missing.")
                .build();

        List<MaintenanceTicket> tickets = List.of(openTicket, inProgressTicket);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
        when(bookingRepository.findFutureBookingsForRoom(any(Room.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(maintenanceTicketRepository.findByRoomId(roomId)).thenReturn(tickets);

        ArgumentCaptor<List<MaintenanceTicket>> ticketListCaptor = ArgumentCaptor.forClass(List.class);

        roomService.deactivateRoomPermanently(roomId);

        assertAll("Verify maintenance tickets are closed and saved",
                () -> verify(maintenanceTicketRepository, times(1)).saveAll(ticketListCaptor.capture()), () -> {
                    List<MaintenanceTicket> savedTickets = ticketListCaptor.getValue();
                    assertThat(savedTickets).hasSize(2);
                    assertThat(savedTickets).allMatch(ticket -> ticket.getStatus() == TicketStatus.CLOSED);
                    assertThat(savedTickets.get(0).getDescription()).endsWith("[Auto-Closed] Room disabled.");
                    assertThat(savedTickets.get(1).getDescription()).endsWith("[Auto-Closed] Room disabled.");
                }, () -> verify(roomRepository, times(1)).save(any(Room.class)));
    }
}
