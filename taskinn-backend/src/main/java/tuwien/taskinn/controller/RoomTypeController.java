package tuwien.taskinn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tuwien.taskinn.annotation.RequireRole;
import tuwien.taskinn.dto.request.RoomTypeRequestDto;
import tuwien.taskinn.dto.response.LiteRoomTypeDto;
import tuwien.taskinn.dto.response.RoomDto;
import tuwien.taskinn.dto.response.RoomTypeDto;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.services.RoomTypeService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/room-type")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @GetMapping()
    public ResponseEntity<List<LiteRoomTypeDto>> getAllRoomTypes() {
        return ResponseEntity.status(HttpStatus.OK).body(roomTypeService.getAllRoomTypes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomTypeDto> getRoomTypeById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(roomTypeService.getRoomTypeById(id));
    }

    @RequireRole(Roles.ADMIN)
    @PostMapping
    public ResponseEntity<RoomTypeDto> createRoomType(@Valid @RequestBody RoomTypeRequestDto dto) {
        RoomTypeDto created = roomTypeService.createRoomType(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @RequireRole(Roles.ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<RoomTypeDto> updateRoomType(@PathVariable Long id,
            @Valid @RequestBody RoomTypeRequestDto dto) {
        RoomTypeDto updated = roomTypeService.updateRoomType(id, dto);
        return ResponseEntity.status(HttpStatus.OK).body(updated);
    }

    @RequireRole(Roles.ADMIN)
    @GetMapping("/{id}/rooms")
    public ResponseEntity<List<RoomDto>> getRoomsByRoomTypeId(@PathVariable Long id) {
        List<RoomDto> rooms = roomTypeService.getRoomsByRoomTypeId(id);
        return ResponseEntity.status(HttpStatus.OK).body(rooms);
    }

    @RequireRole(Roles.ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoomType(@PathVariable Long id) {
        roomTypeService.deleteRoomType(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
