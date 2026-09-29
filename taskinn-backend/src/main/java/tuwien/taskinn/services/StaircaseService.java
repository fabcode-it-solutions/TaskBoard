package tuwien.taskinn.services;

import java.util.List;
import tuwien.taskinn.dto.request.StaircaseRequestDto;
import tuwien.taskinn.dto.response.StaircaseDto;

public interface StaircaseService {

    /**
     * Get all active staircases.
     *
     * @return list of all active staircases
     */
    List<StaircaseDto> getAllStaircases();

    /**
     * Get all active staircases on a specific floor.
     *
     * @param floor the floor number
     * @return list of staircases on the floor
     */
    List<StaircaseDto> getStaircasesByFloor(Integer floor);

    /**
     * Get a staircase by its ID.
     *
     * @param id the staircase ID
     * @return the staircase DTO
     */
    StaircaseDto getStaircaseById(Long id);

    /**
     * Create a new staircase.
     *
     * @param requestDto the staircase data
     * @return the created staircase DTO
     */
    StaircaseDto createStaircase(StaircaseRequestDto requestDto);

    /**
     * Update an existing staircase.
     *
     * @param id the staircase ID
     * @param requestDto the updated staircase data
     * @return the updated staircase DTO
     */
    StaircaseDto updateStaircase(Long id, StaircaseRequestDto requestDto);

    /**
     * Delete a staircase (soft delete by setting active to false).
     *
     * @param id the staircase ID
     */
    void deleteStaircase(Long id);
}
