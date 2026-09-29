package tuwien.taskinn.mapper;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import tuwien.taskinn.dto.response.LightRoomDto;
import tuwien.taskinn.dto.response.RoomOptionDto;
import tuwien.taskinn.dto.PointDto;
import tuwien.taskinn.dto.request.RoomRequestDto;
import tuwien.taskinn.dto.response.RoomDto;
import tuwien.taskinn.entity.Point;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Room entities and DTOs.
 */
@Component
@RequiredArgsConstructor
public class RoomMapper {

    private final RoomTypeMapper roomTypeMapper;

    public RoomOptionDto toRoomOptionDto(Room room) {
        if (room == null) {
            return null;
        }

        return RoomOptionDto.builder().roomId(room.getId()).roomType(roomTypeMapper.toDto(room.getRoomType())).build();
    }

    /**
     * Converts a PointDto to a Point entity.
     *
     * @param pointDto the point DTO to convert
     * @return the converted Point entity
     */
    public Point toPointEntity(PointDto pointDto) {
        if (pointDto == null) {
            return null;
        }
        return new Point(pointDto.getX(), pointDto.getY());
    }

    /**
     * Converts a Point entity to a PointDto.
     *
     * @param point the point entity to convert
     * @return the converted PointDto
     */
    public PointDto toPointDto(Point point) {
        if (point == null) {
            return null;
        }
        return new PointDto(point.getX(), point.getY());
    }

    /**
     * Converts a list of PointDto to a list of Point entities.
     *
     * @param pointDtos the list of point DTOs to convert
     * @return the converted list of Point entities
     */
    public List<Point> toPointEntityList(List<PointDto> pointDtos) {
        if (pointDtos == null) {
            return null;
        }
        return pointDtos.stream().map(this::toPointEntity).collect(Collectors.toList());
    }

    /**
     * Converts a list of Point entities to a list of PointDto.
     *
     * @param points the list of point entities to convert
     * @return the converted list of PointDto
     */
    public List<PointDto> toPointDtoList(List<Point> points) {
        if (points == null) {
            return null;
        }
        return points.stream().map(this::toPointDto).collect(Collectors.toList());
    }

    /**
     * Converts a RoomRequestDto to a Room entity.
     *
     * @param requestDto the room request DTO
     * @param roomType the room type entity
     * @return the converted Room entity
     */
    public Room toEntity(RoomRequestDto requestDto, RoomType roomType) {
        if (requestDto == null) {
            return null;
        }

        return Room.builder()
                .id(requestDto.getId())
                .roomNumber(requestDto.getRoomNumber())
                .floor(requestDto.getFloor())
                .status(requestDto.getStatus())
                .svgPoints(toPointEntityList(requestDto.getSvgPoints()))
                .active(true)
                .roomType(roomType)
                .capacityOverride(requestDto.getCapacityOverride())
                .basePriceOverride(requestDto.getBasePriceOverride())
                .singleBedOverride(requestDto.getSingleBedOverride())
                .doubleBedOverride(requestDto.getDoubleBedOverride())
                .build();
    }

    /**
     * Converts a Room entity to a RoomDto.
     *
     * @param room the room entity to convert
     * @return the converted RoomDto
     */
    public RoomDto toDto(Room room) {
        if (room == null) {
            return null;
        }

        return RoomDto.builder()
                .id(room.getId())
                .number(room.getRoomNumber())
                .floor(room.getFloor())
                .status(room.getStatus())
                .svgPoints(toPointDtoList(room.getSvgPoints()))
                .active(room.isActive())
                .roomTypeId(room.getRoomType().getId())
                .capacityOverride(room.getCapacityOverride())
                .basePriceOverride(room.getBasePriceOverride())
                .singleBedOverride(room.getSingleBedOverride())
                .doubleBedOverride(room.getDoubleBedOverride())
                .build();
    }

    /**
     * Converts a Room entity to a LightRoomDto with effective values computed from overrides or RoomType.
     *
     * @param room the room entity to convert
     * @return the converted LightRoomDto with effective values
     */
    public LightRoomDto toLightDto(Room room) {
        if (room == null) {
            return null;
        }

        RoomType roomType = room.getRoomType();
        LightRoomDto.RoomTypeInfo roomTypeInfo = null;
        if (roomType != null) {
            roomTypeInfo = LightRoomDto.RoomTypeInfo.builder()
                    .id(roomType.getId())
                    .name(roomType.getName())
                    .capacity(roomType.getCapacity())
                    .basePrice(roomType.getBasePrice())
                    .singleBed(roomType.getSingleBed())
                    .doubleBed(roomType.getDoubleBed())
                    .build();
        }

        return LightRoomDto.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .floor(room.getFloor())
                .active(room.isActive())
                .status(room.getStatus())
                .roomType(roomTypeInfo)
                .effectiveCapacity(room.getEffectiveCapacity())
                .effectiveBasePrice(room.getEffectiveBasePrice())
                .effectiveSingleBed(room.getEffectiveSingleBed())
                .effectiveDoubleBed(room.getEffectiveDoubleBed())
                .build();
    }
}
