package tuwien.taskinn.unittest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import tuwien.taskinn.dto.request.StaircaseRequestDto;
import tuwien.taskinn.dto.response.StaircaseDto;
import tuwien.taskinn.entity.Staircase;
import tuwien.taskinn.mapper.StaircaseMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("StaircaseMapper Unit Tests")
public class StaircaseMapperTest {

    @InjectMocks
    private StaircaseMapper staircaseMapper;

    @Test
    @DisplayName("toDto should correctly map Staircase entity to StaircaseDto")
    void toDto_whenValidEntity_correctlyMapsToDto() {
        Staircase staircase =
                Staircase.builder().id(1L).floor(2).x(100.0).y(200.0).name("Main Staircase").active(true).build();

        StaircaseDto result = staircaseMapper.toDto(staircase);

        assertAll("Staircase DTO Mapping", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getId()).isEqualTo(1L), () -> assertThat(result.getFloor()).isEqualTo(2),
                () -> assertThat(result.getX()).isEqualTo(100.0), () -> assertThat(result.getY()).isEqualTo(200.0),
                () -> assertThat(result.getName()).isEqualTo("Main Staircase"),
                () -> assertThat(result.isActive()).isTrue());
    }

    @Test
    @DisplayName("toDto should return null when entity is null")
    void toDto_whenEntityIsNull_returnsNull() {
        StaircaseDto result = staircaseMapper.toDto(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDtoList should correctly map list of entities to list of DTOs")
    void toDtoList_whenValidEntityList_correctlyMapsToDtoList() {
        Staircase staircase1 =
                Staircase.builder().id(1L).floor(1).x(50.0).y(100.0).name("Staircase A").active(true).build();

        Staircase staircase2 =
                Staircase.builder().id(2L).floor(2).x(150.0).y(200.0).name("Staircase B").active(true).build();

        List<StaircaseDto> result = staircaseMapper.toDtoList(List.of(staircase1, staircase2));

        assertAll("Staircase DTO List Mapping", () -> assertThat(result).isNotNull().hasSize(2),
                () -> assertThat(result.get(0).getId()).isEqualTo(1L),
                () -> assertThat(result.get(0).getName()).isEqualTo("Staircase A"),
                () -> assertThat(result.get(1).getId()).isEqualTo(2L),
                () -> assertThat(result.get(1).getName()).isEqualTo("Staircase B"));
    }

    @Test
    @DisplayName("toDtoList should return empty list when input list is empty")
    void toDtoList_whenEmptyList_returnsEmptyList() {
        List<StaircaseDto> result = staircaseMapper.toDtoList(Collections.emptyList());
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("toDtoList should return null when input list is null")
    void toDtoList_whenListIsNull_returnsNull() {
        List<StaircaseDto> result = staircaseMapper.toDtoList(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toEntity should correctly map StaircaseRequestDto to Staircase entity")
    void toEntity_whenValidDto_correctlyMapsToEntity() {
        StaircaseRequestDto requestDto = StaircaseRequestDto.builder()
                .id(5L)
                .floor(3)
                .x(75.0)
                .y(125.0)
                .name("Emergency Staircase")
                .active(true)
                .build();

        Staircase result = staircaseMapper.toEntity(requestDto);

        assertAll("Staircase Entity Mapping", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getId()).isEqualTo(5L), () -> assertThat(result.getFloor()).isEqualTo(3),
                () -> assertThat(result.getX()).isEqualTo(75.0), () -> assertThat(result.getY()).isEqualTo(125.0),
                () -> assertThat(result.getName()).isEqualTo("Emergency Staircase"),
                () -> assertThat(result.isActive()).isTrue());
    }

    @Test
    @DisplayName("toEntity should return null when request DTO is null")
    void toEntity_whenDtoIsNull_returnsNull() {
        Staircase result = staircaseMapper.toEntity(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toEntity should default active to true when active field is null")
    void toEntity_whenActiveIsNull_defaultsToTrue() {
        StaircaseRequestDto requestDto = StaircaseRequestDto.builder()
                .id(6L)
                .floor(4)
                .x(80.0)
                .y(130.0)
                .name("Side Staircase")
                .active(null)
                .build();

        Staircase result = staircaseMapper.toEntity(requestDto);

        assertAll("Staircase Entity with null active", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.isActive()).isTrue());
    }

    @Test
    @DisplayName("updateEntity should correctly update Staircase entity from request DTO")
    void updateEntity_whenValidInputs_updatesEntity() {
        Staircase existingStaircase =
                Staircase.builder().id(10L).floor(1).x(50.0).y(100.0).name("Old Name").active(true).build();

        StaircaseRequestDto updateDto = StaircaseRequestDto.builder()
                .floor(5)
                .x(250.0)
                .y(350.0)
                .name("Updated Staircase")
                .active(false)
                .build();

        staircaseMapper.updateEntity(existingStaircase, updateDto);

        assertAll("Updated Staircase Entity", () -> assertThat(existingStaircase.getId()).isEqualTo(10L),
                () -> assertThat(existingStaircase.getFloor()).isEqualTo(5),
                () -> assertThat(existingStaircase.getX()).isEqualTo(250.0),
                () -> assertThat(existingStaircase.getY()).isEqualTo(350.0),
                () -> assertThat(existingStaircase.getName()).isEqualTo("Updated Staircase"),
                () -> assertThat(existingStaircase.isActive()).isFalse());
    }

    @Test
    @DisplayName("updateEntity should not throw when entity is null")
    void updateEntity_whenEntityIsNull_noOp() {
        StaircaseRequestDto updateDto =
                StaircaseRequestDto.builder().floor(5).x(250.0).y(350.0).name("Test").active(true).build();

        staircaseMapper.updateEntity(null, updateDto);
        // No exception means success
    }

    @Test
    @DisplayName("updateEntity should not throw when request DTO is null")
    void updateEntity_whenDtoIsNull_noOp() {
        Staircase existingStaircase =
                Staircase.builder().id(10L).floor(1).x(50.0).y(100.0).name("Original Name").active(true).build();

        staircaseMapper.updateEntity(existingStaircase, null);

        // Entity should remain unchanged
        assertAll("Unchanged Staircase Entity", () -> assertThat(existingStaircase.getFloor()).isEqualTo(1),
                () -> assertThat(existingStaircase.getX()).isEqualTo(50.0),
                () -> assertThat(existingStaircase.getY()).isEqualTo(100.0),
                () -> assertThat(existingStaircase.getName()).isEqualTo("Original Name"),
                () -> assertThat(existingStaircase.isActive()).isTrue());
    }

    @Test
    @DisplayName("updateEntity should preserve existing active value when request active is null")
    void updateEntity_whenActiveIsNull_preservesExistingValue() {
        Staircase existingStaircase =
                Staircase.builder().id(10L).floor(1).x(50.0).y(100.0).name("Original Name").active(true).build();

        StaircaseRequestDto updateDto =
                StaircaseRequestDto.builder().floor(5).x(250.0).y(350.0).name("Updated Name").active(null).build();

        staircaseMapper.updateEntity(existingStaircase, updateDto);

        assertAll("Staircase with preserved active", () -> assertThat(existingStaircase.getFloor()).isEqualTo(5),
                () -> assertThat(existingStaircase.getName()).isEqualTo("Updated Name"),
                () -> assertThat(existingStaircase.isActive()).isTrue());
    }
}
