package tuwien.taskinn.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.EmployeeDto;
import tuwien.taskinn.dto.request.CreatePtoRequest;
import tuwien.taskinn.dto.response.ErrorResponseDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.PTO;
import tuwien.taskinn.entity.Shift;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.PTORepository;
import tuwien.taskinn.repo.ShiftRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Employee Controller Integration Tests")
public class EmployeeControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String EMPLOYEE_BASE_URI = BASE_URI + "/employees";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PTORepository ptoRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Test
    @DisplayName("GET /api/v1/employees -> Fails with 403 Forbidden for unauthenticated user")
    public void getAllStaffMembers_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get(EMPLOYEE_BASE_URI)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/employees -> Returns list of employees excluding admins")
    public void getAllStaffMembers_whenAuthenticated_thenReturnsEmployeeList() throws Exception {
        // Create test employees
        Employee staff1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .passwordHash("hashedPassword")
                .build();

        Employee staff2 = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(30))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(35))
                .roles(Set.of(Roles.EMPLOYEE))
                .passwordHash("hashedPassword")
                .build();

        employeeRepository.saveAll(List.of(staff1, staff2));
        String adminToken = getAdminAuthToken();

        var mvcResult =
                mockMvc.perform(get(EMPLOYEE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<EmployeeDto> apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});

        assertAll(() -> assertThat(apiResponse).hasSize(2),
                () -> assertThat(apiResponse).extracting(EmployeeDto::getFirstName)
                        .containsExactlyInAnyOrder("John", "Jane"),
                () -> assertThat(apiResponse).extracting(EmployeeDto::getLastName)
                        .containsExactlyInAnyOrder("Doe", "Smith"),
                () -> assertThat(apiResponse).extracting(EmployeeDto::getEmail)
                        .containsExactlyInAnyOrder("john.doe@taskinn.com", "jane.smith@taskinn.com"),
                () -> assertThat(apiResponse).extracting(EmployeeDto::isActive).containsOnly(true),
                () -> assertThat(apiResponse).extracting(EmployeeDto::getHourlyWage)
                        .usingElementComparator(BigDecimal::compareTo)
                        .containsExactlyInAnyOrder(BigDecimal.valueOf(25), BigDecimal.valueOf(30)),
                () -> assertThat(apiResponse).extracting(EmployeeDto::getHourlyOvertimeWage)
                        .usingElementComparator(BigDecimal::compareTo)
                        .containsExactlyInAnyOrder(BigDecimal.valueOf(30), BigDecimal.valueOf(30)),
                () -> assertThat(apiResponse).extracting(EmployeeDto::getWorkingHoursPerWeek)
                        .usingElementComparator(BigDecimal::compareTo)
                        .containsExactlyInAnyOrder(BigDecimal.valueOf(40), BigDecimal.valueOf(35)));
    }

    @Test
    @DisplayName("GET /api/v1/employees/{id} -> Fails with 403 Forbidden for unauthenticated user")
    public void getEmployeeById_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get(EMPLOYEE_BASE_URI + "/1")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/employees/{id} -> Returns employee when ID exists")
    public void getEmployeeById_whenEmployeeExists_thenReturnsEmployee() throws Exception {
        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .passwordHash("hashedPassword")
                .build();

        Employee savedEmployee = employeeRepository.save(employee);
        String adminToken = getAdminAuthToken();

        var mvcResult = mockMvc.perform(get(EMPLOYEE_BASE_URI + "/" + savedEmployee.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        EmployeeDto apiResponse = objectMapper.readValue(responseBody, EmployeeDto.class);

        assertAll(() -> assertThat(apiResponse.getId()).isEqualTo(savedEmployee.getId()),
                () -> assertThat(apiResponse.getEmail()).isEqualTo("john.doe@taskinn.com"),
                () -> assertThat(apiResponse.getFirstName()).isEqualTo("John"),
                () -> assertThat(apiResponse.getLastName()).isEqualTo("Doe"));
    }

    @Test
    @DisplayName("GET /api/v1/employees/{id} -> Returns 404 when employee not found")
    public void getEmployeeById_whenEmployeeNotFound_thenNotFound() throws Exception {
        String adminToken = getAdminAuthToken();

        mockMvc.perform(get(EMPLOYEE_BASE_URI + "/999").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/employees -> Fails with 403 Forbidden for unauthenticated user")
    public void updateEmployee_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(put(EMPLOYEE_BASE_URI)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id} -> Successfully updates employee")
    public void updateEmployee_whenDataValid_thenUpdatesSuccessfully() throws Exception {
        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .passwordHash("hashedPassword")
                .build();

        Employee savedEmployee = employeeRepository.save(employee);
        String adminToken = getAdminAuthToken();

        EmployeeDto updateDto = EmployeeDto.builder()
                .id(savedEmployee.getId())
                .firstName("John Updated")
                .lastName("Doe Updated")
                .email("john.updated@taskinn.com")
                .active(false)
                .hourlyWage(BigDecimal.valueOf(30))
                .hourlyOvertimeWage(BigDecimal.valueOf(40))
                .workingHoursPerWeek(BigDecimal.valueOf(35))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        var mvcResult = mockMvc.perform(put(EMPLOYEE_BASE_URI + "/" + savedEmployee.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        EmployeeDto apiResponse = objectMapper.readValue(responseBody, EmployeeDto.class);

        assertAll(() -> assertThat(apiResponse.getFirstName()).isEqualTo("John Updated"),
                () -> assertThat(apiResponse.getLastName()).isEqualTo("Doe Updated"),
                () -> assertThat(apiResponse.getEmail()).isEqualTo("john.updated@taskinn.com"),
                () -> assertThat(apiResponse.isActive()).isFalse(),
                () -> assertThat(apiResponse.getHourlyWage()).isEqualTo(BigDecimal.valueOf(30)),
                () -> assertThat(apiResponse.getHourlyOvertimeWage()).isEqualTo(BigDecimal.valueOf(40)),
                () -> assertThat(apiResponse.getWorkingHoursPerWeek()).isEqualTo(BigDecimal.valueOf(35)));
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id} -> Returns 404 when employee not found")
    public void updateEmployee_whenEmployeeNotFound_thenNotFound() throws Exception {
        String adminToken = getAdminAuthToken();

        EmployeeDto updateDto = EmployeeDto.builder()
                .id(999L)
                .firstName("Updated")
                .lastName("Name")
                .email("updated@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(30))
                .hourlyOvertimeWage(BigDecimal.valueOf(40))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        mockMvc.perform(put(EMPLOYEE_BASE_URI + "/999").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id} -> Returns 400 when email already in use")
    public void updateEmployee_whenEmailAlreadyInUse_thenBadRequest() throws Exception {
        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .passwordHash("hashedPassword")
                .build();

        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(30))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(35))
                .roles(Set.of(Roles.EMPLOYEE))
                .passwordHash("hashedPassword")
                .build();

        Employee savedEmployee1 = employeeRepository.save(employee1);
        employeeRepository.save(employee2);
        String adminToken = getAdminAuthToken();

        EmployeeDto updateDto = EmployeeDto.builder()
                .id(savedEmployee1.getId())
                .firstName("John")
                .lastName("Doe")
                .email("jane.smith@taskinn.com") // Trying to use Jane's email
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        mockMvc.perform(put(EMPLOYEE_BASE_URI + "/" + savedEmployee1.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id} -> Returns 400 Bad Request when employee has zero hourly wage")
    void updateEmployee_whenZeroHourlyWage_returnsBadRequest() throws Exception {
        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .passwordHash("hashedPassword")
                .build();

        Employee savedEmployee = employeeRepository.save(employee);
        String adminToken = getAdminAuthToken();

        EmployeeDto requestDto = EmployeeDto.builder()
                .id(savedEmployee.getId())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .roles(Set.of(Roles.EMPLOYEE))
                .hourlyWage(BigDecimal.ZERO)
                .hourlyOvertimeWage(BigDecimal.ZERO)
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .build();

        // When
        Map<String, String> validationErrors =
                performBadRequestAndGetValidationErrors(put(EMPLOYEE_BASE_URI + "/" + savedEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)));

        // Then
        assertAll(() -> assertThat(validationErrors).hasSize(1), () -> assertThat(validationErrors)
                .containsEntry("hourlyWage", "Hourly wage must be greater than 0 for non-admin users"));
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id} -> Returns 400 Bad Request when employee has zero working hours per week")
    void updateEmployee_whenZeroWorkingHours_returnsBadRequest() throws Exception {
        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .passwordHash("hashedPassword")
                .build();

        Employee savedEmployee = employeeRepository.save(employee);
        String adminToken = getAdminAuthToken();

        EmployeeDto requestDto = EmployeeDto.builder()
                .id(savedEmployee.getId())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@taskinn.com")
                .active(true)
                .roles(Set.of(Roles.EMPLOYEE))
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.ZERO)
                .build();

        // When
        Map<String, String> validationErrors =
                performBadRequestAndGetValidationErrors(put(EMPLOYEE_BASE_URI + "/" + savedEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)));

        // Then
        assertAll(() -> assertThat(validationErrors).hasSize(1),
                () -> assertThat(validationErrors).containsEntry("workingHoursPerWeek",
                        "Working hours per week must be greater than 0 for non-admin users"));
    }

    // --- PTO Endpoints ---
    @Test
    @DisplayName("POST /api/v1/employees/pto -> Creates PTO for authenticated employee")
    void createPto_whenValid_returnsCreatedPto() throws Exception {
        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("emp1@taskinn.com")
                .active(true)
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(Set.of(Roles.EMPLOYEE))
                .passwordHash("hashedPassword")
                .build();

        Employee savedEmployee = employeeRepository.save(employee);
        String token = generateToken(savedEmployee);

        CreatePtoRequest request = CreatePtoRequest.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .build();

        var mvcResult =
                mockMvc.perform(post(EMPLOYEE_BASE_URI + "/pto").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk()).andReturn();

        PTO saved = ptoRepository.findByEmployeeEmail(savedEmployee.getEmail()).stream().findFirst().orElseThrow();
        String body = mvcResult.getResponse().getContentAsString();

        assertAll(() -> assertThat(saved.getStartDate()).isEqualTo(request.getStartDate()),
                () -> assertThat(saved.getEndDate()).isEqualTo(request.getEndDate()),
                () -> assertThat(body).contains("startDate", "endDate"));
    }

    @Test
    @DisplayName("POST /api/v1/employees/pto -> Returns 400 when overlaps existing PTO")
    void createPto_whenOverlapsExisting_returnsBadRequest() throws Exception {
        Employee employee = createAndSaveEmployee("emp2@taskinn.com", Roles.EMPLOYEE);
        String token = generateToken(employee);

        PTO existing = PTO.builder()
                .employee(employee)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .build();
        ptoRepository.save(existing);

        CreatePtoRequest overlapping = CreatePtoRequest.builder()
                .startDate(LocalDate.now().plusDays(1)) // touches/overlaps end
                .endDate(LocalDate.now().plusDays(3))
                .build();

        mockMvc.perform(post(EMPLOYEE_BASE_URI + "/pto").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(overlapping))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/v1/employees/pto/{id} -> Deletes PTO of current user")
    void deletePto_whenOwnedByUser_deletesSuccessfully() throws Exception {
        Employee employee = createAndSaveEmployee("emp3@taskinn.com", Roles.EMPLOYEE);
        String token = generateToken(employee);

        PTO existing = PTO.builder()
                .employee(employee)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .build();
        PTO saved = ptoRepository.save(existing);

        mockMvc.perform(delete(EMPLOYEE_BASE_URI + "/pto/" + saved.getId()).header(HttpHeaders.AUTHORIZATION,
                "Bearer " + token)).andExpect(status().isOk());

        assertThat(ptoRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/employees/shifts/{year}/{month} -> Returns shifts for current employee")
    void getMyShifts_whenShiftsExist_returnsDayIndices() throws Exception {
        Employee employee = createAndSaveEmployee("shifted@taskinn.com", Roles.EMPLOYEE);
        String token = generateToken(employee);

        shiftRepository.save(Shift.builder()
                .employee(employee)
                .department("frontdesk")
                .date(LocalDateTime.of(2026, 5, 2, 0, 0))
                .build());
        shiftRepository.save(Shift.builder()
                .employee(employee)
                .department("frontdesk")
                .date(LocalDateTime.of(2026, 5, 10, 0, 0))
                .build());

        var mvcResult = mockMvc
                .perform(get(EMPLOYEE_BASE_URI + "/shifts/2026/5").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        List<Integer> response =
                objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(response).containsExactlyInAnyOrder(1, 9);
    }

    @Test
    @DisplayName("GET /api/v1/employees/shifts/{year}/{month} -> Returns 400 for invalid month")
    void getMyShifts_whenMonthInvalid_returnsBadRequest() throws Exception {
        String token = getEmployeeAuthToken();

        mockMvc.perform(get(EMPLOYEE_BASE_URI + "/shifts/2026/13").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    /**
     * Utility method to perform a bad request and extract the validation errors as field->message pairs. This helps
     * verify that proper validation feedback is provided to clients.
     *
     * @param requestBuilder the MockMvc request builder for the bad request
     * @return Map of field names to their validation error messages
     * @throws Exception if the request fails or response parsing fails
     */
    private Map<String, String> performBadRequestAndGetValidationErrors(RequestBuilder requestBuilder)
            throws Exception {
        var mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto errorResponse = objectMapper.readValue(responseBody, new TypeReference<ErrorResponseDto>() {});

        return errorResponse.getValidationErrors()
                .stream()
                .collect(Collectors.toMap(ErrorResponseDto.ValidationError::getField,
                        ErrorResponseDto.ValidationError::getMessage));
    }
}
