package tuwien.taskinn.mapper;

import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.request.RoomTypeRequestDto;
import tuwien.taskinn.dto.response.RoomTypeDto;
import tuwien.taskinn.entity.RoomType;

@Component
public class RoomTypeMapper {

    public RoomTypeDto toDto(RoomType roomType) {
        if (roomType == null) {
            return null;
        }

        return RoomTypeDto.builder()
                .id(roomType.getId())
                .name(roomType.getName())
                .description(roomType.getDescription())
                .capacity(roomType.getCapacity())
                .basePrice(roomType.getBasePrice())
                .singleBed(roomType.getSingleBed())
                .doubleBed(roomType.getDoubleBed())
                .build();
    }

    public RoomType toEntity(RoomTypeRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return RoomType.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .capacity(dto.getCapacity())
                .basePrice(dto.getBasePrice())
                .singleBed(dto.getSingleBed())
                .doubleBed(dto.getDoubleBed())
                .build();
    }

    public void updateEntity(RoomType roomType, RoomTypeRequestDto dto) {
        if (roomType == null || dto == null) {
            return;
        }

        roomType.setName(dto.getName());
        roomType.setDescription(dto.getDescription());
        roomType.setCapacity(dto.getCapacity());
        roomType.setBasePrice(dto.getBasePrice());
        roomType.setSingleBed(dto.getSingleBed());
        roomType.setDoubleBed(dto.getDoubleBed());
    }
}
