package tuwien.taskinn.services.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.request.RoomTypeRequestDto;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.repo.RoomTypeRepository;

@Component
@RequiredArgsConstructor
public class RoomTypeValidator {
    private final RoomTypeRepository roomTypeRepository;

    public void validateRoomTypeCreation(RoomTypeRequestDto roomTypeRequestDto) {
        if (roomTypeRepository.existsByName(roomTypeRequestDto.getName())) {
            throw new BusinessValidationException("name",
                    "Room type with name '" + roomTypeRequestDto.getName() + "' already exists");
        }

        if (roomTypeRequestDto.getCapacity() != roomTypeRequestDto.getSingleBed()
                + 2 * roomTypeRequestDto.getDoubleBed()) {
            throw new BusinessValidationException("capacity", "Capacity must equal the sum of single and double beds.");
        }
    }

    public void validateRoomTypeUpdate(RoomTypeRequestDto dto, RoomType roomType) {
        if (!roomType.getName().equals(dto.getName()) && roomTypeRepository.existsByName(dto.getName())) {
            throw new BusinessValidationException("name", "Room type with name '" + dto.getName() + "' already exists");
        }

        if (dto.getCapacity() != dto.getSingleBed() + 2 * dto.getDoubleBed()) {
            throw new BusinessValidationException("capacity", "Capacity must equal the sum of single and double beds.");
        }
    }
}
