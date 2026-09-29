package tuwien.taskinn.services.impl;

import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tuwien.taskinn.dto.request.StaircaseRequestDto;
import tuwien.taskinn.dto.response.StaircaseDto;
import tuwien.taskinn.entity.Staircase;
import tuwien.taskinn.mapper.StaircaseMapper;
import tuwien.taskinn.repo.StaircaseRepository;
import tuwien.taskinn.services.StaircaseService;

@Service
@RequiredArgsConstructor
public class StaircaseServiceImpl implements StaircaseService {

    private final StaircaseRepository staircaseRepository;
    private final StaircaseMapper staircaseMapper;

    @Override
    public List<StaircaseDto> getAllStaircases() {
        List<Staircase> staircases = staircaseRepository.findAllByActiveTrue();
        return staircaseMapper.toDtoList(staircases);
    }

    @Override
    public List<StaircaseDto> getStaircasesByFloor(Integer floor) {
        List<Staircase> staircases = staircaseRepository.findAllByFloorAndActiveTrue(floor);
        return staircaseMapper.toDtoList(staircases);
    }

    @Override
    public StaircaseDto getStaircaseById(Long id) {
        Staircase staircase = staircaseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Staircase not found: " + id));
        return staircaseMapper.toDto(staircase);
    }

    @Override
    @Transactional
    public StaircaseDto createStaircase(StaircaseRequestDto requestDto) {
        Staircase staircase = staircaseMapper.toEntity(requestDto);
        staircase.setActive(true);
        Staircase savedStaircase = staircaseRepository.save(staircase);
        return staircaseMapper.toDto(savedStaircase);
    }

    @Override
    @Transactional
    public StaircaseDto updateStaircase(Long id, StaircaseRequestDto requestDto) {
        Staircase staircase = staircaseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Staircase not found: " + id));
        staircaseMapper.updateEntity(staircase, requestDto);
        Staircase updatedStaircase = staircaseRepository.save(staircase);
        return staircaseMapper.toDto(updatedStaircase);
    }

    @Override
    @Transactional
    public void deleteStaircase(Long id) {
        Staircase staircase = staircaseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Staircase not found: " + id));
        staircase.setActive(false);
        staircaseRepository.save(staircase);
    }
}
