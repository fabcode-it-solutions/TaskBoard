package tuwien.taskinn.services.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tuwien.taskinn.dto.UserSettingDto;
import tuwien.taskinn.entity.UserSetting;
import tuwien.taskinn.mapper.UserSettingMapper;
import tuwien.taskinn.repo.UserSettingRepository;
import tuwien.taskinn.services.UserSettingService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSettingServiceImpl implements UserSettingService {

    private final UserSettingRepository settingRepository;
    private final UserSettingMapper settingMapper;

    public UserSettingDto getSetting(String namespace, String name) {
        return settingMapper.toDto(settingRepository.findByNamespaceAndName(namespace, name)
                .orElseThrow(() -> new EntityNotFoundException("unknown setting: " + namespace + "." + name)));
    }

    public List<UserSettingDto> getSettings(String namespace) {
        return settingRepository.findByNamespace(namespace).stream().map(settingMapper::toDto).toList();
    }

    public List<UserSettingDto> getSettings() {
        return settingRepository.findAll().stream().map(settingMapper::toDto).toList();
    }

    @Transactional
    public void putSetting(String namespace, String name, UserSettingDto request) {
        final Optional<UserSetting> previousOptional = settingRepository.findByNamespaceAndName(namespace, name);
        if (previousOptional.isEmpty()) {
            settingRepository
                    .save(UserSetting.builder().namespace(namespace).name(name).content(request.getContent()).build());
        } else {
            final UserSetting previous = previousOptional.get();
            previous.setContent(request.getContent());
            settingRepository.save(previous);
        }
    }

}
