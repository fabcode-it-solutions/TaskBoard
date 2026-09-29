package tuwien.taskinn.unittest.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tuwien.taskinn.dto.PointDto;
import tuwien.taskinn.dto.request.RoomRequestDto;
import tuwien.taskinn.dto.response.RoomDto;
import tuwien.taskinn.dto.response.RoomOptionDto;
import tuwien.taskinn.dto.response.RoomTypeDto;
import tuwien.taskinn.entity.Point;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.mapper.RoomMapper;
import tuwien.taskinn.mapper.RoomTypeMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomMapper Unit Tests")
public class RoomMapperTest {

    @InjectMocks
    private RoomMapper roomMapper;

    @Mock
    private RoomTypeMapper roomTypeMapper;

    private final PointDto POINT_DTO = new PointDto(10.5, 20.5);

    private final List<PointDto> POINT_DTO_LIST = List.of(new PointDto(1.0, 1.0), new PointDto(2.0, 2.0));
    private final List<Point> POINT_ENTITY_LIST = List.of(new Point(1.0, 1.0), new Point(2.0, 2.0));

    private final Long roomTypeId = 1L;
    private final RoomType roomType = RoomType.builder()
            .id(roomTypeId)
            .name("Deluxe Suite")
            .description("Large suite with balcony.")
            .capacity(4)
            .basePrice(new BigDecimal("180.00"))
            .singleBed(0)
            .doubleBed(2)
            .build();

    @Test
    @DisplayName("toPointEntity should correctly map PointDto to Point entity")
    public void toPointEntity_whenValidDto_correctlyMapsToEntity() {
        Point result = roomMapper.toPointEntity(POINT_DTO);

        assertAll("Point Entity Mapping", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getX()).isEqualTo(10.5), () -> assertThat(result.getY()).isEqualTo(20.5));
    }

    @Test
    @DisplayName("toPointEntity should return null when PointDto is null")
    public void toPointEntity_whenDtoIsNull_returnsNull() {
        Point result = roomMapper.toPointEntity(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toPointEntityList should map list of PointDto to list of Point entities")
    public void toPointEntityList_whenValidDtoList_correctlyMapsToEntityList() {
        List<Point> result = roomMapper.toPointEntityList(POINT_DTO_LIST);

        assertAll("Point Entity List Mapping", () -> assertThat(result).isNotNull().hasSize(2),
                () -> assertThat(result.get(0).getX()).isEqualTo(1.0),
                () -> assertThat(result.get(1).getY()).isEqualTo(2.0));
    }

    @Test
    @DisplayName("toPointDtoList should map list of Point entities to list of PointDto")
    public void toPointDtoList_whenValidEntityList_correctlyMapsToDtoList() {
        List<PointDto> result = roomMapper.toPointDtoList(POINT_ENTITY_LIST);

        assertAll("Point DTO List Mapping", () -> assertThat(result).isNotNull().hasSize(2),
                () -> assertThat(result.get(0).getX()).isEqualTo(1.0),
                () -> assertThat(result.get(1).getY()).isEqualTo(2.0));
    }

    @Test
    @DisplayName("toEntity should correctly map RoomRequestDto and RoomType to Room entity")
    public void toEntity_whenValidDto_correctlyMapsToEntity() {
        RoomRequestDto requestDto = RoomRequestDto.builder()
                .id(5L)
                .roomNumber("201")
                .floor(2)
                .status(RoomStatus.AVAILABLE)
                .roomTypeId(roomTypeId)
                .svgPoints(POINT_DTO_LIST)
                .build();

        Room result = roomMapper.toEntity(requestDto, roomType);

        assertAll("Room Entity Mapping", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getId()).isEqualTo(5L),
                () -> assertThat(result.getRoomNumber()).isEqualTo("201"),
                () -> assertThat(result.getFloor()).isEqualTo(2),
                () -> assertThat(result.getStatus()).isEqualTo(RoomStatus.AVAILABLE),
                () -> assertThat(result.getRoomType().getId()).isEqualTo(roomTypeId),
                () -> assertThat(result.getSvgPoints()).hasSize(2));
    }

    @Test
    @DisplayName("toDto should correctly map Room entity to RoomDto")
    public void toDto_whenValidEntity_correctlyMapsToDto() {
        Room roomEntity = Room.builder()
                .id(15L)
                .roomNumber("305")
                .floor(3)
                .status(RoomStatus.AVAILABLE)
                .roomType(roomType)
                .svgPoints(POINT_ENTITY_LIST)
                .build();

        RoomDto result = roomMapper.toDto(roomEntity);

        assertAll("Room DTO Mapping", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getId()).isEqualTo(15L), () -> assertThat(result.getNumber()).isEqualTo("305"),
                () -> assertThat(result.getFloor()).isEqualTo(3),
                () -> assertThat(result.getStatus()).isEqualTo(RoomStatus.AVAILABLE),
                () -> assertThat(result.getRoomTypeId()).isEqualTo(roomType.getId()),
                () -> assertThat(result.getSvgPoints()).hasSize(2));
    }

    @Test
    @DisplayName("toRoomOptionDto should correctly map Room entity to RoomOptionDto")
    public void toRoomOptionDto_whenValidRoom_correctlyMapsToDto() {
        Room room = Room.builder()
                .id(10L)
                .roomNumber("101")
                .floor(1)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .roomType(roomType)
                .build();

        RoomTypeDto expectedRoomTypeDto = RoomTypeDto.builder()
                .id(roomTypeId)
                .name("Deluxe Suite")
                .description("Large suite with balcony.")
                .capacity(4)
                .basePrice(new BigDecimal("180.00"))
                .singleBed(0)
                .doubleBed(2)
                .build();

        when(roomTypeMapper.toDto(roomType)).thenReturn(expectedRoomTypeDto);

        RoomOptionDto result = roomMapper.toRoomOptionDto(room);

        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result.getRoomId()).isEqualTo(10L),
                () -> assertThat(result.getRoomType()).isNotNull(),
                () -> assertThat(result.getRoomType().getId()).isEqualTo(roomTypeId),
                () -> assertThat(result.getRoomType().getName()).isEqualTo("Deluxe Suite"),
                () -> assertThat(result.getRoomType().getDescription()).isEqualTo("Large suite with balcony."),
                () -> assertThat(result.getRoomType().getCapacity()).isEqualTo(4),
                () -> assertThat(result.getRoomType().getBasePrice()).usingComparator(BigDecimal::compareTo)
                        .isEqualTo(new BigDecimal("180.00")),
                () -> assertThat(result.getRoomType().getSingleBed()).isEqualTo(0),
                () -> assertThat(result.getRoomType().getDoubleBed()).isEqualTo(2));
    }

    @Test
    @DisplayName("toRoomOptionDto should return null when room is null")
    public void toRoomOptionDto_whenRoomIsNull_returnsNull() {
        RoomOptionDto result = roomMapper.toRoomOptionDto(null);

        assertThat(result).isNull();
    }
}
