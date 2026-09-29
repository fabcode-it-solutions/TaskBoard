package tuwien.taskinn.mapper;

import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.UserSettingDto;
import tuwien.taskinn.entity.UserSetting;

@Component
public class UserSettingMapper {

    public UserSettingDto toDto(UserSetting setting) {
        if (setting == null) {
            return null;
        }

        return UserSettingDto.builder()
                .namespace(setting.getNamespace())
                .name(setting.getName())
                .content(setting.getContent())
                .build();
    }

}
