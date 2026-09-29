package tuwien.taskinn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tuwien.taskinn.annotation.RequireRole;
import tuwien.taskinn.dto.request.RoomRequestDto;
import tuwien.taskinn.dto.response.FloorOccupancyDto;
import tuwien.taskinn.dto.response.LightRoomDto;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.dto.response.RoomDto;
import tuwien.taskinn.services.RoomService;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping()
    public ResponseEntity<List<LightRoomDto>> getAllRooms(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.status(HttpStatus.OK).body(roomService.getAllRooms(includeInactive));
    }

    @GetMapping("/available")
    public ResponseEntity<List<LightRoomDto>> getAvailableRooms(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(roomService.getAvailableActiveRoomsByDateRange(checkInDate, checkOutDate));
    }

    @RequireRole(Roles.ADMIN)
    @PostMapping
    public ResponseEntity<RoomDto> createRoom(@Valid @RequestBody RoomRequestDto requestDto) {
        RoomDto newRoom = roomService.createRoom(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newRoom);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomDto> getRoomById(@PathVariable Long id) {
        RoomDto room = roomService.getRoomById(id);
        return ResponseEntity.ok(room);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomDto> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequestDto requestDto) {
        RoomDto updatedRoom = roomService.updateRoom(id, requestDto);
        return ResponseEntity.ok(updatedRoom);
    }

    @GetMapping("/floor/{floor}")
    public ResponseEntity<List<RoomDto>> findAllByFloorAndActiveTrue(@PathVariable Integer floor) {
        List<RoomDto> rooms = roomService.findAllByFloorAndActiveTrue(floor);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/floor/{floor}/all")
    public ResponseEntity<List<RoomDto>> findAllByFloor(@PathVariable Integer floor) {
        List<RoomDto> rooms = roomService.findAllByFloor(floor);
        return ResponseEntity.ok(rooms);
    }

    @RequireRole(Roles.ADMIN)
    @DeleteMapping("/{roomId}/disable")
    public ResponseEntity<Void> disableRoom(@PathVariable Long roomId) {
        roomService.deactivateRoomPermanently(roomId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @GetMapping("/max-floor")
    public ResponseEntity<Integer> getMaxFloor() {
        Integer maxFloor = roomService.getMaxFloor();
        return ResponseEntity.ok(maxFloor);
    }

    @RequireRole(Roles.ADMIN)
    @PostMapping("/copy-floor")
    public ResponseEntity<List<RoomDto>> copyFloor(@RequestParam Integer fromFloor, @RequestParam Integer toFloor) {
        List<RoomDto> copiedRooms = roomService.copyFloor(fromFloor, toFloor);
        return ResponseEntity.status(HttpStatus.CREATED).body(copiedRooms);
    }

    @GetMapping("/occupancy-rate/today")
    public ResponseEntity<Double> getTodaysOccupancyRate() {
        Double occupancyRate = roomService.getTodaysOccupancyRate();
        return ResponseEntity.ok(occupancyRate);
    }

    @GetMapping("/floor-occupancy")
    public ResponseEntity<List<FloorOccupancyDto>> getFloorOccupancyDistribution() {
        List<FloorOccupancyDto> floorOccupancy = roomService.getFloorOccupancyDistribution();
        return ResponseEntity.ok(floorOccupancy);
    }
}
