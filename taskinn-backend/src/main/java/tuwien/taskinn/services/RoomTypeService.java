package tuwien.taskinn.services;

import tuwien.taskinn.dto.request.RoomTypeRequestDto;
import tuwien.taskinn.dto.response.LiteRoomTypeDto;
import tuwien.taskinn.dto.response.RoomDto;
import tuwien.taskinn.dto.response.RoomTypeDto;

import java.util.List;

public interface RoomTypeService {

    /**
     * Get all room types in lite representation
     *
     * @return list of LiteRoomTypeDto
     */
    List<LiteRoomTypeDto> getAllRoomTypes();

    /**
     * Get a room type by ID
     *
     * @param id the room type ID
     * @return the room type DTO
     */
    RoomTypeDto getRoomTypeById(Long id);

    /**
     * Create a new room type
     *
     * @param dto the creation DTO
     * @return the created room type
     */
    RoomTypeDto createRoomType(RoomTypeRequestDto dto);

    /**
     * Update an existing room type
     *
     * @param id the room type ID
     * @param dto the update DTO
     * @return the updated room type
     */
    RoomTypeDto updateRoomType(Long id, RoomTypeRequestDto dto);

    /**
     * Get all rooms that use a specific room type
     *
     * @param id the room type ID
     * @return list of rooms using the room type
     */
    List<RoomDto> getRoomsByRoomTypeId(Long id);

    /**
     * Delete a room type
     *
     * @param id the room type ID
     * @throws tuwien.taskinn.exceptions.BusinessValidationException if rooms are using this room type
     */
    void deleteRoomType(Long id);
}
