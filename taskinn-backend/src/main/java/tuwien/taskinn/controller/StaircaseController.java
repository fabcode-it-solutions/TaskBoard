package tuwien.taskinn.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tuwien.taskinn.annotation.RequireRole;
import tuwien.taskinn.dto.request.StaircaseRequestDto;
import tuwien.taskinn.dto.response.StaircaseDto;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.services.StaircaseService;

@Validated
@RestController
@RequestMapping("/api/v1/staircase")
@RequiredArgsConstructor
public class StaircaseController {

    private final StaircaseService staircaseService;

    @GetMapping
    public ResponseEntity<List<StaircaseDto>> getAllStaircases() {
        List<StaircaseDto> staircases = staircaseService.getAllStaircases();
        return ResponseEntity.ok(staircases);
    }

    @GetMapping("/floor/{floor}")
    public ResponseEntity<List<StaircaseDto>> getStaircasesByFloor(@PathVariable Integer floor) {
        List<StaircaseDto> staircases = staircaseService.getStaircasesByFloor(floor);
        return ResponseEntity.ok(staircases);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaircaseDto> getStaircaseById(@PathVariable Long id) {
        StaircaseDto staircase = staircaseService.getStaircaseById(id);
        return ResponseEntity.ok(staircase);
    }

    @RequireRole(Roles.ADMIN)
    @PostMapping
    public ResponseEntity<StaircaseDto> createStaircase(@Valid @RequestBody StaircaseRequestDto requestDto) {
        StaircaseDto newStaircase = staircaseService.createStaircase(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newStaircase);
    }

    @RequireRole(Roles.ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<StaircaseDto> updateStaircase(@PathVariable Long id,
            @Valid @RequestBody StaircaseRequestDto requestDto) {
        StaircaseDto updatedStaircase = staircaseService.updateStaircase(id, requestDto);
        return ResponseEntity.ok(updatedStaircase);
    }

    @RequireRole(Roles.ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaircase(@PathVariable Long id) {
        staircaseService.deleteStaircase(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }
}
