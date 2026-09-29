package tuwien.taskinn.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import tuwien.taskinn.dto.EmployeeMonthShiftDto;
import tuwien.taskinn.dto.SchedulableEmployeeDto;
import tuwien.taskinn.dto.SchedulingRoleDto;
import tuwien.taskinn.dto.ShiftScheduleTemplateDto;
import tuwien.taskinn.dto.request.ShiftScheduleSolveRequestDto;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.services.ShiftSchedulingService;

@RequireRole(Roles.ADMIN)
@Validated
@RestController
@RequestMapping("/api/v1/shift-scheduling")
@RequiredArgsConstructor
public class ShiftSchedulingController {

    private final ShiftSchedulingService shiftSchedulingService;

    @GetMapping("/employees")
    public List<SchedulableEmployeeDto> getSchedulableEmployees() {
        return shiftSchedulingService.getSchedulableEmployees();
    }

    @GetMapping("/shifts/{year}/{month}")
    public List<EmployeeMonthShiftDto> getShiftsForMonth(@PathVariable @NotNull @Min(2024) Integer year,
            @PathVariable @NotNull @Min(1) @Max(12) Integer month) {
        return shiftSchedulingService.getShiftsForMonth(year, month);
    }

    @PutMapping("/shifts/{year}/{month}")
    public void putShiftSchedule(@PathVariable @NotNull @Min(2024) Integer year,
            @PathVariable @NotNull @Min(1) @Max(12) Integer month,
            @RequestBody @NotNull List<EmployeeMonthShiftDto> shifts) {
        shiftSchedulingService.putShiftSchedule(year, month, shifts);
    }

    @PostMapping("/optimized-shift")
    public List<EmployeeMonthShiftDto> solveShiftScheduling(
            @RequestBody @NotNull ShiftScheduleSolveRequestDto request) {
        return shiftSchedulingService.solveShiftScheduling(request);
    }

    @GetMapping("/roles")
    public List<SchedulingRoleDto> getAllSchedulingRoles() {
        return shiftSchedulingService.getAllSchedulingRoles();
    }

    @PutMapping("/roles")
    public void createSchedulingRole(@NotNull @RequestBody SchedulingRoleDto request) {
        shiftSchedulingService.createSchedulingRole(request);
    }

    @PostMapping("/roles/{id}")
    public void updateSchedulingRole(@PathVariable @NotNull Long id, @RequestBody @NotNull SchedulingRoleDto request) {
        shiftSchedulingService.updateSchedulingRole(id, request);
    }

    @DeleteMapping("/roles/{id}")
    public void deleteSchedulingRole(@PathVariable @NotNull Long id) {
        shiftSchedulingService.deleteSchedulingRole(id);
    }

    @GetMapping("/shift-templates")
    public List<ShiftScheduleTemplateDto> getAllShiftScheduleTemplates() {
        return shiftSchedulingService.getAllShiftScheduleTemplates();
    }

    @PutMapping("/shift-templates")
    public void createShiftScheduleTemplate(@RequestBody @NotNull ShiftScheduleTemplateDto request) {
        shiftSchedulingService.createShiftScheduleTemplate(request);
    }

    @DeleteMapping("/shift-templates/{id}")
    public void deleteShiftScheduleTemplate(@PathVariable @NotNull Long id) {
        shiftSchedulingService.deleteShiftScheduleTemplate(id);
    }

    @PostMapping("/shift-templates/{id}")
    public void updateShiftScheduleTemplate(@PathVariable @NotNull Long id,
            @RequestBody @NotNull ShiftScheduleTemplateDto request) {
        shiftSchedulingService.updateShiftScheduleTemplate(id, request);
    }

    @GetMapping(value = "/shifts/{year}/{month}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getShiftSchedulePdf(@PathVariable @NotNull @Min(2024) Integer year,
            @PathVariable @NotNull @Min(1) @Max(12) Integer month) {
        byte[] pdfBytes = shiftSchedulingService.getShiftSchedulePdf(year, month);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=schedule_" + year + "_" + month + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

}
