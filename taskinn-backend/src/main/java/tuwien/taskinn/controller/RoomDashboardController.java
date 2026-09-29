package tuwien.taskinn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tuwien.taskinn.annotation.RequireRole;
import tuwien.taskinn.annotation.RequireRoles;
import tuwien.taskinn.dto.response.DashboardRoomDto;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.services.RoomDashboardService;

import java.util.List;

@RequireRoles({@RequireRole(Roles.ADMIN), @RequireRole(Roles.EMPLOYEE)})
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class RoomDashboardController {

    private final RoomDashboardService roomDashboardService;

    @GetMapping("/rooms")
    public ResponseEntity<List<DashboardRoomDto>> getCurrentRoomStates() {
        List<DashboardRoomDto> roomStates = roomDashboardService.getCurrentRoomStates();
        return ResponseEntity.ok(roomStates);
    }
}
