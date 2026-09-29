package tuwien.taskinn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tuwien.taskinn.dto.request.MaintenanceTicketCreationUpdateDto;
import tuwien.taskinn.dto.response.MaintenanceTicketDto;
import tuwien.taskinn.services.MaintenanceService;
import tuwien.taskinn.util.UserUtil;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping()
    public ResponseEntity<MaintenanceTicketDto> createMaintenanceTicket(
            @Valid @RequestBody MaintenanceTicketCreationUpdateDto requestDto) {

        UserUtil.UserInfo activeUser = UserUtil.getActiveUser();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.maintenanceService.createMaintenanceTicket(requestDto, activeUser.getIdentifier()));
    }

    @GetMapping()
    public ResponseEntity<List<MaintenanceTicketDto>> getAllMaintenanceTickets() {
        return ResponseEntity.status(HttpStatus.OK).body(this.maintenanceService.getAllMaintenanceTickets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceTicketDto> getMaintenanceTicketById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(this.maintenanceService.getMaintenanceTicketById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceTicketDto> updateMaintenanceTicket(@PathVariable Long id,
            @Valid @RequestBody MaintenanceTicketCreationUpdateDto requestDto) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(this.maintenanceService.updateMaintenanceTicket(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaintenanceTicket(@PathVariable Long id) {
        this.maintenanceService.deleteMaintenanceTicket(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @GetMapping("/open")
    public ResponseEntity<List<MaintenanceTicketDto>> getAllOpenMaintenanceTickets() {
        return ResponseEntity.status(HttpStatus.OK).body(this.maintenanceService.getAllOpenMaintenanceTickets());
    }

    @GetMapping("/open/high-priority/count")
    public ResponseEntity<Long> countHighPriorityOpenTickets() {
        Long count = this.maintenanceService.countHighPriorityTicketsNeedingAttention();
        return ResponseEntity.status(HttpStatus.OK).body(count);
    }
}
