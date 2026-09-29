package tuwien.taskinn.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskScheduleSolveRequestDto {

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private List<@NotNull Long> employeeIds;

    @NotNull
    private List<@NotNull TaskConstraintDto> constraints;

}
