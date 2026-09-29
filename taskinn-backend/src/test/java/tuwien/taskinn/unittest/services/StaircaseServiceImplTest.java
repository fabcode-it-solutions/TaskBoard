package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tuwien.taskinn.dto.request.StaircaseRequestDto;
import tuwien.taskinn.dto.response.StaircaseDto;
import tuwien.taskinn.entity.Staircase;
import tuwien.taskinn.mapper.StaircaseMapper;
import tuwien.taskinn.repo.StaircaseRepository;
import tuwien.taskinn.services.impl.StaircaseServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("StaircaseServiceImpl Unit Tests")
public class StaircaseServiceImplTest {

    @Mock
    private StaircaseRepository staircaseRepository;

    @Mock
    private StaircaseMapper staircaseMapper;

    @InjectMocks
    private StaircaseServiceImpl staircaseService;

    @Test
    @DisplayName("getAllStaircases should return list of mapped DTOs")
    void getAllStaircases_whenStaircasesExist_returnsMappedDtos() {
        Staircase staircase1 =
                Staircase.builder().id(1L).floor(1).x(100.0).y(200.0).name("Staircase A").active(true).build();
        Staircase staircase2 =
                Staircase.builder().id(2L).floor(2).x(150.0).y(250.0).name("Staircase B").active(true).build();
        List<Staircase> staircases = List.of(staircase1, staircase2);

        StaircaseDto dto1 =
                StaircaseDto.builder().id(1L).floor(1).x(100.0).y(200.0).name("Staircase A").active(true).build();
        StaircaseDto dto2 =
                StaircaseDto.builder().id(2L).floor(2).x(150.0).y(250.0).name("Staircase B").active(true).build();
        List<StaircaseDto> expectedDtos = List.of(dto1, dto2);

        when(staircaseRepository.findAllByActiveTrue()).thenReturn(staircases);
        when(staircaseMapper.toDtoList(staircases)).thenReturn(expectedDtos);

        List<StaircaseDto> result = staircaseService.getAllStaircases();

        assertAll("getAllStaircases success", () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0).getName()).isEqualTo("Staircase A"),
                () -> assertThat(result.get(1).getName()).isEqualTo("Staircase B"),
                () -> verify(staircaseRepository, times(1)).findAllByActiveTrue(),
                () -> verify(staircaseMapper, times(1)).toDtoList(staircases));
    }

    @Test
    @DisplayName("getAllStaircases should return empty list when no staircases exist")
    void getAllStaircases_whenNoStaircasesExist_returnsEmptyList() {
        when(staircaseRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());
        when(staircaseMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<StaircaseDto> result = staircaseService.getAllStaircases();

        assertAll("getAllStaircases empty", () -> assertThat(result).isEmpty(),
                () -> verify(staircaseRepository, times(1)).findAllByActiveTrue());
    }

    @Test
    @DisplayName("getStaircasesByFloor should return staircases for given floor")
    void getStaircasesByFloor_whenStaircasesExist_returnsMappedDtos() {
        Integer floor = 2;
        Staircase staircase = Staircase.builder()
                .id(1L)
                .floor(floor)
                .x(100.0)
                .y(200.0)
                .name("Floor 2 Staircase")
                .active(true)
                .build();
        List<Staircase> staircases = List.of(staircase);

        StaircaseDto dto = StaircaseDto.builder()
                .id(1L)
                .floor(floor)
                .x(100.0)
                .y(200.0)
                .name("Floor 2 Staircase")
                .active(true)
                .build();
        List<StaircaseDto> expectedDtos = List.of(dto);

        when(staircaseRepository.findAllByFloorAndActiveTrue(floor)).thenReturn(staircases);
        when(staircaseMapper.toDtoList(staircases)).thenReturn(expectedDtos);

        List<StaircaseDto> result = staircaseService.getStaircasesByFloor(floor);

        assertAll("getStaircasesByFloor success", () -> assertThat(result).hasSize(1),
                () -> assertThat(result.get(0).getFloor()).isEqualTo(floor),
                () -> verify(staircaseRepository, times(1)).findAllByFloorAndActiveTrue(floor));
    }

    @Test
    @DisplayName("getStaircasesByFloor should return empty list for floor with no staircases")
    void getStaircasesByFloor_whenNoStaircasesOnFloor_returnsEmptyList() {
        Integer floor = 5;
        when(staircaseRepository.findAllByFloorAndActiveTrue(floor)).thenReturn(Collections.emptyList());
        when(staircaseMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<StaircaseDto> result = staircaseService.getStaircasesByFloor(floor);

        assertAll("getStaircasesByFloor empty", () -> assertThat(result).isEmpty(),
                () -> verify(staircaseRepository, times(1)).findAllByFloorAndActiveTrue(floor));
    }

    @Test
    @DisplayName("getStaircaseById should return mapped DTO when staircase exists")
    void getStaircaseById_whenStaircaseExists_returnsMappedDto() {
        Long id = 1L;
        Staircase staircase =
                Staircase.builder().id(id).floor(2).x(100.0).y(200.0).name("Main Staircase").active(true).build();
        StaircaseDto expectedDto =
                StaircaseDto.builder().id(id).floor(2).x(100.0).y(200.0).name("Main Staircase").active(true).build();

        when(staircaseRepository.findById(id)).thenReturn(Optional.of(staircase));
        when(staircaseMapper.toDto(staircase)).thenReturn(expectedDto);

        StaircaseDto result = staircaseService.getStaircaseById(id);

        assertAll("getStaircaseById success", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getId()).isEqualTo(id),
                () -> assertThat(result.getName()).isEqualTo("Main Staircase"),
                () -> verify(staircaseRepository, times(1)).findById(id),
                () -> verify(staircaseMapper, times(1)).toDto(staircase));
    }

    @Test
    @DisplayName("getStaircaseById should throw NoSuchElementException when staircase not found")
    void getStaircaseById_whenStaircaseNotFound_throwsNoSuchElementException() {
        Long id = 999L;
        when(staircaseRepository.findById(id)).thenReturn(Optional.empty());

        NoSuchElementException exception =
                assertThrows(NoSuchElementException.class, () -> staircaseService.getStaircaseById(id));

        assertAll("getStaircaseById not found",
                () -> assertThat(exception.getMessage()).contains("Staircase not found"),
                () -> verify(staircaseRepository, times(1)).findById(id),
                () -> verify(staircaseMapper, never()).toDto(any()));
    }

    @Test
    @DisplayName("createStaircase should create staircase with active=true and return DTO")
    void createStaircase_whenValidRequest_createsAndReturnsDto() {
        StaircaseRequestDto requestDto =
                StaircaseRequestDto.builder().floor(3).x(75.0).y(125.0).name("New Staircase").active(true).build();

        Staircase mappedEntity =
                Staircase.builder().floor(3).x(75.0).y(125.0).name("New Staircase").active(false).build();

        Staircase savedEntity =
                Staircase.builder().id(1L).floor(3).x(75.0).y(125.0).name("New Staircase").active(true).build();

        StaircaseDto expectedDto =
                StaircaseDto.builder().id(1L).floor(3).x(75.0).y(125.0).name("New Staircase").active(true).build();

        when(staircaseMapper.toEntity(requestDto)).thenReturn(mappedEntity);
        when(staircaseRepository.save(any(Staircase.class))).thenReturn(savedEntity);
        when(staircaseMapper.toDto(savedEntity)).thenReturn(expectedDto);

        StaircaseDto result = staircaseService.createStaircase(requestDto);

        ArgumentCaptor<Staircase> staircaseCaptor = ArgumentCaptor.forClass(Staircase.class);
        verify(staircaseRepository).save(staircaseCaptor.capture());

        assertAll("createStaircase success", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getId()).isEqualTo(1L),
                () -> assertThat(staircaseCaptor.getValue().isActive()).isTrue(),
                () -> verify(staircaseMapper, times(1)).toEntity(requestDto),
                () -> verify(staircaseMapper, times(1)).toDto(savedEntity));
    }

    @Test
    @DisplayName("updateStaircase should update existing staircase and return DTO")
    void updateStaircase_whenStaircaseExists_updatesAndReturnsDto() {
        Long id = 1L;
        StaircaseRequestDto requestDto = StaircaseRequestDto.builder()
                .floor(5)
                .x(250.0)
                .y(350.0)
                .name("Updated Staircase")
                .active(false)
                .build();

        Staircase existingStaircase =
                Staircase.builder().id(id).floor(2).x(100.0).y(200.0).name("Old Staircase").active(true).build();

        Staircase updatedStaircase =
                Staircase.builder().id(id).floor(5).x(250.0).y(350.0).name("Updated Staircase").active(false).build();

        StaircaseDto expectedDto = StaircaseDto.builder()
                .id(id)
                .floor(5)
                .x(250.0)
                .y(350.0)
                .name("Updated Staircase")
                .active(false)
                .build();

        when(staircaseRepository.findById(id)).thenReturn(Optional.of(existingStaircase));
        when(staircaseRepository.save(existingStaircase)).thenReturn(updatedStaircase);
        when(staircaseMapper.toDto(updatedStaircase)).thenReturn(expectedDto);

        StaircaseDto result = staircaseService.updateStaircase(id, requestDto);

        assertAll("updateStaircase success", () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getName()).isEqualTo("Updated Staircase"),
                () -> verify(staircaseRepository, times(1)).findById(id),
                () -> verify(staircaseMapper, times(1)).updateEntity(existingStaircase, requestDto),
                () -> verify(staircaseRepository, times(1)).save(existingStaircase));
    }

    @Test
    @DisplayName("updateStaircase should throw NoSuchElementException when staircase not found")
    void updateStaircase_whenStaircaseNotFound_throwsNoSuchElementException() {
        Long id = 999L;
        StaircaseRequestDto requestDto = StaircaseRequestDto.builder()
                .floor(5)
                .x(250.0)
                .y(350.0)
                .name("Updated Staircase")
                .active(false)
                .build();

        when(staircaseRepository.findById(id)).thenReturn(Optional.empty());

        NoSuchElementException exception =
                assertThrows(NoSuchElementException.class, () -> staircaseService.updateStaircase(id, requestDto));

        assertAll("updateStaircase not found", () -> assertThat(exception.getMessage()).contains("Staircase not found"),
                () -> verify(staircaseRepository, times(1)).findById(id),
                () -> verify(staircaseMapper, never()).updateEntity(any(), any()),
                () -> verify(staircaseRepository, never()).save(any()));
    }

    @Test
    @DisplayName("deleteStaircase should perform soft delete by setting active=false")
    void deleteStaircase_whenStaircaseExists_setsActiveToFalse() {
        Long id = 1L;
        Staircase existingStaircase =
                Staircase.builder().id(id).floor(2).x(100.0).y(200.0).name("Staircase to Delete").active(true).build();

        when(staircaseRepository.findById(id)).thenReturn(Optional.of(existingStaircase));

        ArgumentCaptor<Staircase> staircaseCaptor = ArgumentCaptor.forClass(Staircase.class);

        staircaseService.deleteStaircase(id);

        verify(staircaseRepository).save(staircaseCaptor.capture());

        assertAll("deleteStaircase soft delete", () -> assertThat(staircaseCaptor.getValue().isActive()).isFalse(),
                () -> verify(staircaseRepository, times(1)).findById(id),
                () -> verify(staircaseRepository, times(1)).save(any(Staircase.class)));
    }

    @Test
    @DisplayName("deleteStaircase should throw NoSuchElementException when staircase not found")
    void deleteStaircase_whenStaircaseNotFound_throwsNoSuchElementException() {
        Long id = 999L;
        when(staircaseRepository.findById(id)).thenReturn(Optional.empty());

        NoSuchElementException exception =
                assertThrows(NoSuchElementException.class, () -> staircaseService.deleteStaircase(id));

        assertAll("deleteStaircase not found", () -> assertThat(exception.getMessage()).contains("Staircase not found"),
                () -> verify(staircaseRepository, times(1)).findById(id),
                () -> verify(staircaseRepository, never()).save(any()));
    }
}
