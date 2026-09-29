package tuwien.taskinn.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.request.SignUpRequestDto;
import tuwien.taskinn.dto.response.ErrorResponseDto;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.repo.EmployeeRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Admin Controller Integration Tests")
public class AdminControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String ADMIN_BASE_URI = BASE_URI + "/admin";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @DisplayName("POST /admin-account -> Fails with 403 Forbidden for unauthenticated user")
    public void createNewAdminAccount_givenNothing_whenUserNotAuthenticated_thenDontAllowAccess() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(post(ADMIN_BASE_URI + "/account")).andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    @Test
    @DisplayName("POST /admin-account -> Fails with 403 Forbidden for employee role")
    public void createNewAdminAccount_givenNothing_whenUserEmployee_thenDontAllowAccess() throws Exception {

        String employeeToken = getEmployeeAuthToken();

        SignUpRequestDto requestDto = SignUpRequestDto.builder()
                .firstName("New")
                .lastName("Admin")
                .email("new_admin@taskinn.com")
                .password("StrongPassword123!")
                .roles(Set.of(Roles.ADMIN))
                .build();

        MvcResult mvcResult = this.mockMvc
                .perform(post(ADMIN_BASE_URI + "/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    @Test
    @DisplayName("POST /admin-account -> Successfully creates new admin when authenticated as ADMIN")
    void createNewAdminAccount_whenAuthenticatedAsAdmin_thenSucceeds() throws Exception {
        String adminToken = getAdminAuthToken();

        SignUpRequestDto requestDto = SignUpRequestDto.builder()
                .firstName("New")
                .lastName("Admin")
                .email("new_admin@taskinn.com")
                .password("StrongPassword123!")
                .roles(Set.of(Roles.ADMIN))
                .build();

        mockMvc.perform(post(ADMIN_BASE_URI + "/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isCreated());

        boolean userExists = employeeRepository.existsByEmail("new_admin@taskinn.com");
        assertThat(userExists).isTrue();
    }

    @Test
    @DisplayName("POST /employee-account -> Successfully creates new employee when authenticated as ADMIN")
    void createNewEmployeeAccount_whenAuthenticatedAsAdmin_thenSucceeds() throws Exception {
        String adminToken = getAdminAuthToken();

        SignUpRequestDto requestDto = SignUpRequestDto.builder()
                .firstName("New")
                .lastName("Employee")
                .email("new_employee@taskinn.com")
                .password("StrongPassword123!")
                .roles(Set.of(Roles.EMPLOYEE))
                .hourlyWage(BigDecimal.valueOf(25))
                .hourlyOvertimeWage(BigDecimal.valueOf(30))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .build();

        mockMvc.perform(post(ADMIN_BASE_URI + "/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isCreated());

        boolean userExists = employeeRepository.existsByEmail("new_employee@taskinn.com");
        assertThat(userExists).isTrue();
    }

    @Test
    @DisplayName("POST /employee-account -> Returns 400 Bad Request when employee has zero hourly wage")
    void createNewEmployeeAccount_whenZeroHourlyWage_returnsBadRequest() throws Exception {
        String adminToken = getAdminAuthToken();

        SignUpRequestDto requestDto = SignUpRequestDto.builder()
                .firstName("Invalid")
                .lastName("Employee")
                .email("invalid_employee@taskinn.com")
                .password("StrongPassword123!")
                .roles(Set.of(Roles.EMPLOYEE))
                .hourlyWage(BigDecimal.ZERO)
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .build();

        // When
        Map<String, String> validationErrors = performBadRequestAndGetValidationErrors(
                post(ADMIN_BASE_URI + "/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)));

        // Then
        assertAll(() -> assertThat(validationErrors).hasSize(1), () -> assertThat(validationErrors)
                .containsEntry("hourlyWage", "Hourly wage must be greater than 0 for non-admin users"));
    }

    @Test
    @DisplayName("POST /employee-account -> Returns 400 Bad Request when employee has zero working hours per week")
    void createNewEmployeeAccount_whenZeroWorkingHoursPerWeek_returnsBadRequest() throws Exception {
        String adminToken = getAdminAuthToken();

        SignUpRequestDto requestDto = SignUpRequestDto.builder()
                .firstName("Invalid")
                .lastName("Employee")
                .email("invalid_employee@taskinn.com")
                .password("StrongPassword123!")
                .roles(Set.of(Roles.EMPLOYEE))
                .hourlyWage(BigDecimal.valueOf(40))
                .workingHoursPerWeek(BigDecimal.ZERO)
                .build();

        // When
        Map<String, String> validationErrors = performBadRequestAndGetValidationErrors(
                post(ADMIN_BASE_URI + "/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)));

        // Then
        assertAll(() -> assertThat(validationErrors).hasSize(1),
                () -> assertThat(validationErrors).containsEntry("workingHoursPerWeek",
                        "Working hours per week must be greater than 0 for non-admin users"));
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
