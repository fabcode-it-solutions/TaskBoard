package tuwien.taskinn.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShiftScheduleTemplateDto {

    @NotNull
    private Long id;

    @NotNull
    private String name;

    /** Content of the template (a JSON string, format controlled by the frontend) */
    @NotNull
    private String content;

}
