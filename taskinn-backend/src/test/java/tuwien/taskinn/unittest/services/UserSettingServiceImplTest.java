package tuwien.taskinn.unittest.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tuwien.taskinn.dto.UserSettingDto;
import tuwien.taskinn.entity.*;
import tuwien.taskinn.mapper.UserSettingMapper;
import tuwien.taskinn.repo.UserSettingRepository;
import tuwien.taskinn.services.impl.UserSettingServiceImpl;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSettingServiceImplTest Unit Tests")
public class UserSettingServiceImplTest {

    @Mock
    private UserSettingRepository settingRepository;

    @Spy
    private UserSettingMapper settingMapper = new UserSettingMapper();

    @InjectMocks
    private UserSettingServiceImpl settingService;

    @Test
    public void getSetting_whenSettingExists_returnSetting() {
        final UserSetting setting =
                UserSetting.builder().namespace("namespace").name("name").content("content").build();

        when(settingRepository.findByNamespaceAndName(eq("namespace"), eq("name"))).thenReturn(Optional.of(setting));

        final UserSettingDto result = settingService.getSetting("namespace", "name");

        assertAll(() -> assertThat(result.getNamespace()).isEqualTo(setting.getNamespace()),
                () -> assertThat(result.getName()).isEqualTo(setting.getName()),
                () -> assertThat(result.getContent()).isEqualTo(setting.getContent()));
    }

    @Test
    public void getSettings_whenNamespaceGiven_returnSettingsInNamespace() {
        final UserSetting setting1 =
                UserSetting.builder().namespace("namespace").name("name1").content("content1").build();
        final UserSetting setting2 =
                UserSetting.builder().namespace("namespace").name("name2").content("content2").build();

        when(settingRepository.findByNamespace(eq("namespace"))).thenReturn(List.of(setting1, setting2));

        final List<UserSettingDto> result = settingService.getSettings("namespace");

        assertAll(() -> assertThat(result).hasSize(2),
                () -> assertThat(result).map(UserSettingDto::getName)
                        .containsExactlyInAnyOrder(setting1.getName(), setting2.getName()),
                () -> assertThat(result).map(UserSettingDto::getContent)
                        .containsExactlyInAnyOrder(setting1.getContent(), setting2.getContent()));
    }

    @Test
    public void getSettings_whenNoNamespaceGiven_returnAllSettings() {
        final UserSetting setting1 =
                UserSetting.builder().namespace("namespace1").name("name1").content("content1").build();
        final UserSetting setting2 =
                UserSetting.builder().namespace("namespace2").name("name2").content("content2").build();

        when(settingRepository.findAll()).thenReturn(List.of(setting1, setting2));

        final List<UserSettingDto> result = settingService.getSettings();

        assertAll(() -> assertThat(result).hasSize(2),
                () -> assertThat(result).map(UserSettingDto::getNamespace)
                        .containsExactlyInAnyOrder(setting1.getNamespace(), setting2.getNamespace()),
                () -> assertThat(result).map(UserSettingDto::getName)
                        .containsExactlyInAnyOrder(setting1.getName(), setting2.getName()),
                () -> assertThat(result).map(UserSettingDto::getContent)
                        .containsExactlyInAnyOrder(setting1.getContent(), setting2.getContent()));
    }

    @Test
    public void putSetting_whenValid_savesSetting() {
        when(settingRepository.findByNamespaceAndName(any(), any())).thenReturn(Optional.empty());

        final UserSettingDto request =
                UserSettingDto.builder().namespace("namespace").name("name").content("content").build();

        settingService.putSetting("namespace", "name", request);

        assertAll(() -> verify(settingRepository, times(1)).save(argThat(x -> x.getNamespace().equals("namespace")
                && x.getName().equals("name") && x.getContent().equals("content"))));
    }

}
