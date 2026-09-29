package tuwien.taskinn.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tuwien.taskinn.dto.EmployeeDto;
import tuwien.taskinn.dto.PtoDto;
import tuwien.taskinn.dto.request.CreatePtoRequest;
import tuwien.taskinn.services.EmployeeService;
import tuwien.taskinn.util.UserUtil;

@Validated
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping()
    public List<EmployeeDto> getAllStaffMembers() {
        return employeeService.getAllStaffMembers();
    }

    @GetMapping("/{id}")
    public EmployeeDto getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id);
    }

    @PutMapping("/{id}")
    public EmployeeDto updateEmployee(@PathVariable Long id, @RequestBody EmployeeDto updateEmployeeRequestDto) {
        return employeeService.updateEmployee(id, updateEmployeeRequestDto);
    }

    @GetMapping("/pto")
    public List<PtoDto> getPTOs() {
        UserUtil.UserInfo activeUser = UserUtil.getActiveUser();
        return this.employeeService.getPtosByEmployeeEmail(activeUser.getIdentifier());
    }

    @PostMapping("/pto")
    public PtoDto createPto(@RequestBody @Valid CreatePtoRequest request) {
        UserUtil.UserInfo activeUser = UserUtil.getActiveUser();
        return this.employeeService.createPtoForEmployee(activeUser.getIdentifier(), request);
    }

    @DeleteMapping("/pto/{id}")
    public void deletePto(@PathVariable("id") Long id) {
        UserUtil.UserInfo activeUser = UserUtil.getActiveUser();
        this.employeeService.deletePtoForEmployee(activeUser.getIdentifier(), id);
    }

    @GetMapping("/shifts/{year}/{month}")
    public List<Integer> getMyShifts(@PathVariable @NotNull @Min(2024) Integer year,
            @PathVariable @NotNull @Min(1) @Max(12) Integer month) {
        UserUtil.UserInfo activeUser = UserUtil.getActiveUser();
        return this.employeeService.getShiftsByEmployeeEmail(activeUser.getIdentifier(), year, month);
    }
}
