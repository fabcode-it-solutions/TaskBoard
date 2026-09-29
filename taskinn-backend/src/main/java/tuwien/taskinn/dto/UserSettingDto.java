package tuwien.taskinn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSettingDto {

    @NotNull
    @NotBlank
    private String namespace;

    @NotNull
    @NotBlank
    private String name;

    @Size(max = 1024)
    private String content;

}
