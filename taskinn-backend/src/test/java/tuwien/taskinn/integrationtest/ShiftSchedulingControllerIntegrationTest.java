package tuwien.taskinn.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.EmployeeMonthShiftDto;
import tuwien.taskinn.dto.SchedulableEmployeeDto;
import tuwien.taskinn.dto.SchedulingRoleDto;
import tuwien.taskinn.dto.ShiftScheduleTemplateDto;
import tuwien.taskinn.dto.request.ShiftScheduleSolveRequestDto;
import tuwien.taskinn.entity.*;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.PTORepository;
import tuwien.taskinn.repo.SchedulingRoleRepository;
import tuwien.taskinn.repo.ShiftScheduleTemplateRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ShiftSchedulingControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String SHIFT_SCHEDULING_BASE_URI = BASE_URI + "/shift-scheduling";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PTORepository ptoRepository;

    @Autowired
    private SchedulingRoleRepository roleRepository;

    @Autowired
    private ShiftScheduleTemplateRepository templateRepository;

    @Test
    public void getSchedulableEmployees_whenSchedulableEmployeesExistAndAuthorized_returnsAllSchedulableEmployees()
            throws Exception {
        final Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());
        final Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());
        final Employee employee3 = employeeRepository.save(Employee.builder()
                .firstName("Max")
                .lastName("Mustermann")
                .email("max.muster@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .roles(Set.of(Roles.ADMIN))
                .build());

        final MvcResult mvcResult =
                mockMvc.perform(get(SHIFT_SCHEDULING_BASE_URI + "/employees").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + getAdminAuthToken())).andExpect(status().isOk()).andReturn();

        final String responseString = mvcResult.getResponse().getContentAsString();
        final List<SchedulableEmployeeDto> result =
                objectMapper.readValue(responseString, new TypeReference<List<SchedulableEmployeeDto>>() {});

        assertAll(() -> assertThat(result).extracting(SchedulableEmployeeDto::getId)
                .containsExactlyInAnyOrder(employee1.getId(), employee2.getId()));
    }

    @Test
    public void getSchedulableEmployees_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(get(SHIFT_SCHEDULING_BASE_URI + "/employees")).andExpect(status().isForbidden());
    }

    @Test
    public void getShiftsForMonth_whenShiftsExistAndAuthorized_returnShifts() throws Exception {
        final Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());
        final Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        shiftRepository.save(
                Shift.builder().date(LocalDateTime.of(2026, 5, 1, 0, 0, 0)).department("").employee(employee1).build());
        shiftRepository.save(
                Shift.builder().date(LocalDateTime.of(2026, 5, 5, 0, 0, 0)).department("").employee(employee1).build());
        shiftRepository.save(
                Shift.builder().date(LocalDateTime.of(2026, 5, 3, 0, 0, 0)).department("").employee(employee2).build());

        ptoRepository.save(PTO.builder()
                .startDate(LocalDate.of(2026, 5, 10))
                .endDate(LocalDate.of(2026, 5, 15))
                .employee(employee2)
                .build());

        final MvcResult mvcResult =
                mockMvc.perform(get(SHIFT_SCHEDULING_BASE_URI + "/shifts/2026/5").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + getAdminAuthToken())).andExpect(status().isOk()).andReturn();

        final String responseString = mvcResult.getResponse().getContentAsString();
        final List<EmployeeMonthShiftDto> result =
                objectMapper.readValue(responseString, new TypeReference<List<EmployeeMonthShiftDto>>() {});
        final Map<Long, EmployeeMonthShiftDto> resultMap =
                result.stream().collect(Collectors.toMap(x -> x.getEmployee().getId(), x -> x));

        assertAll(() -> assertThat(result).hasSize(2), () -> assertThat(resultMap).containsKey(employee1.getId()),
                () -> assertThat(resultMap).containsKey(employee2.getId()),
                () -> assertThat(resultMap.get(employee1.getId()).getDayIndices()).containsExactlyInAnyOrder(0, 4),
                () -> assertThat(resultMap.get(employee1.getId()).getPtoIndices()).isEmpty(),
                () -> assertThat(resultMap.get(employee2.getId()).getDayIndices()).containsExactlyInAnyOrder(2),
                () -> assertThat(resultMap.get(employee2.getId()).getPtoIndices()).containsExactlyInAnyOrder(9, 10, 11,
                        12, 13, 14));
    }

    @Test
    public void getShiftsForMonth_whenNoShiftsExistAndAuthorized_returnShifts() throws Exception {
        final Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        shiftRepository.save(
                Shift.builder().date(LocalDateTime.of(2026, 5, 1, 0, 0, 0)).department("").employee(employee1).build());
        shiftRepository.save(
                Shift.builder().date(LocalDateTime.of(2026, 5, 5, 0, 0, 0)).department("").employee(employee1).build());

        final MvcResult mvcResult =
                mockMvc.perform(get(SHIFT_SCHEDULING_BASE_URI + "/shifts/2027/1").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + getAdminAuthToken())).andExpect(status().isOk()).andReturn();

        final String responseString = mvcResult.getResponse().getContentAsString();
        final List<EmployeeMonthShiftDto> result =
                objectMapper.readValue(responseString, new TypeReference<List<EmployeeMonthShiftDto>>() {});

        assertAll(() -> assertThat(result).isEmpty());
    }

    @Test
    public void getShiftsForMonth_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(get(SHIFT_SCHEDULING_BASE_URI + "/shifts/2026/5")).andExpect(status().isForbidden());
    }

    @Test
    public void putShiftSchedule_whenValidAndAuthorized_replaceShifts() throws Exception {
        final Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        shiftRepository.save(
                Shift.builder().date(LocalDateTime.of(2026, 5, 1, 0, 0, 0)).department("").employee(employee1).build());
        shiftRepository.save(
                Shift.builder().date(LocalDateTime.of(2026, 5, 5, 0, 0, 0)).department("").employee(employee1).build());

        final List<EmployeeMonthShiftDto> request = List.of(EmployeeMonthShiftDto.builder()
                .employee(SchedulableEmployeeDto.builder().id(employee1.getId()).build())
                .dayIndices(List.of(9, 10, 11))
                .build());

        mockMvc.perform(put(SHIFT_SCHEDULING_BASE_URI + "/shifts/2026/5")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

        final List<Shift> shifts = shiftRepository.findAllShiftsInTimeframe(LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 31, 0, 0));

        assertAll(() -> assertThat(shifts).hasSize(3),
                () -> assertThat(shifts).extracting(x -> x.getDate().getDayOfMonth())
                        .containsExactlyInAnyOrder(10, 11, 12));
    }

    @Test
    public void putShiftSchedule_whenNotAuthorized_returnForbidden() throws Exception {
        final List<EmployeeMonthShiftDto> request = List.of(EmployeeMonthShiftDto.builder()
                .employee(SchedulableEmployeeDto.builder().id(1L).build())
                .dayIndices(List.of(9, 10, 11))
                .build());

        mockMvc.perform(put(SHIFT_SCHEDULING_BASE_URI + "/shifts/2026/5").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
    }

    @Test
    public void solveShiftScheduling_whenAuthorized_returnSolution() throws Exception {
        final Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());
        final Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        ptoRepository.save(PTO.builder()
                .startDate(LocalDate.of(2026, 5, 10))
                .endDate(LocalDate.of(2026, 5, 15))
                .employee(employee2)
                .build());

        final ShiftScheduleSolveRequestDto request = ShiftScheduleSolveRequestDto.builder()
                .year(2026)
                .month(5)
                .employeeIds(List.of(employee1.getId(), employee2.getId()))
                .staffingLevels(List.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1))
                .dailyHours(List.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1))
                .build();

        final MvcResult mvcResult = mockMvc.perform(post(SHIFT_SCHEDULING_BASE_URI + "/optimized-shift")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk()).andReturn();

        final String responseString = mvcResult.getResponse().getContentAsString();
        final List<EmployeeMonthShiftDto> result =
                objectMapper.readValue(responseString, new TypeReference<List<EmployeeMonthShiftDto>>() {});
        final Map<Long, EmployeeMonthShiftDto> resultMap =
                result.stream().collect(Collectors.toMap(x -> x.getEmployee().getId(), x -> x));

        assertAll(() -> assertThat(result).hasSize(2), () -> assertThat(resultMap).containsKey(employee1.getId()),
                () -> assertThat(resultMap).containsKey(employee2.getId()),
                () -> assertThat(resultMap.get(employee1.getId()).getDayIndices()).contains(9, 10, 11, 12, 13, 14),
                () -> assertThat(resultMap.get(employee2.getId()).getDayIndices()).doesNotContain(9, 10, 11, 12, 13,
                        14));
    }

    @Test
    public void solveShiftScheduling_whenNotAuthorized_returnForbidden() throws Exception {
        final ShiftScheduleSolveRequestDto request = ShiftScheduleSolveRequestDto.builder()
                .year(2026)
                .month(5)
                .employeeIds(List.of(1L, 2L))
                .staffingLevels(List.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1))
                .dailyHours(List.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1))
                .build();
        mockMvc.perform(post(SHIFT_SCHEDULING_BASE_URI + "/optimized-shift").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
    }

    @Test
    public void getAllSchedulingRoles_whenAuthorized_returnAllSchedulingRoles() throws Exception {
        final SchedulingRole role1 = roleRepository.save(SchedulingRole.builder().roleName("Role One").build());
        final SchedulingRole role2 = roleRepository.save(SchedulingRole.builder().roleName("Role One").build());

        final Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .schedulingRole(role1)
                .build());
        final Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .schedulingRole(role2)
                .build());

        final MvcResult mvcResult =
                mockMvc.perform(get(SHIFT_SCHEDULING_BASE_URI + "/roles").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + getAdminAuthToken())).andExpect(status().isOk()).andReturn();

        final String responseString = mvcResult.getResponse().getContentAsString();
        final List<SchedulingRoleDto> result =
                objectMapper.readValue(responseString, new TypeReference<List<SchedulingRoleDto>>() {});
        final Map<Long, SchedulingRoleDto> resultMap =
                result.stream().collect(Collectors.toMap(SchedulingRoleDto::getId, x -> x));

        assertAll(() -> assertThat(result).hasSize(2), () -> assertThat(resultMap).containsKey(role1.getId()),
                () -> assertThat(resultMap).containsKey(role2.getId()),
                () -> assertThat(resultMap.get(role1.getId()).getEmployees()).extracting(SchedulableEmployeeDto::getId)
                        .containsExactly(employee1.getId()),
                () -> assertThat(resultMap.get(role2.getId()).getEmployees()).extracting(SchedulableEmployeeDto::getId)
                        .containsExactly(employee2.getId()));
    }

    @Test
    public void getAllSchedulingRoles_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(get(SHIFT_SCHEDULING_BASE_URI + "/roles")).andExpect(status().isForbidden());
    }

    @Test
    public void createSchedulingRole_whenAuthorized_createSchedulingRole() throws Exception {
        final List<SchedulingRole> rolesBefore = roleRepository.findAll();

        final Employee employee = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        final SchedulingRoleDto request = SchedulingRoleDto.builder()
                .name("Role One")
                .employees(List.of(SchedulableEmployeeDto.builder().id(employee.getId()).build()))
                .build();

        mockMvc.perform(put(SHIFT_SCHEDULING_BASE_URI + "/roles")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

        final List<SchedulingRole> rolesAfter = roleRepository.findAll();
        final Employee employeeAfter = employeeRepository.findById(employee.getId()).orElseThrow();

        assertAll(() -> assertThat(rolesBefore).isEmpty(), () -> assertThat(rolesAfter).hasSize(1),
                () -> assertThat(rolesAfter.getFirst().getRoleName()).isEqualTo("Role One"),
                () -> assertThat(employeeAfter.getSchedulingRole().getId()).isEqualTo(rolesAfter.getFirst().getId()));
    }

    @Test
    public void createSchedulingRole_whenUnauthorized_returnForbidden() throws Exception {
        final SchedulingRoleDto request = SchedulingRoleDto.builder()
                .name("Role One")
                .employees(List.of(SchedulableEmployeeDto.builder().id(1L).build()))
                .build();

        mockMvc.perform(put(SHIFT_SCHEDULING_BASE_URI + "/roles").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
    }

    @Test
    public void updateSchedulingRole_whenValidAndAuthorized_updateSchedulingRole() throws Exception {
        final SchedulingRole role = roleRepository.save(SchedulingRole.builder().roleName("Role One").build());

        final Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .schedulingRole(role)
                .build());
        final Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        final SchedulingRoleDto request = SchedulingRoleDto.builder()
                .name("Role One Updated")
                .employees(List.of(SchedulableEmployeeDto.builder().id(employee2.getId()).build()))
                .build();

        mockMvc.perform(post(SHIFT_SCHEDULING_BASE_URI + "/roles/" + role.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

        final Employee employee1After = employeeRepository.findById(employee1.getId()).orElseThrow();
        final Employee employee2After = employeeRepository.findById(employee2.getId()).orElseThrow();
        final SchedulingRole roleAfter = roleRepository.findById(role.getId()).orElseThrow();

        assertAll(() -> assertThat(roleAfter.getRoleName()).isEqualTo("Role One Updated"),
                () -> assertThat(employee1After.getSchedulingRole()).isNull(),
                () -> assertThat(employee2After.getSchedulingRole().getId()).isEqualTo(roleAfter.getId()));
    }

    @Test
    public void updateSchedulingRole_whenNotAuthorized_returnForbidden() throws Exception {
        final SchedulingRoleDto request = SchedulingRoleDto.builder()
                .name("Role One Updated")
                .employees(List.of(SchedulableEmployeeDto.builder().id(1L).build()))
                .build();

        mockMvc.perform(post(SHIFT_SCHEDULING_BASE_URI + "/roles/1").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
    }

    @Test
    public void deleteSchedulingRole_whenExistsAndAuthorized_deleteSchedulingRole() throws Exception {
        final SchedulingRole role = roleRepository.save(SchedulingRole.builder().roleName("Role One").build());

        final Employee employee = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .schedulingRole(role)
                .build());

        mockMvc.perform(delete(SHIFT_SCHEDULING_BASE_URI + "/roles/" + role.getId()).header(HttpHeaders.AUTHORIZATION,
                "Bearer " + getAdminAuthToken())).andExpect(status().isOk());

        final Optional<SchedulingRole> roleAfter = roleRepository.findById(role.getId());
        final Employee employeeAfter = employeeRepository.findById(employee.getId()).orElseThrow();

        assertAll(() -> assertThat(roleAfter).isEmpty(), () -> assertThat(employeeAfter.getSchedulingRole()).isNull());
    }

    @Test
    public void deleteSchedulingRole_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(delete(SHIFT_SCHEDULING_BASE_URI + "/roles/1")).andExpect(status().isForbidden());
    }

    @Test
    void getAllShiftScheduleTemplates_whenAuthorized_returnAllShiftSchedulingTemplates() throws Exception {
        final ShiftScheduleTemplate template1 = templateRepository
                .save(ShiftScheduleTemplate.builder().name("Template One").content("Content One").build());
        final ShiftScheduleTemplate template2 = templateRepository
                .save(ShiftScheduleTemplate.builder().name("Template Two").content("Content Two").build());

        final MvcResult mvcResult =
                mockMvc.perform(get(SHIFT_SCHEDULING_BASE_URI + "/shift-templates").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + getAdminAuthToken())).andExpect(status().isOk()).andReturn();

        final String responseString = mvcResult.getResponse().getContentAsString();
        final List<ShiftScheduleTemplateDto> result =
                objectMapper.readValue(responseString, new TypeReference<List<ShiftScheduleTemplateDto>>() {});
        final Map<Long, ShiftScheduleTemplateDto> resultMap =
                result.stream().collect(Collectors.toMap(ShiftScheduleTemplateDto::getId, x -> x));

        assertAll(() -> assertThat(result).hasSize(2),
                () -> assertThat(resultMap).containsKeys(template1.getId(), template2.getId()),
                () -> assertThat(resultMap.get(template1.getId()).getName()).isEqualTo("Template One"),
                () -> assertThat(resultMap.get(template1.getId()).getContent()).isEqualTo("Content One"),
                () -> assertThat(resultMap.get(template2.getId()).getName()).isEqualTo("Template Two"),
                () -> assertThat(resultMap.get(template2.getId()).getContent()).isEqualTo("Content Two"));
    }

    @Test
    void getAllShiftScheduleTemplates_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(get(SHIFT_SCHEDULING_BASE_URI + "/shift-templates")).andExpect(status().isForbidden());
    }

    @Test
    void createShiftScheduleTemplate_whenValidAndAuthorized_createShiftScheduleTemplate() throws Exception {
        final ShiftScheduleTemplateDto request =
                ShiftScheduleTemplateDto.builder().name("New Role Name").content("New Role Content").build();

        mockMvc.perform(put(SHIFT_SCHEDULING_BASE_URI + "/shift-templates")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

        final List<ShiftScheduleTemplate> templates = templateRepository.findAll();

        assertAll(() -> assertThat(templates).hasSize(1),
                () -> assertThat(templates.getFirst().getName()).isEqualTo("New Role Name"),
                () -> assertThat(templates.getFirst().getContent()).isEqualTo("New Role Content"));
    }

    @Test
    void createShiftScheduleTemplate_whenNotAuthorized_returnForbidden() throws Exception {
        final ShiftScheduleTemplateDto request =
                ShiftScheduleTemplateDto.builder().name("New Role Name").content("New Role Content").build();

        mockMvc.perform(put(SHIFT_SCHEDULING_BASE_URI + "/shift-templates").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
    }

    @Test
    void deleteShiftScheduleTemplate_whenExistsAndAuthorized_deleteShiftScheduleTemplate() throws Exception {
        final ShiftScheduleTemplate template = templateRepository
                .save(ShiftScheduleTemplate.builder().name("New Role Name").content("New Role Content").build());

        mockMvc.perform(delete(SHIFT_SCHEDULING_BASE_URI + "/shift-templates/" + template.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())).andExpect(status().isOk());

        final Optional<ShiftScheduleTemplate> newTemplate = templateRepository.findById(template.getId());

        assertAll(() -> assertThat(newTemplate).isEmpty());
    }

    @Test
    void deleteShiftScheduleTemplate_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(delete(SHIFT_SCHEDULING_BASE_URI + "/shift-templates/1")).andExpect(status().isForbidden());
    }

    @Test
    void updateShiftScheduleTemplate_whenValidAndAuthorized_updateShiftScheduleTemplate() throws Exception {
        final ShiftScheduleTemplate template = templateRepository
                .save(ShiftScheduleTemplate.builder().name("New Role Name").content("New Role Content").build());

        final ShiftScheduleTemplateDto request = ShiftScheduleTemplateDto.builder()
                .name("New Role Name Updated")
                .content("New Role Content Updated")
                .build();

        mockMvc.perform(post(SHIFT_SCHEDULING_BASE_URI + "/shift-templates/" + template.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

        final ShiftScheduleTemplate templateAfter = templateRepository.findById(template.getId()).orElseThrow();

        assertAll(() -> assertThat(templateAfter.getName()).isEqualTo("New Role Name Updated"),
                () -> assertThat(templateAfter.getContent()).isEqualTo("New Role Content Updated"));
    }

    @Test
    void updateShiftScheduleTemplate_whenNotAuthorized_returnForbidden() throws Exception {
        final ShiftScheduleTemplateDto request = ShiftScheduleTemplateDto.builder()
                .name("New Role Name Updated")
                .content("New Role Content Updated")
                .build();

        mockMvc.perform(post(SHIFT_SCHEDULING_BASE_URI + "/shift-templates/1").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
    }

    @Test
    void getShiftSchedulePdf_whenAuthorized_returnsValidPdf() throws Exception {
        final Employee employee = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe.pdf@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        shiftRepository.save(
                Shift.builder().date(LocalDateTime.of(2026, 5, 1, 0, 0, 0)).department("").employee(employee).build());

        final MvcResult result = mockMvc
                .perform(get(SHIFT_SCHEDULING_BASE_URI + "/shifts/2026/5/pdf").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + getAdminAuthToken()))
                .andExpect(status().isOk())
                .andExpect(resultMatcher -> assertThat(resultMatcher.getResponse().getContentType())
                        .isEqualTo(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(resultMatcher -> assertThat(
                        resultMatcher.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                        .isEqualTo("attachment; filename=schedule_2026_5.pdf"))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content).isNotEmpty();
        String fileHeader = new String(content, 0, 5, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(fileHeader).startsWith("%PDF-");
    }

    @Test
    void getShiftSchedulePdf_whenUserIsEmployee_returnsForbidden() throws Exception {
        final Employee employeeUser = employeeRepository.save(Employee.builder()
                .firstName("Regular")
                .lastName("Employee")
                .email("regular.emp@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        String employeeToken = jwtService.generateAccessToken(employeeUser.getEmail(), employeeUser.getRoles());

        mockMvc.perform(get(SHIFT_SCHEDULING_BASE_URI + "/shifts/2026/5/pdf").header(HttpHeaders.AUTHORIZATION,
                "Bearer " + employeeToken)).andExpect(status().isForbidden());
    }

    @Test
    void getShiftSchedulePdf_whenUnauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get(SHIFT_SCHEDULING_BASE_URI + "/shifts/2026/5/pdf")).andExpect(status().isForbidden());
    }

    @Test
    void getShiftSchedulePdf_whenInvalidDate_returnsBadRequest() throws Exception {
        mockMvc.perform(get(SHIFT_SCHEDULING_BASE_URI + "/shifts/2026/13/pdf").header(HttpHeaders.AUTHORIZATION,
                "Bearer " + getAdminAuthToken())).andExpect(status().isBadRequest());
    }

    @Test
    void solveShiftScheduling_whenInfeasibleStaffingRequirements_returnsBadRequest() throws Exception {
        // Given - only one employee but requiring 10 staff per day (infeasible)
        final Employee employee = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.lonely@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build());

        final ShiftScheduleSolveRequestDto request = ShiftScheduleSolveRequestDto.builder()
                .year(2026)
                .month(5)
                .employeeIds(List.of(employee.getId()))
                .staffingLevels(List.of(10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
                        10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10)) // 31 days with 10 staff required each day
                .dailyHours(List.of(8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                        8, 8, 8))
                .build();

        // When & Then - should return 400 Bad Request due to InfeasibleInstanceException
        mockMvc.perform(post(SHIFT_SCHEDULING_BASE_URI + "/optimized-shift")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
    }
}
