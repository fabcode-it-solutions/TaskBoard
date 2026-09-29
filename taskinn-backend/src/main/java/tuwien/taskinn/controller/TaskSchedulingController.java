package tuwien.taskinn.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tuwien.taskinn.dto.CleaningTaskDto;
import tuwien.taskinn.dto.request.TaskScheduleSolveRequestDto;
import tuwien.taskinn.services.TaskSchedulingService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/task-scheduling")
@RequiredArgsConstructor
public class TaskSchedulingController {

    private final TaskSchedulingService taskService;

    @GetMapping("/schedule/{year}/{month}/{day}")
    public List<CleaningTaskDto> getTasksForDay(@PathVariable @NotNull @Min(2024) Integer year,
            @PathVariable @NotNull @Min(1) @Max(12) Integer month,
            @PathVariable @NotNull @Min(1) @Max(31) Integer day) {
        return taskService.getTasksForDay(year, month, day);
    }

    @GetMapping("/schedule/{year}/{month}/{day}/{id}")
    public List<CleaningTaskDto> getTasksForEmployeeAndDay(@PathVariable @NotNull @Min(2024) Integer year,
            @PathVariable @NotNull @Min(1) @Max(12) Integer month, @PathVariable @NotNull @Min(1) @Max(31) Integer day,
            @PathVariable @NotNull Long id) {
        return taskService.getTasksForEmployeeAndDay(year, month, day, id);
    }

    @PutMapping("/schedule/{year}/{month}/{day}")
    public void putTasksForDay(@PathVariable @NotNull @Min(2024) Integer year,
            @PathVariable @NotNull @Min(1) @Max(12) Integer month, @PathVariable @NotNull @Min(1) @Max(31) Integer day,
            @RequestBody @NotNull List<CleaningTaskDto> tasks) {
        taskService.putTasksForDay(year, month, day, tasks);
    }

    @PostMapping("/optimized-schedule")
    public List<CleaningTaskDto> solveTaskScheduling(@RequestBody @NotNull TaskScheduleSolveRequestDto request) {
        return taskService.solveTaskScheduling(request);
    }

}
