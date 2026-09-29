package tuwien.taskinn.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tuwien.taskinn.enums.TaskStatus;
import tuwien.taskinn.enums.TaskType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningTaskDto {
    private Long id;

    @NotNull
    private LocalDateTime scheduledStart;
    @NotNull
    private LocalDateTime scheduledEnd;

    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;

    @NotNull
    private TaskType taskType;

    private TaskStatus status;

    private String notes;

    private Boolean constraintViolated;

    @NotNull
    private Long employeeId;
    private String employeeName;

    @NotNull
    private Long roomId;
    private String roomNumber;
}
