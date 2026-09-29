package tuwien.taskinn.services;

import tuwien.taskinn.dto.UserSettingDto;

import java.util.List;

public interface UserSettingService {

    UserSettingDto getSetting(String namespace, String name);

    List<UserSettingDto> getSettings(String namespace);

    List<UserSettingDto> getSettings();

    void putSetting(String namespace, String name, UserSettingDto request);

}
