package tuwien.taskinn.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.request.StaircaseRequestDto;
import tuwien.taskinn.dto.response.StaircaseDto;
import tuwien.taskinn.entity.Staircase;

@DisplayName("Staircase Controller Integration Tests")
public class StaircaseControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String STAIRCASE_BASE_URI = BASE_URI + "/staircase";

    // ===== GET / (getAllStaircases) =====

    @Test
    @DisplayName("GET /api/v1/staircase -> Returns 200 OK with list for authenticated user")
    void getAllStaircases_whenAuthenticated_returnsOkWithList() throws Exception {
        Staircase staircase1 = Staircase.builder().floor(1).x(100.0).y(200.0).name("Staircase A").active(true).build();
        Staircase staircase2 = Staircase.builder().floor(2).x(150.0).y(250.0).name("Staircase B").active(true).build();
        staircaseRepository.saveAll(List.of(staircase1, staircase2));

        String adminToken = getAdminAuthToken();

        var mvcResult =
                mockMvc.perform(get(STAIRCASE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<StaircaseDto> staircases = objectMapper.readValue(responseBody, new TypeReference<>() {});

        assertAll(() -> assertThat(staircases).hasSize(2),
                () -> assertThat(staircases).extracting(StaircaseDto::getName)
                        .containsExactlyInAnyOrder("Staircase A", "Staircase B"));
    }

    @Test
    @DisplayName("GET /api/v1/staircase -> Returns 403 Forbidden for unauthenticated user")
    void getAllStaircases_whenUnauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get(STAIRCASE_BASE_URI)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/staircase -> Returns only active staircases")
    void getAllStaircases_onlyReturnsActiveStaircases() throws Exception {
        Staircase activeStaircase = Staircase.builder().floor(1).x(100.0).y(200.0).name("Active").active(true).build();
        Staircase inactiveStaircase =
                Staircase.builder().floor(2).x(150.0).y(250.0).name("Inactive").active(false).build();
        staircaseRepository.saveAll(List.of(activeStaircase, inactiveStaircase));

        String adminToken = getAdminAuthToken();

        var mvcResult =
                mockMvc.perform(get(STAIRCASE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<StaircaseDto> staircases = objectMapper.readValue(responseBody, new TypeReference<>() {});

        assertAll(() -> assertThat(staircases).hasSize(1),
                () -> assertThat(staircases.get(0).getName()).isEqualTo("Active"));
    }

    // ===== GET /floor/{floor} =====

    @Test
    @DisplayName("GET /api/v1/staircase/floor/{floor} -> Returns 200 OK with filtered list for valid floor")
    void getStaircasesByFloor_whenValidFloor_returnsFilteredList() throws Exception {
        Staircase floor1Staircase =
                Staircase.builder().floor(1).x(100.0).y(200.0).name("Floor 1 Staircase").active(true).build();
        Staircase floor2Staircase =
                Staircase.builder().floor(2).x(150.0).y(250.0).name("Floor 2 Staircase").active(true).build();
        staircaseRepository.saveAll(List.of(floor1Staircase, floor2Staircase));

        String adminToken = getAdminAuthToken();

        var mvcResult = mockMvc
                .perform(get(STAIRCASE_BASE_URI + "/floor/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<StaircaseDto> staircases = objectMapper.readValue(responseBody, new TypeReference<>() {});

        assertAll(() -> assertThat(staircases).hasSize(1),
                () -> assertThat(staircases.get(0).getName()).isEqualTo("Floor 1 Staircase"),
                () -> assertThat(staircases.get(0).getFloor()).isEqualTo(1));
    }

    @Test
    @DisplayName("GET /api/v1/staircase/floor/{floor} -> Returns 200 OK with empty list for floor with no staircases")
    void getStaircasesByFloor_whenNoStaircasesOnFloor_returnsEmptyList() throws Exception {
        Staircase staircase =
                Staircase.builder().floor(1).x(100.0).y(200.0).name("Floor 1 Staircase").active(true).build();
        staircaseRepository.save(staircase);

        String adminToken = getAdminAuthToken();

        var mvcResult = mockMvc
                .perform(get(STAIRCASE_BASE_URI + "/floor/5").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<StaircaseDto> staircases = objectMapper.readValue(responseBody, new TypeReference<>() {});

        assertThat(staircases).isEmpty();
    }

    // ===== GET /{id} =====

    @Test
    @DisplayName("GET /api/v1/staircase/{id} -> Returns 200 OK with staircase for valid ID")
    void getStaircaseById_whenValidId_returnsStaircase() throws Exception {
        Staircase staircase =
                Staircase.builder().floor(1).x(100.0).y(200.0).name("Main Staircase").active(true).build();
        Staircase savedStaircase = staircaseRepository.save(staircase);

        String adminToken = getAdminAuthToken();

        var mvcResult = mockMvc.perform(get(STAIRCASE_BASE_URI + "/" + savedStaircase.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        StaircaseDto result = objectMapper.readValue(responseBody, StaircaseDto.class);

        assertAll(() -> assertThat(result.getId()).isEqualTo(savedStaircase.getId()),
                () -> assertThat(result.getName()).isEqualTo("Main Staircase"),
                () -> assertThat(result.getFloor()).isEqualTo(1), () -> assertThat(result.getX()).isEqualTo(100.0),
                () -> assertThat(result.getY()).isEqualTo(200.0), () -> assertThat(result.isActive()).isTrue());
    }

    @Test
    @DisplayName("GET /api/v1/staircase/{id} -> Returns 404 Not Found for invalid ID")
    void getStaircaseById_whenInvalidId_returnsNotFound() throws Exception {
        String adminToken = getAdminAuthToken();

        mockMvc.perform(get(STAIRCASE_BASE_URI + "/99999").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ===== POST / (createStaircase) =====

    @Test
    @DisplayName("POST /api/v1/staircase -> Returns 201 Created for admin with valid data")
    void createStaircase_whenAdminWithValidData_returnsCreated() throws Exception {
        String adminToken = getAdminAuthToken();

        StaircaseRequestDto requestDto =
                StaircaseRequestDto.builder().floor(3).x(75.0).y(125.0).name("New Staircase").active(true).build();

        var mvcResult = mockMvc
                .perform(post(STAIRCASE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        StaircaseDto result = objectMapper.readValue(responseBody, StaircaseDto.class);

        assertAll(() -> assertThat(result.getId()).isNotNull(), () -> assertThat(result.getFloor()).isEqualTo(3),
                () -> assertThat(result.getX()).isEqualTo(75.0), () -> assertThat(result.getY()).isEqualTo(125.0),
                () -> assertThat(result.getName()).isEqualTo("New Staircase"),
                () -> assertThat(result.isActive()).isTrue());
    }

    @Test
    @DisplayName("POST /api/v1/staircase -> Returns 400 Bad Request for null floor")
    void createStaircase_whenNullFloor_returnsBadRequest() throws Exception {
        String adminToken = getAdminAuthToken();

        StaircaseRequestDto requestDto = StaircaseRequestDto.builder()
                .floor(null)
                .x(75.0)
                .y(125.0)
                .name("Invalid Staircase")
                .active(true)
                .build();

        mockMvc.perform(post(STAIRCASE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/staircase -> Returns 400 Bad Request for negative floor")
    void createStaircase_whenNegativeFloor_returnsBadRequest() throws Exception {
        String adminToken = getAdminAuthToken();

        StaircaseRequestDto requestDto =
                StaircaseRequestDto.builder().floor(-1).x(75.0).y(125.0).name("Invalid Staircase").active(true).build();

        mockMvc.perform(post(STAIRCASE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/staircase -> Returns 400 Bad Request for null x coordinate")
    void createStaircase_whenNullXCoordinate_returnsBadRequest() throws Exception {
        String adminToken = getAdminAuthToken();

        StaircaseRequestDto requestDto =
                StaircaseRequestDto.builder().floor(1).x(null).y(125.0).name("Invalid Staircase").active(true).build();

        mockMvc.perform(post(STAIRCASE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/staircase -> Returns 400 Bad Request for null y coordinate")
    void createStaircase_whenNullYCoordinate_returnsBadRequest() throws Exception {
        String adminToken = getAdminAuthToken();

        StaircaseRequestDto requestDto =
                StaircaseRequestDto.builder().floor(1).x(75.0).y(null).name("Invalid Staircase").active(true).build();

        mockMvc.perform(post(STAIRCASE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/staircase -> Returns 400 Bad Request for null active field")
    void createStaircase_whenNullActive_returnsBadRequest() throws Exception {
        String adminToken = getAdminAuthToken();

        StaircaseRequestDto requestDto =
                StaircaseRequestDto.builder().floor(1).x(75.0).y(125.0).name("Invalid Staircase").active(null).build();

        mockMvc.perform(post(STAIRCASE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/staircase -> Returns 403 Forbidden for non-admin user")
    void createStaircase_whenNonAdmin_returnsForbidden() throws Exception {
        String employeeToken = getEmployeeAuthToken();

        StaircaseRequestDto requestDto =
                StaircaseRequestDto.builder().floor(3).x(75.0).y(125.0).name("New Staircase").active(true).build();

        mockMvc.perform(post(STAIRCASE_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/staircase -> Returns 403 Forbidden for unauthenticated user")
    void createStaircase_whenUnauthenticated_returnsForbidden() throws Exception {
        StaircaseRequestDto requestDto =
                StaircaseRequestDto.builder().floor(3).x(75.0).y(125.0).name("New Staircase").active(true).build();

        mockMvc.perform(post(STAIRCASE_BASE_URI).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isForbidden());
    }

    // ===== PUT /{id} (updateStaircase) =====

    @Test
    @DisplayName("PUT /api/v1/staircase/{id} -> Returns 200 OK with updated staircase for admin")
    void updateStaircase_whenAdminWithValidData_returnsUpdated() throws Exception {
        Staircase staircase = Staircase.builder().floor(1).x(100.0).y(200.0).name("Original Name").active(true).build();
        Staircase savedStaircase = staircaseRepository.save(staircase);

        String adminToken = getAdminAuthToken();

        StaircaseRequestDto updateDto =
                StaircaseRequestDto.builder().floor(5).x(250.0).y(350.0).name("Updated Name").active(true).build();

        var mvcResult = mockMvc.perform(put(STAIRCASE_BASE_URI + "/" + savedStaircase.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        StaircaseDto result = objectMapper.readValue(responseBody, StaircaseDto.class);

        assertAll(() -> assertThat(result.getId()).isEqualTo(savedStaircase.getId()),
                () -> assertThat(result.getFloor()).isEqualTo(5), () -> assertThat(result.getX()).isEqualTo(250.0),
                () -> assertThat(result.getY()).isEqualTo(350.0),
                () -> assertThat(result.getName()).isEqualTo("Updated Name"));
    }

    @Test
    @DisplayName("PUT /api/v1/staircase/{id} -> Returns 404 Not Found for invalid ID")
    void updateStaircase_whenInvalidId_returnsNotFound() throws Exception {
        String adminToken = getAdminAuthToken();

        StaircaseRequestDto updateDto =
                StaircaseRequestDto.builder().floor(5).x(250.0).y(350.0).name("Updated Name").active(true).build();

        mockMvc.perform(put(STAIRCASE_BASE_URI + "/99999").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/staircase/{id} -> Returns 403 Forbidden for non-admin user")
    void updateStaircase_whenNonAdmin_returnsForbidden() throws Exception {
        Staircase staircase = Staircase.builder().floor(1).x(100.0).y(200.0).name("Original").active(true).build();
        Staircase savedStaircase = staircaseRepository.save(staircase);

        String employeeToken = getEmployeeAuthToken();

        StaircaseRequestDto updateDto =
                StaircaseRequestDto.builder().floor(5).x(250.0).y(350.0).name("Updated").active(true).build();

        mockMvc.perform(put(STAIRCASE_BASE_URI + "/" + savedStaircase.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isForbidden());
    }

    // ===== DELETE /{id} (deleteStaircase) =====

    @Test
    @DisplayName("DELETE /api/v1/staircase/{id} -> Returns 204 No Content for admin with valid ID")
    void deleteStaircase_whenAdminWithValidId_returnsNoContent() throws Exception {
        Staircase staircase = Staircase.builder().floor(1).x(100.0).y(200.0).name("To Delete").active(true).build();
        Staircase savedStaircase = staircaseRepository.save(staircase);

        String adminToken = getAdminAuthToken();

        mockMvc.perform(delete(STAIRCASE_BASE_URI + "/" + savedStaircase.getId()).header(HttpHeaders.AUTHORIZATION,
                "Bearer " + adminToken)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/staircase/{id} -> Returns 404 Not Found for invalid ID")
    void deleteStaircase_whenInvalidId_returnsNotFound() throws Exception {
        String adminToken = getAdminAuthToken();

        mockMvc.perform(delete(STAIRCASE_BASE_URI + "/99999").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/staircase/{id} -> Returns 403 Forbidden for non-admin user")
    void deleteStaircase_whenNonAdmin_returnsForbidden() throws Exception {
        Staircase staircase = Staircase.builder().floor(1).x(100.0).y(200.0).name("To Delete").active(true).build();
        Staircase savedStaircase = staircaseRepository.save(staircase);

        String employeeToken = getEmployeeAuthToken();

        mockMvc.perform(delete(STAIRCASE_BASE_URI + "/" + savedStaircase.getId()).header(HttpHeaders.AUTHORIZATION,
                "Bearer " + employeeToken)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/staircase/{id} -> Performs soft delete by setting active=false")
    void deleteStaircase_performsSoftDelete() throws Exception {
        Staircase staircase =
                Staircase.builder().floor(1).x(100.0).y(200.0).name("To Soft Delete").active(true).build();
        Staircase savedStaircase = staircaseRepository.save(staircase);
        Long staircaseId = savedStaircase.getId();

        String adminToken = getAdminAuthToken();

        mockMvc.perform(delete(STAIRCASE_BASE_URI + "/" + staircaseId).header(HttpHeaders.AUTHORIZATION,
                "Bearer " + adminToken)).andExpect(status().isNoContent());

        // Verify soft delete by checking the database directly
        Staircase deletedStaircase = staircaseRepository.findById(staircaseId).orElseThrow();
        assertThat(deletedStaircase.isActive()).isFalse();
    }
}
