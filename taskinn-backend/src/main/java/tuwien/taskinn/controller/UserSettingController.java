package tuwien.taskinn.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tuwien.taskinn.dto.UserSettingDto;
import tuwien.taskinn.services.UserSettingService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/user-settings")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService settingService;

    @GetMapping("/{namespace}/{name}")
    public UserSettingDto getSetting(@PathVariable @NotNull @NotBlank String namespace,
            @PathVariable @NotNull @NotBlank String name) {
        return settingService.getSetting(namespace, name);
    }

    @GetMapping("/{namespace}")
    public List<UserSettingDto> getSettings(@PathVariable @NotNull @NotBlank String namespace) {
        return settingService.getSettings(namespace);
    }

    @GetMapping("/")
    public List<UserSettingDto> getSettings() {
        return settingService.getSettings();
    }

    @PutMapping("/{namespace}/{name}")
    public void putSetting(@PathVariable @NotNull @NotBlank String namespace,
            @PathVariable @NotNull @NotBlank String name, @RequestBody @NotNull UserSettingDto content) {
        settingService.putSetting(namespace, name, content);
    }

}
