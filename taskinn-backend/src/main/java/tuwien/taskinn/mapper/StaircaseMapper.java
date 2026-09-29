package tuwien.taskinn.mapper;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.request.StaircaseRequestDto;
import tuwien.taskinn.dto.response.StaircaseDto;
import tuwien.taskinn.entity.Staircase;

@Component
public class StaircaseMapper {

    public StaircaseDto toDto(Staircase staircase) {
        if (staircase == null) {
            return null;
        }

        return StaircaseDto.builder()
                .id(staircase.getId())
                .floor(staircase.getFloor())
                .x(staircase.getX())
                .y(staircase.getY())
                .name(staircase.getName())
                .active(staircase.isActive())
                .build();
    }

    public List<StaircaseDto> toDtoList(List<Staircase> staircases) {
        if (staircases == null) {
            return null;
        }
        return staircases.stream().map(this::toDto).collect(Collectors.toList());
    }

    public Staircase toEntity(StaircaseRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }

        return Staircase.builder()
                .id(requestDto.getId())
                .floor(requestDto.getFloor())
                .x(requestDto.getX())
                .y(requestDto.getY())
                .name(requestDto.getName())
                .active(requestDto.getActive() != null ? requestDto.getActive() : true)
                .build();
    }

    public void updateEntity(Staircase staircase, StaircaseRequestDto requestDto) {
        if (staircase == null || requestDto == null) {
            return;
        }

        staircase.setFloor(requestDto.getFloor());
        staircase.setX(requestDto.getX());
        staircase.setY(requestDto.getY());
        staircase.setName(requestDto.getName());
        if (requestDto.getActive() != null) {
            staircase.setActive(requestDto.getActive());
        }
    }
}
