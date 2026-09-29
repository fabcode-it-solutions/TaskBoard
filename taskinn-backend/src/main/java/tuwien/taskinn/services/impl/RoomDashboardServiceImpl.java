package tuwien.taskinn.services.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tuwien.taskinn.dto.PointDto;
import tuwien.taskinn.dto.response.DashboardRoomDto;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.TicketStatus;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.MaintenanceTicketRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.services.RoomDashboardService;

@Service
@RequiredArgsConstructor
public class RoomDashboardServiceImpl implements RoomDashboardService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DashboardRoomDto> getCurrentRoomStates() {
        List<Room> allActiveRooms = roomRepository.findAllActiveWithDetails();

        List<Booking> currentBookings = bookingRepository.findAllCurrentBookings(LocalDate.now());

        Set<Long> roomsWithOpenTickets = maintenanceTicketRepository
                .findRoomIdsWithOpenTickets(List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS));

        Map<Long, Booking> roomToBookingMap = currentBookings.stream()
                .flatMap(booking -> booking.getRooms().stream().map(room -> Map.entry(room.getId(), booking)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (b1, b2) -> b1));

        return allActiveRooms.stream()
                .map(room -> buildDashboardRoomDto(room, roomToBookingMap.get(room.getId()), roomsWithOpenTickets))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardRoomDto getUpdatedStateForRoom(Long roomId) {
        Room room = roomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + roomId));

        Booking currentBooking = bookingRepository.findCurrentBookingForRoom(room, LocalDate.now()).orElse(null);

        boolean hasOpenTicket = maintenanceTicketRepository.existsByRoomAndStatusIn(room,
                List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS));

        return buildDashboardRoomDto(room, currentBooking, hasOpenTicket ? Set.of(roomId) : Set.of());
    }

    private DashboardRoomDto buildDashboardRoomDto(Room room, Booking booking, Set<Long> roomsWithOpenTickets) {
        String guestName = (booking != null && booking.getGuest() != null)
                ? booking.getGuest().getFirstName() + " " + booking.getGuest().getLastName()
                : null;

        return DashboardRoomDto.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .status(room.getStatus())
                .guestName(guestName)
                .hasOpenMaintenanceTicket(roomsWithOpenTickets.contains(room.getId()))
                .floorNumber(room.getFloor())
                .svgPoints(room.getSvgPoints()
                        .stream()
                        .map(point -> PointDto.builder().x(point.getX()).y(point.getY()).build())
                        .toList())
                .build();
    }
}
