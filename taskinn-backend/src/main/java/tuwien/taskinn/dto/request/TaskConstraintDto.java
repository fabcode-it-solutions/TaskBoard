package tuwien.taskinn.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tuwien.taskinn.enums.TaskStatus;
import tuwien.taskinn.enums.TaskType;

import java.time.LocalDateTime;

/**
 * Represents parameters for the automated task scheduling for a single room.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskConstraintDto {

    @NotNull
    private Long roomId;

    /** Duration of the cleaning task in minutes. */
    @NotNull
    private Long duration;

    @NotNull
    private TaskType taskType;

    private TaskStatus taskStatus;

    private LocalDateTime maxEndTime;

    private LocalDateTime minStartTime;

}
