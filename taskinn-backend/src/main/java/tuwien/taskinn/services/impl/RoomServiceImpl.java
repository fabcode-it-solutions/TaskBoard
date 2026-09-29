package tuwien.taskinn.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tuwien.taskinn.dto.request.RoomRequestDto;
import tuwien.taskinn.dto.response.FloorOccupancyDto;
import tuwien.taskinn.dto.response.LightRoomDto;
import tuwien.taskinn.dto.response.RoomDto;
import tuwien.taskinn.entity.Point;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.mapper.RoomMapper;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.MaintenanceTicket;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.enums.TicketStatus;
import tuwien.taskinn.exceptions.RoomConflictException;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.MaintenanceTicketRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.repo.RoomTypeRepository;
import tuwien.taskinn.services.RoomService;
import tuwien.taskinn.services.validator.RoomValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomMapper roomMapper;
    private final RoomValidator roomValidator;

    private final BookingRepository bookingRepository;

    private final MaintenanceTicketRepository maintenanceTicketRepository;

    @Override
    public List<LightRoomDto> getAllRooms(boolean includeInactive) {
        return roomRepository.findAll()
                .stream()
                .filter(room -> includeInactive || room.isActive())
                .map(roomMapper::toLightDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void deactivateRoomPermanently(Long roomId) {
        Room roomToDisable = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Room with ID " + roomId + " does not exist."));

        if (!roomToDisable.isActive()) {
            return; // Room is already disabled, no action needed
        }

        // Check for any booking containing this room with a check-out date in the future
        // If yes, try to assign another room of the same type, else throw exception
        List<Booking> futureBookings = bookingRepository.findFutureBookingsForRoom(roomToDisable, LocalDate.now());
        if (!futureBookings.isEmpty()) {
            for (Booking booking : futureBookings) {

                if (booking.getStatus().equals(BookingStatus.CANCELLED)
                        || booking.getStatus().equals(BookingStatus.CHECKED_OUT)) {
                    continue;
                }

                // A room cannot be disabled if there is an active booking currently checked-in
                if (booking.getStatus().equals(BookingStatus.CHECKED_IN)) {
                    throw new RoomConflictException("Cannot disable room. Booking #" + booking.getId()
                            + " is currently in status '" + booking.getStatus() + "'.");
                }

                // Try to find a replacement room of the same type
                Optional<Room> replacementRoom =
                        roomRepository
                                .findActiveAvailableRooms(roomToDisable.getRoomType(), booking.getCheckInDate(),
                                        booking.getCheckOutDate(), roomToDisable.getId())
                                .stream()
                                .findFirst();

                // If found, reallocate the booking to the new room
                if (replacementRoom.isPresent()) {
                    Room newRoom = replacementRoom.get();
                    booking.getRooms().remove(roomToDisable);
                    booking.getRooms().add(newRoom);
                    bookingRepository.save(booking);
                    log.info("Successfully reallocated Booking #{} from Room #{} to Room #{}", booking.getId(),
                            roomToDisable.getId(), newRoom.getId());
                } else {
                    throw new RoomConflictException("Cannot disable room. No alternative room of type '"
                            + roomToDisable.getRoomType().getName() + "' is available for booking #" + booking.getId()
                            + " (" + booking.getCheckInDate() + " to " + booking.getCheckOutDate() + ").");
                }
            }
        }

        // Auto close all maintenance tickets with this room
        List<MaintenanceTicket> openTickets = maintenanceTicketRepository.findByRoomId(roomId);
        openTickets.forEach(ticket -> {
            ticket.setStatus(TicketStatus.CLOSED);
            ticket.setDescription(ticket.getDescription() + "\n\n[Auto-Closed] Room disabled.");
        });
        maintenanceTicketRepository.saveAll(openTickets); // More efficient

        roomToDisable.setActive(false);
        roomRepository.save(roomToDisable);
    }

    @Override
    @Transactional
    public RoomDto createRoom(RoomRequestDto requestDto) {

        RoomType roomType = roomTypeRepository.findById(requestDto.getRoomTypeId())
                .orElseThrow(() -> new NoSuchElementException("RoomType not found: " + requestDto.getRoomTypeId()));

        Room room = roomMapper.toEntity(requestDto, roomType);
        roomValidator.validateRoomNumberFloorPrefix(room.getRoomNumber(), room.getFloor());
        roomValidator.validateRoomNumberUniqueness(null, room.getRoomNumber());
        roomValidator.validateOverlap(null, room.getFloor(), room.getSvgPoints());
        Room savedRoom = roomRepository.save(room);

        return roomMapper.toDto(savedRoom);
    }

    @Override
    @Transactional
    public RoomDto updateRoom(Long id, RoomRequestDto requestDto) {
        Room existingRoom =
                roomRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Room not found: " + id));

        if (!id.equals(requestDto.getId())) {
            throw new IllegalArgumentException("ID in path and request body do not match");
        }

        roomValidator.validateRoomNumberFloorPrefix(requestDto.getRoomNumber(), requestDto.getFloor());
        roomValidator.validateRoomNumberUniqueness(id, requestDto.getRoomNumber());
        List<Point> svgPoints = roomMapper.toPointEntityList(requestDto.getSvgPoints());
        roomValidator.validateOverlap(id, requestDto.getFloor(), svgPoints);

        RoomType roomType = roomTypeRepository.findById(requestDto.getRoomTypeId())
                .orElseThrow(() -> new NoSuchElementException("RoomType not found: " + requestDto.getRoomTypeId()));
        roomValidator.validateRoomUpdate(requestDto, roomType);

        existingRoom.setRoomNumber(requestDto.getRoomNumber());
        existingRoom.setFloor(requestDto.getFloor());
        existingRoom.setRoomType(roomType);
        existingRoom.setStatus(requestDto.getStatus());
        existingRoom.setSvgPoints(roomMapper.toPointEntityList(requestDto.getSvgPoints()));
        existingRoom.setActive(requestDto.isActive());
        existingRoom.setCapacityOverride(requestDto.getCapacityOverride());
        existingRoom.setBasePriceOverride(requestDto.getBasePriceOverride());
        existingRoom.setSingleBedOverride(requestDto.getSingleBedOverride());
        existingRoom.setDoubleBedOverride(requestDto.getDoubleBedOverride());

        Room savedRoom = roomRepository.save(existingRoom);
        return roomMapper.toDto(savedRoom);
    }

    @Override
    public RoomDto getRoomById(Long id) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Room not found: " + id));
        return roomMapper.toDto(room);
    }

    @Override
    public List<RoomDto> findAllByFloorAndActiveTrue(Integer floor) {
        List<Room> rooms = roomRepository.findAllByFloorAndActiveTrue(floor);
        return rooms.stream().map(roomMapper::toDto).toList();
    }

    @Override
    public Integer getMaxFloor() {
        return roomRepository.getMaxFloor();
    }

    @Override
    public List<LightRoomDto> getAvailableActiveRoomsByDateRange(LocalDate checkInDate, LocalDate checkOutDate) {
        roomValidator.validateDateRange(checkInDate, checkOutDate);
        return roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate)
                .stream()
                .map(roomMapper::toLightDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<RoomDto> copyFloor(Integer fromFloor, Integer toFloor) {
        List<Room> sourceRooms = roomRepository.findAllByFloorAndActiveTrue(fromFloor);

        if (sourceRooms.isEmpty()) {
            return List.of();
        }

        List<Room> copiedRooms = sourceRooms.stream().map(sourceRoom -> {
            String newRoomNumber = generateRoomNumberForFloor(sourceRoom.getRoomNumber(), fromFloor, toFloor);

            List<Point> copiedPoints = sourceRoom.getSvgPoints()
                    .stream()
                    .map(point -> new Point(point.getX(), point.getY()))
                    .collect(Collectors.toList());

            return Room.builder()
                    .roomNumber(newRoomNumber)
                    .floor(toFloor)
                    .status(RoomStatus.AVAILABLE)
                    .active(true)
                    .svgPoints(copiedPoints)
                    .roomType(sourceRoom.getRoomType())
                    .build();
        }).collect(Collectors.toList());

        for (Room room : copiedRooms) {
            roomValidator.validateRoomNumberFloorPrefix(room.getRoomNumber(), room.getFloor());
            roomValidator.validateRoomNumberUniqueness(null, room.getRoomNumber());
        }

        List<Room> savedRooms = roomRepository.saveAll(copiedRooms);

        return savedRooms.stream().map(roomMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public Double getTodaysOccupancyRate() {
        LocalDate today = LocalDate.now();
        Long totalActiveRooms = roomRepository.countActiveRooms();

        if (totalActiveRooms == 0) {
            return 0.0;
        }

        Long occupiedRooms = roomRepository.countOccupiedRoomsOnDate(today);

        return (occupiedRooms.doubleValue() / totalActiveRooms.doubleValue()) * 100.0;
    }

    @Override
    public List<FloorOccupancyDto> getFloorOccupancyDistribution() {
        LocalDate today = LocalDate.now();
        List<Integer> floors = roomRepository.findDistinctFloors();

        return floors.stream().map(floor -> {
            Long totalRooms = roomRepository.countActiveRoomsByFloor(floor);
            Long occupiedRooms = roomRepository.countOccupiedRoomsByFloorOnDate(floor, today);

            double occupancyRate = 0.0;
            if (totalRooms > 0) {
                occupancyRate = (occupiedRooms.doubleValue() / totalRooms.doubleValue()) * 100.0;
            }

            return FloorOccupancyDto.builder()
                    .floor(floor)
                    .totalRooms(totalRooms)
                    .occupiedRooms(occupiedRooms)
                    .occupancyRate(occupancyRate)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<RoomDto> findAllByFloor(Integer floor) {
        List<Room> rooms = roomRepository.findAllByFloor(floor);
        return rooms.stream().map(roomMapper::toDto).toList();
    }

    /**
     * Generate a new room number for a copied room by replacing the floor prefix while keeping everything after the
     * floor number unchanged. For example, copying room "100001" from floor 1 to floor 2 results in "200001".
     *
     * @param originalRoomNumber the original room number
     * @param fromFloor the source floor number
     * @param toFloor the target floor number
     * @return the new room number for the target floor
     */
    private String generateRoomNumberForFloor(String originalRoomNumber, Integer fromFloor, Integer toFloor) {
        String fromFloorStr = String.valueOf(fromFloor);
        String toFloorStr = String.valueOf(toFloor);

        String roomSuffix;
        if (originalRoomNumber.startsWith(fromFloorStr)) {
            roomSuffix = originalRoomNumber.substring(fromFloorStr.length());
        } else {
            roomSuffix = originalRoomNumber.replaceFirst("^\\d+", "");
        }

        return toFloorStr + roomSuffix;
    }
}
