package tuwien.taskinn.dto.response;

import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tuwien.taskinn.enums.TaskStatus;
import tuwien.taskinn.enums.TaskType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningTaskDto {
    private Long id;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private TaskType taskType;
    private TaskStatus status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    private Long employeeId;
    private String employeeName;

    private Long roomId;
    private String roomNumber;
}
