package tuwien.taskinn.services;

import tuwien.taskinn.dto.request.RoomRequestDto;
import tuwien.taskinn.dto.response.FloorOccupancyDto;
import tuwien.taskinn.dto.response.LightRoomDto;
import tuwien.taskinn.dto.response.RoomDto;

import java.time.LocalDate;
import java.util.List;

public interface RoomService {

    /**
     * Get all rooms in light representation
     *
     * @param includeInactive whether to include inactive rooms in the results
     * @return list of LightRoomDto
     */
    List<LightRoomDto> getAllRooms(boolean includeInactive);

    /**
     * Create a new room
     *
     * @param requestDto the room request data
     * @return the created RoomDto, including a generated id
     */
    RoomDto createRoom(RoomRequestDto requestDto);

    /**
     * Update an existing room
     *
     * @param requestDto the room request data
     * @return the updated RoomDto
     */
    RoomDto updateRoom(Long id, RoomRequestDto requestDto);

    /**
     * Get a room by its identifier
     *
     * @param id the identifier of the room to retrieve
     * @return the RoomDto corresponding to the given id
     */
    RoomDto getRoomById(Long id);

    /**
     * Get rooms by floor number
     *
     * @param floor the floor number
     * @return list of RoomDto on the specified floor
     */
    List<RoomDto> findAllByFloorAndActiveTrue(Integer floor);

    /**
     * Disable a room by its ID, once a room is disabled, it cannot be booked anymore
     *
     * @param roomId the ID of the room to disable
     */
    void deactivateRoomPermanently(Long roomId);

    /**
     * Get the maximum floor number among all rooms
     *
     * @return the highest floor number
     */
    Integer getMaxFloor();

    /**
     * Get all active and available rooms for a specific date range. Used for manual booking.
     *
     * @param checkInDate the check-in date
     * @param checkOutDate the check-out date
     * @return list of active and available LightRoomDto
     */
    List<LightRoomDto> getAvailableActiveRoomsByDateRange(LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * Copy all rooms from one floor to another floor.
     *
     * @param fromFloor the source floor number
     * @param toFloor the destination floor number
     * @return list of RoomDto that were copied to the new floor
     */
    List<RoomDto> copyFloor(Integer fromFloor, Integer toFloor);

    /**
     * Get today's occupancy rate as a percentage
     *
     * @return occupancy rate (0-100)
     */
    Double getTodaysOccupancyRate();

    /**
     * Get occupancy distribution across all floors
     *
     * @return list of FloorOccupancyDto containing floor-by-floor occupancy statistics
     */
    List<FloorOccupancyDto> getFloorOccupancyDistribution();

    /**
     * Get all rooms on a floor, including inactive rooms. Used for data export.
     *
     * @param floor the floor number
     * @return list of all RoomDto on the specified floor
     */
    List<RoomDto> findAllByFloor(Integer floor);
}
