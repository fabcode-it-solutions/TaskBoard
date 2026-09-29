package tuwien.taskinn.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.response.BookingStatisticsResponseDto;
import tuwien.taskinn.dto.response.CleaningStatisticsResponseDto;
import tuwien.taskinn.dto.response.EmployeeStatisticsResponseDto;
import tuwien.taskinn.dto.response.MinibarStatisticsResponseDto;
import tuwien.taskinn.dto.response.ErrorResponseDto;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Statistics Controller Integration Tests")
public class StatisticsControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String STATISTICS_BASE_URI = BASE_URI + "/statistics";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /statistics/minibar - Returns statistics for ADMIN without query params")
    void getMinibarStatistics_whenAdminNoParams_returnsOk() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc
                .perform(
                        get(STATISTICS_BASE_URI + "/minibar").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String body = mvcResult.getResponse().getContentAsString();
        MinibarStatisticsResponseDto dto = objectMapper.readValue(body, MinibarStatisticsResponseDto.class);

        assertAll(() -> assertThat(dto).isNotNull(), () -> assertThat(dto.getSalesByEarnings()).isNotNull(),
                () -> assertThat(dto.getSalesByQuantity()).isNotNull(),
                () -> assertThat(dto.getDailyItemSales()).isNotNull());
    }

    @Test
    @DisplayName("GET /statistics/minibar - Returns statistics for ADMIN with valid parameters")
    void getMinibarStatistics_whenAdminWithParams_returnsOk() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc
                .perform(get(STATISTICS_BASE_URI + "/minibar").queryParam("periodFrom", "2025-01-01")
                        .queryParam("periodTo", "2025-12-31")
                        .queryParam("itemIds", "1,2,3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String body = mvcResult.getResponse().getContentAsString();
        MinibarStatisticsResponseDto dto = objectMapper.readValue(body, MinibarStatisticsResponseDto.class);

        assertAll(() -> assertThat(dto).isNotNull(), () -> assertThat(dto.getSalesByEarnings()).isNotNull(),
                () -> assertThat(dto.getSalesByQuantity()).isNotNull(),
                () -> assertThat(dto.getDailyItemSales()).isNotNull());
    }

    @Test
    @DisplayName("GET /statistics/minibar - Returns 403 Forbidden when unauthenticated")
    void getMinibarStatistics_whenUnauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get(STATISTICS_BASE_URI + "/minibar")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /statistics/minibar - Returns 400 Bad Request when only periodFrom provided")
    void getMinibarStatistics_whenOnlyPeriodFrom_returnsBadRequest() throws Exception {
        String adminToken = getAdminAuthToken();

        var mvcResult = mockMvc
                .perform(get(STATISTICS_BASE_URI + "/minibar").queryParam("periodFrom", "2025-01-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto err = objectMapper.readValue(body, ErrorResponseDto.class);

        assertThat(err.getValidationErrors()).hasSize(1);
        assertThat(err.getValidationErrors().get(0).getMessage()).containsIgnoringCase("periodFrom");
    }

    @Test
    @DisplayName("GET /statistics/minibar - Returns 400 when periodFrom after periodTo")
    void getMinibarStatistics_whenFromAfterTo_returnsBadRequest() throws Exception {
        String adminToken = getAdminAuthToken();

        var mvcResult = mockMvc
                .perform(get(STATISTICS_BASE_URI + "/minibar").queryParam("periodFrom", "2025-12-31")
                        .queryParam("periodTo", "2025-01-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto err = objectMapper.readValue(body, ErrorResponseDto.class);

        assertThat(err.getValidationErrors()).hasSize(1);
        assertThat(err.getValidationErrors().get(0).getMessage()).containsIgnoringCase("on or before");
    }

    @Test
    @DisplayName("GET /statistics/employees - Returns statistics for ADMIN without query params (Default Employee)")
    void getEmployeeStatistics_whenAdminNoParams_returnsOk() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc.perform(
                get(STATISTICS_BASE_URI + "/employees").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String body = mvcResult.getResponse().getContentAsString();
        List<EmployeeStatisticsResponseDto> dtos = objectMapper.readValue(body, new TypeReference<>() {});

        assertAll(() -> assertThat(dtos).isNotNull(),
                // Should return at least one employee if data exists, or empty list if no employees
                // Assuming test data exists or empty list is valid response
                () -> assertThat(dtos).isInstanceOf(List.class));
    }

    @Test
    @DisplayName("GET /statistics/employees - Returns statistics for specific employee IDs")
    void getEmployeeStatistics_whenSpecificIds_returnsOk() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();
        // Assuming employee IDs 1 and 2 might exist or not, validation shouldn't fail even if empty

        // When
        var mvcResult = mockMvc
                .perform(get(STATISTICS_BASE_URI + "/employees").queryParam("employeeIds", "1,2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String body = mvcResult.getResponse().getContentAsString();
        List<EmployeeStatisticsResponseDto> dtos = objectMapper.readValue(body, new TypeReference<>() {});

        assertThat(dtos).isNotNull();
    }

    @Test
    @DisplayName("GET /statistics/employees - Returns statistics with valid date range")
    void getEmployeeStatistics_whenValidDateRange_returnsOk() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc
                .perform(get(STATISTICS_BASE_URI + "/employees").queryParam("periodFrom", "2025-01-01")
                        .queryParam("periodTo", "2025-12-31")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String body = mvcResult.getResponse().getContentAsString();
        List<EmployeeStatisticsResponseDto> dtos = objectMapper.readValue(body, new TypeReference<>() {});

        assertThat(dtos).isNotNull();
    }

    @Test
    @DisplayName("GET /statistics/employees - Returns 403 Forbidden when unauthenticated")
    void getEmployeeStatistics_whenUnauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get(STATISTICS_BASE_URI + "/employees")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /statistics/employees - Returns 400 Bad Request when only periodFrom provided")
    void getEmployeeStatistics_whenOnlyPeriodFrom_returnsBadRequest() throws Exception {
        String adminToken = getAdminAuthToken();

        var mvcResult = mockMvc
                .perform(get(STATISTICS_BASE_URI + "/employees").queryParam("periodFrom", "2025-01-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto err = objectMapper.readValue(body, ErrorResponseDto.class);

        assertThat(err.getValidationErrors()).hasSize(1);
        assertThat(err.getValidationErrors().get(0).getMessage()).containsIgnoringCase("periodFrom");
    }

    @Test
    @DisplayName("GET /statistics/employees - Returns 400 when periodFrom after periodTo")
    void getEmployeeStatistics_whenFromAfterTo_returnsBadRequest() throws Exception {
        String adminToken = getAdminAuthToken();

        var mvcResult = mockMvc
                .perform(get(STATISTICS_BASE_URI + "/employees").queryParam("periodFrom", "2025-12-31")
                        .queryParam("periodTo", "2025-01-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto err = objectMapper.readValue(body, ErrorResponseDto.class);

        assertThat(err.getValidationErrors()).hasSize(1);
        assertThat(err.getValidationErrors().get(0).getMessage()).containsIgnoringCase("on or before");
    }

    @Test
    @DisplayName("GET /statistics/cleaning - Returns statistics for ADMIN without query params")
    void getCleaningStatistics_whenAdminNoParams_returnsOk() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc.perform(
                get(STATISTICS_BASE_URI + "/cleaning").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String body = mvcResult.getResponse().getContentAsString();
        CleaningStatisticsResponseDto dto = objectMapper.readValue(body, CleaningStatisticsResponseDto.class);

        assertAll(() -> assertThat(dto).isNotNull(),
                () -> assertThat(dto.getAvgDurationByTaskAndRoomType()).isNotNull(),
                () -> assertThat(dto.getAvgHoursByMonth()).isNotNull(),
                () -> assertThat(dto.getBlockedTasksPercentage()).isNotNull());
    }

    @Test
    @DisplayName("GET /statistics/cleaning - Returns statistics for ADMIN with valid periodFrom/periodTo")
    void getCleaningStatistics_whenAdminWithValidPeriod_returnsOk() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc
                .perform(get(STATISTICS_BASE_URI + "/cleaning").queryParam("periodFrom", "2025-01-01")
                        .queryParam("periodTo", "2025-12-31")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String body = mvcResult.getResponse().getContentAsString();
        CleaningStatisticsResponseDto dto = objectMapper.readValue(body, CleaningStatisticsResponseDto.class);

        assertAll(() -> assertThat(dto).isNotNull(),
                () -> assertThat(dto.getAvgDurationByTaskAndRoomType()).isNotNull(),
                () -> assertThat(dto.getAvgHoursByMonth()).isNotNull(),
                () -> assertThat(dto.getBlockedTasksPercentage()).isNotNull());
    }

    @Test
    @DisplayName("GET /statistics/cleaning - Returns 403 Forbidden when unauthenticated")
    void getCleaningStatistics_whenUnauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get(STATISTICS_BASE_URI + "/cleaning")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /statistics/cleaning - Returns 400 Bad Request when only periodFrom provided")
    void getCleaningStatistics_whenOnlyPeriodFrom_returnsBadRequest() throws Exception {
        String adminToken = getAdminAuthToken();

        var mvcResult = mockMvc
                .perform(get(STATISTICS_BASE_URI + "/cleaning").queryParam("periodFrom", "2025-01-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto err = objectMapper.readValue(body, ErrorResponseDto.class);

        assertThat(err.getValidationErrors()).isNotNull();
        assertThat(err.getValidationErrors()).hasSize(1);
        assertThat(err.getValidationErrors().get(0).getMessage()).containsIgnoringCase("periodFrom");
    }

    @Test
    @DisplayName("GET /statistics/cleaning - Returns 400 Bad Request for invalid LocalDate query params")
    void getCleaningStatistics_whenInvalidDateParam_returnsBadRequest() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When / Then
        mockMvc.perform(get(STATISTICS_BASE_URI + "/cleaning").queryParam("periodFrom", "01-01-2025")
                .queryParam("periodTo", "2025-12-31")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /statistics/cleaning - Returns 400 when periodFrom after periodTo")
    void getCleaningStatistics_whenFromAfterTo_returnsBadRequest() throws Exception {
        String adminToken = getAdminAuthToken();

        var mvcResult = mockMvc
                .perform(get(STATISTICS_BASE_URI + "/cleaning").queryParam("periodFrom", "2025-12-31")
                        .queryParam("periodTo", "2025-01-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto err = objectMapper.readValue(body, ErrorResponseDto.class);

        assertThat(err.getValidationErrors()).isNotNull();
        assertThat(err.getValidationErrors()).hasSize(1);
        assertThat(err.getValidationErrors().get(0).getMessage()).containsIgnoringCase("on or before");
    }

    @Test
    @DisplayName("GET /statistics/bookings - Returns statistics for ADMIN without query params")
    void getBookingStatistics_whenAdminNoParams_returnsOk() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc.perform(
                get(STATISTICS_BASE_URI + "/bookings").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String body = mvcResult.getResponse().getContentAsString();
        BookingStatisticsResponseDto dto = objectMapper.readValue(body, BookingStatisticsResponseDto.class);

        assertAll(() -> assertThat(dto).isNotNull(), () -> assertThat(dto.getAllBookingsByDay()).isNotNull(),
                () -> assertThat(dto.getMonthBookingsByDay()).isNotNull(),
                () -> assertThat(dto.getBookingsByMonth()).isNotNull(),
                () -> assertThat(dto.getBookingsByRoomType()).isNotNull());
    }

    @Test
    @DisplayName("GET /statistics/bookings - Returns statistics for ADMIN with periodFrom/periodTo/month")
    void getBookingStatistics_whenAdminWithParams_returnsOk() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc
                .perform(get(STATISTICS_BASE_URI + "/bookings").queryParam("periodFrom", "2025-01-01")
                        .queryParam("periodTo", "2025-12-31")
                        .queryParam("month", "2025-03")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String body = mvcResult.getResponse().getContentAsString();
        BookingStatisticsResponseDto dto = objectMapper.readValue(body, BookingStatisticsResponseDto.class);

        assertAll(() -> assertThat(dto).isNotNull(), () -> assertThat(dto.getAllBookingsByDay()).isNotNull(),
                () -> assertThat(dto.getMonthBookingsByDay()).isNotNull(),
                () -> assertThat(dto.getBookingsByMonth()).isNotNull(),
                () -> assertThat(dto.getBookingsByRoomType()).isNotNull());
    }

    @Test
    @DisplayName("GET /statistics/bookings - Returns 403 Forbidden when unauthenticated")
    void getBookingStatistics_whenUnauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get(STATISTICS_BASE_URI + "/bookings")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /statistics/bookings - Returns 400 Bad Request for invalid LocalDate query params")
    void getBookingStatistics_whenInvalidDateParam_returnsBadRequest() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When / Then
        // periodFrom must be ISO yyyy-MM-dd because of @DateTimeFormat(iso = ISO.DATE)
        mockMvc.perform(get(STATISTICS_BASE_URI + "/bookings").queryParam("periodFrom", "01-01-2025")
                .queryParam("periodTo", "2025-12-31")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /statistics/bookings - Returns 400 Bad Request for invalid YearMonth query param")
    void getBookingStatistics_whenInvalidYearMonth_returnsBadRequest() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When / Then
        // The frontend sends YYYY-MM (e.g. 2025-03). Anything else should fail conversion.
        mockMvc.perform(get(STATISTICS_BASE_URI + "/bookings").queryParam("month", "2025/03")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /statistics/bookings - Returns 400 and validation message when periodFrom provided without periodTo")
    void getBookingStatistics_whenMissingPeriodTo_returnsBadRequestWithMessage() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc
                .perform(get(STATISTICS_BASE_URI + "/bookings").queryParam("periodFrom", "2025-01-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto err = objectMapper.readValue(body, ErrorResponseDto.class);

        assertAll(() -> assertThat(err).isNotNull(),
                () -> assertThat(err.getMessage()).containsIgnoringCase("fields have an error"),
                () -> assertThat(err.getValidationErrors()).isNotNull(),
                () -> assertThat(err.getValidationErrors()).hasSize(1),
                () -> assertThat(err.getValidationErrors().getFirst().getField()).isEqualTo("booking statistics"),
                () -> assertThat(err.getValidationErrors().getFirst().getMessage()).contains("periodFrom"),
                () -> assertThat(err.getValidationErrors().getFirst().getMessage()).contains("periodTo"));
    }

    @SuppressWarnings("unused")
    private Map<String, String> extractValidationErrors(String responseBody) throws Exception {
        ErrorResponseDto error = objectMapper.readValue(responseBody, ErrorResponseDto.class);
        if (error.getValidationErrors() == null)
            return Map.of();
        return error.getValidationErrors()
                .stream()
                .collect(java.util.stream.Collectors.toMap(ErrorResponseDto.ValidationError::getField,
                        ErrorResponseDto.ValidationError::getMessage));
    }
}
