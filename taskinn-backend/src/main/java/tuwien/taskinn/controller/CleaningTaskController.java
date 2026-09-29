package tuwien.taskinn.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tuwien.taskinn.annotation.RequireRole;
import tuwien.taskinn.annotation.RequireRoles;
import tuwien.taskinn.dto.request.FinishCleaningRequestDto;
import tuwien.taskinn.dto.request.SkipCleaningRequestDto;
import tuwien.taskinn.dto.request.StartCleaningRequestDto;
import tuwien.taskinn.dto.response.CleaningTaskDto;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.services.CleaningTaskService;
import tuwien.taskinn.util.UserUtil;

@Validated
@RestController
@RequestMapping("/api/v1/cleaning-task")
@RequiredArgsConstructor
@RequireRoles(value = {@RequireRole(Roles.ADMIN), @RequireRole(Roles.EMPLOYEE)})
public class CleaningTaskController {

    private final CleaningTaskService cleaningTaskService;

    @PostMapping("/start")
    public ResponseEntity<CleaningTaskDto> startCleaning(@Valid @RequestBody StartCleaningRequestDto requestDto) {

        UserUtil.UserInfo activeUser = UserUtil.getActiveUser();

        CleaningTaskDto task = cleaningTaskService.startCleaning(requestDto.getRoomId(), activeUser.getIdentifier());

        return ResponseEntity.status(HttpStatus.OK).body(task);
    }

    @PostMapping("/finish")
    public ResponseEntity<CleaningTaskDto> finishCleaning(@Valid @RequestBody FinishCleaningRequestDto requestDto) {

        UserUtil.UserInfo activeUser = UserUtil.getActiveUser();
        if (activeUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CleaningTaskDto task = cleaningTaskService.finishCleaning(requestDto.getRoomId(), activeUser.getIdentifier(),
                requestDto.getNotes());

        return ResponseEntity.status(HttpStatus.OK).body(task);
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<CleaningTaskDto> getCurrentTaskForRoom(@PathVariable Long roomId) {
        CleaningTaskDto task = cleaningTaskService.getCurrentTaskForRoom(roomId);

        if (task == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(task);
    }

    @GetMapping("/in-progress")
    public ResponseEntity<List<CleaningTaskDto>> getAllInProgressTasks() {
        List<CleaningTaskDto> tasks = cleaningTaskService.getAllInProgressTasks();
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/skip")
    public ResponseEntity<CleaningTaskDto> skipCleaning(@Valid @RequestBody SkipCleaningRequestDto requestDto) {

        UserUtil.UserInfo activeUser = UserUtil.getActiveUser();
        if (activeUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CleaningTaskDto task = cleaningTaskService.skipCleaning(requestDto.getRoomId(), activeUser.getIdentifier(),
                requestDto.getReason());

        return ResponseEntity.status(HttpStatus.OK).body(task);
    }
}
