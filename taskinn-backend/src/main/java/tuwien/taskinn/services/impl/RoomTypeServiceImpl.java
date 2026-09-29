package tuwien.taskinn.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import tuwien.taskinn.dto.request.RoomTypeRequestDto;
import tuwien.taskinn.dto.response.LiteRoomTypeDto;
import tuwien.taskinn.dto.response.RoomDto;
import tuwien.taskinn.dto.response.RoomTypeDto;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.mapper.RoomMapper;
import tuwien.taskinn.mapper.RoomTypeMapper;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.repo.RoomTypeRepository;
import tuwien.taskinn.services.RoomTypeService;
import tuwien.taskinn.services.validator.RoomTypeValidator;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final RoomTypeValidator roomTypeValidator;

    @Override
    public List<LiteRoomTypeDto> getAllRoomTypes() {
        return roomTypeRepository.findAll()
                .stream()
                .map(roomType -> LiteRoomTypeDto.builder().id(roomType.getId()).name(roomType.getName()).build())
                .toList();
    }

    @Override
    public RoomTypeDto getRoomTypeById(Long id) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + id));
        return roomTypeMapper.toDto(roomType);
    }

    @Override
    @Transactional
    public RoomTypeDto createRoomType(RoomTypeRequestDto dto) {
        roomTypeValidator.validateRoomTypeCreation(dto);
        RoomType roomType = roomTypeMapper.toEntity(dto);
        RoomType savedRoomType = roomTypeRepository.save(roomType);
        return roomTypeMapper.toDto(savedRoomType);
    }

    @Override
    @Transactional
    public RoomTypeDto updateRoomType(Long id, RoomTypeRequestDto dto) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + id));
        roomTypeValidator.validateRoomTypeUpdate(dto, roomType);
        roomTypeMapper.updateEntity(roomType, dto);
        RoomType updatedRoomType = roomTypeRepository.save(roomType);
        return roomTypeMapper.toDto(updatedRoomType);
    }

    @Override
    public List<RoomDto> getRoomsByRoomTypeId(Long id) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + id));
        return roomRepository.findAllByRoomType(roomType).stream().map(roomMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRoomType(Long id) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + id));

        List<Room> roomsUsingType = roomRepository.findAllByRoomType(roomType);
        if (!roomsUsingType.isEmpty()) {
            String roomNumbers = roomsUsingType.stream().map(r -> r.getRoomNumber()).collect(Collectors.joining(", "));
            throw new BusinessValidationException("roomType",
                    "Cannot delete room type. The following rooms are using it: " + roomNumbers);
        }

        roomTypeRepository.delete(roomType);
    }
}
