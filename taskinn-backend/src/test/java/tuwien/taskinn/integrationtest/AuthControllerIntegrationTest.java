package tuwien.taskinn.integrationtest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.request.SignInRequestDto;
import tuwien.taskinn.dto.response.UserProfileDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.enums.Roles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Auth Controller Integration Tests")
public class AuthControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String AUTH_BASE_URI = BASE_URI + "/auth";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /me -> Fails with 403 Forbidden for unauthenticated user")
    public void getCurrentAccountProfile_givenNothing_whenUserUnauthenticated_thenDontAllowAccess() throws Exception {

        mockMvc.perform(get(AUTH_BASE_URI + "/me")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /me -> Returns correct user profile for an authenticated ADMIN user")
    public void getMyProfile_whenAuthenticated_thenReturnsCorrectProfile() throws Exception {
        String adminToken = getAdminAuthToken();

        var mvcResult =
                mockMvc.perform(get(AUTH_BASE_URI + "/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        UserProfileDto apiResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});

        assertAll(() -> assertThat(apiResponse.getEmail()).isEqualTo(ADMIN_EMAIL),
                () -> assertThat(apiResponse.getFirstName()).isEqualTo(ADMIN_FIRST_NAME),
                () -> assertThat(apiResponse.getLastName()).isEqualTo(ADMIN_LAST_NAME),
                () -> assertThat(apiResponse.getRoles().contains("ADMIN")).isTrue());
    }

    @Test
    @DisplayName("POST /sign-in -> Returns 401 Unauthorized when user is inactive")
    public void signIn_whenUserIsInactive_returnsUnauthorized() throws Exception {
        String email = "inactive.user@taskinn.com";
        String password = "password123";

        Employee inactiveEmployee = Employee.builder()
                .firstName("Inactive")
                .lastName("User")
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .active(false)
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        employeeRepository.save(inactiveEmployee);

        SignInRequestDto signInRequest = new SignInRequestDto(email, password);

        var mvcResult = mockMvc
                .perform(post(AUTH_BASE_URI + "/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        assertAll(() -> assertThat(responseBody).contains("User account is inactive"),
                () -> assertThat(mvcResult.getResponse().getStatus()).isEqualTo(401));
    }
}
