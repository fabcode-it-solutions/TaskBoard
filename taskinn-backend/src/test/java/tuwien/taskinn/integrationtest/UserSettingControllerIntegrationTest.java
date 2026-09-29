package tuwien.taskinn.integrationtest;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.UserSettingDto;
import tuwien.taskinn.entity.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserSettingControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String USER_SETTING_BASE_URI = BASE_URI + "/user-settings";

    @Test
    public void getSetting_whenSettingExistsAndAuthorized_returnSetting() throws Exception {
        final UserSetting setting = settingRepository
                .save(UserSetting.builder().namespace("namespace").name("name").content("content").build());

        final MvcResult mvcResult =
                mockMvc.perform(get(USER_SETTING_BASE_URI + "/namespace/name").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + getAdminAuthToken())).andExpect(status().isOk()).andReturn();

        final String responseString = mvcResult.getResponse().getContentAsString();
        final UserSettingDto result = objectMapper.readValue(responseString, new TypeReference<>() {});

        assertAll(() -> assertThat(result.getNamespace()).isEqualTo(setting.getNamespace()),
                () -> assertThat(result.getName()).isEqualTo(setting.getName()),
                () -> assertThat(result.getContent()).isEqualTo(setting.getContent()));
    }

    @Test
    public void getSetting_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(get(USER_SETTING_BASE_URI + "/namespace/name")).andExpect(status().isForbidden());
    }

    @Test
    public void getSettings_whenGivenNamespaceAndAuthorized_returnSettingsInNamespace() throws Exception {
        final UserSetting setting1 = settingRepository
                .save(UserSetting.builder().namespace("namespace").name("name1").content("content1").build());
        final UserSetting setting2 = settingRepository
                .save(UserSetting.builder().namespace("namespace").name("name2").content("content2").build());
        settingRepository.save(UserSetting.builder().namespace("namespace2").name("name").content("content").build());

        final MvcResult mvcResult =
                mockMvc.perform(get(USER_SETTING_BASE_URI + "/namespace").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + getAdminAuthToken())).andExpect(status().isOk()).andReturn();

        final String responseString = mvcResult.getResponse().getContentAsString();
        final List<UserSettingDto> result = objectMapper.readValue(responseString, new TypeReference<>() {});

        assertAll(() -> assertThat(result).hasSize(2),
                () -> assertThat(result).map(UserSettingDto::getName)
                        .containsExactlyInAnyOrder(setting1.getName(), setting2.getName()),
                () -> assertThat(result).map(UserSettingDto::getContent)
                        .containsExactlyInAnyOrder(setting1.getContent(), setting2.getContent()));
    }

    @Test
    public void getSettings_whenGivenNamespaceAndNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(get(USER_SETTING_BASE_URI + "/namespace")).andExpect(status().isForbidden());
    }

    @Test
    public void getSettings_whenAuthorized_returnAllSettings() throws Exception {
        final UserSetting setting1 = settingRepository
                .save(UserSetting.builder().namespace("namespace").name("name1").content("content1").build());
        final UserSetting setting2 = settingRepository
                .save(UserSetting.builder().namespace("namespace").name("name2").content("content2").build());
        final UserSetting setting3 = settingRepository
                .save(UserSetting.builder().namespace("namespace2").name("name").content("content").build());

        final MvcResult mvcResult = mockMvc.perform(
                get(USER_SETTING_BASE_URI + "/").header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken()))
                .andExpect(status().isOk())
                .andReturn();

        final String responseString = mvcResult.getResponse().getContentAsString();
        final List<UserSettingDto> result = objectMapper.readValue(responseString, new TypeReference<>() {});

        assertAll(() -> assertThat(result).hasSize(3),
                () -> assertThat(result).map(UserSettingDto::getName)
                        .containsExactlyInAnyOrder(setting1.getName(), setting2.getName(), setting3.getName()),
                () -> assertThat(result).map(UserSettingDto::getContent)
                        .containsExactlyInAnyOrder(setting1.getContent(), setting2.getContent(),
                                setting3.getContent()));
    }

    @Test
    public void getSettings_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(get(USER_SETTING_BASE_URI + "/")).andExpect(status().isForbidden());
    }

    @Test
    public void putSetting_whenValidAndAuthorized_saveSetting() throws Exception {
        final UserSettingDto request =
                UserSettingDto.builder().namespace("namespace").name("name").content("content").build();

        mockMvc.perform(put(USER_SETTING_BASE_URI + "/namespace/name")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

        final Optional<UserSetting> setting =
                settingRepository.findByNamespaceAndName(request.getNamespace(), request.getName());

        assertAll(() -> assertThat(setting).isPresent(),
                () -> assertThat(setting.get().getContent()).isEqualTo(request.getContent()));
    }

    @Test
    public void putSetting_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(
                put(USER_SETTING_BASE_URI + "/namespace/name").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isForbidden());
    }

}
