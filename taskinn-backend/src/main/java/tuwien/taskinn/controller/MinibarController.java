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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tuwien.taskinn.annotation.RequireRole;
import tuwien.taskinn.annotation.RequireRoles;
import tuwien.taskinn.dto.request.InventoryItemRequestDto;
import tuwien.taskinn.dto.request.MinibarBatchConsumptionRequestDto;
import tuwien.taskinn.dto.request.MinibarConsumptionRequestDto;
import tuwien.taskinn.dto.request.MinibarRestockRequestDto;
import tuwien.taskinn.dto.response.InventoryItemDto;
import tuwien.taskinn.dto.response.InventoryItemSelectionResponseDto;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.services.MinibarService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/minibar")
@RequiredArgsConstructor
public class MinibarController {

    private final MinibarService minibarService;

    @RequireRoles({@RequireRole(Roles.ADMIN), @RequireRole(Roles.EMPLOYEE)})
    @GetMapping("/items")
    public ResponseEntity<List<InventoryItemDto>> getAllItems() {
        return ResponseEntity.status(HttpStatus.OK).body(minibarService.getAllActiveItems());
    }

    @RequireRoles({@RequireRole(Roles.ADMIN), @RequireRole(Roles.EMPLOYEE)})
    @GetMapping("/items/{id}")
    public ResponseEntity<InventoryItemDto> getItemById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(minibarService.getItemById(id));
    }

    @RequireRole(Roles.ADMIN)
    @PostMapping("/items")
    public ResponseEntity<InventoryItemDto> createItem(@Valid @RequestBody InventoryItemRequestDto dto) {
        InventoryItemDto created = minibarService.createItem(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @RequireRole(Roles.ADMIN)
    @PutMapping("/items/{id}")
    public ResponseEntity<InventoryItemDto> updateItem(@PathVariable Long id,
            @Valid @RequestBody InventoryItemRequestDto dto) {
        InventoryItemDto updated = minibarService.updateItem(id, dto);
        return ResponseEntity.status(HttpStatus.OK).body(updated);
    }

    @RequireRole(Roles.ADMIN)
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        minibarService.deleteItem(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @RequireRoles({@RequireRole(Roles.ADMIN), @RequireRole(Roles.EMPLOYEE)})
    @PostMapping("/rooms/{roomId}/consume")
    public ResponseEntity<Void> recordConsumption(@PathVariable Long roomId,
            @Valid @RequestBody MinibarConsumptionRequestDto dto,
            @RequestParam(defaultValue = "false") boolean allowRecentBooking) {
        minibarService.recordConsumption(roomId, dto, allowRecentBooking);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @RequireRoles({@RequireRole(Roles.ADMIN), @RequireRole(Roles.EMPLOYEE)})
    @PostMapping("/rooms/{roomId}/consume-batch")
    public ResponseEntity<Void> recordBatchConsumption(@PathVariable Long roomId,
            @Valid @RequestBody MinibarBatchConsumptionRequestDto dto,
            @RequestParam(defaultValue = "false") boolean allowRecentBooking) {
        minibarService.recordBatchConsumption(roomId, dto.getItems(), allowRecentBooking);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @RequireRoles({@RequireRole(Roles.ADMIN), @RequireRole(Roles.EMPLOYEE)})
    @PostMapping("/rooms/{roomId}/restock")
    public ResponseEntity<InventoryItemDto> restockItem(@PathVariable Long roomId,
            @Valid @RequestBody MinibarRestockRequestDto dto) {
        InventoryItemDto updated = minibarService.restockItem(roomId, dto);
        return ResponseEntity.status(HttpStatus.OK).body(updated);
    }

    /**
     * Retrieves a lightweight list (ID and Name) of all inventory items, including deleted ones. Intended for
     * populating statistics filters/dropdowns.
     */
    @RequireRoles({@RequireRole(Roles.ADMIN)})
    @GetMapping("/items/selection")
    public ResponseEntity<List<InventoryItemSelectionResponseDto>> getAllItemsForSelection() {
        return ResponseEntity.ok(minibarService.getAllItemsForSelection());
    }

    @RequireRoles({@RequireRole(Roles.ADMIN), @RequireRole(Roles.EMPLOYEE)})
    @GetMapping("/rooms/{roomId}/can-report")
    public ResponseEntity<Boolean> canReportMinibar(@PathVariable Long roomId) {
        boolean canReport = minibarService.hasValidBookingForMinibar(roomId);
        return ResponseEntity.status(HttpStatus.OK).body(canReport);
    }
}
