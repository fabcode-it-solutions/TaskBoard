package tuwien.taskinn.unittest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import tuwien.taskinn.dto.response.CleaningTaskDto;
import tuwien.taskinn.entity.CleaningTask;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.TaskStatus;
import tuwien.taskinn.enums.TaskType;
import tuwien.taskinn.mapper.CleaningTaskMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("CleaningTaskMapper Unit Tests")
public class CleaningTaskMapperTest {

    private final CleaningTaskMapper cleaningTaskMapper = new CleaningTaskMapper();

    @Test
    @DisplayName("toDto should correctly map CleaningTask entity to CleaningTaskDto with employee")
    public void toDto_whenCleaningTaskHasEmployee_correctlyMapsToDto() {
        Employee employee = Employee.builder().id(1L).firstName("John").lastName("Doe").build();

        Room room = Room.builder().id(10L).roomNumber("101").floor(1).build();

        CleaningTask cleaningTask = CleaningTask.builder()
                .id(100L)
                .scheduledStart(LocalDateTime.of(2026, 1, 26, 10, 0))
                .scheduledEnd(LocalDateTime.of(2026, 1, 26, 11, 0))
                .actualStart(LocalDateTime.of(2026, 1, 26, 10, 5))
                .actualEnd(LocalDateTime.of(2026, 1, 26, 10, 55))
                .taskType(TaskType.CHECKOUT_CLEAN)
                .status(TaskStatus.COMPLETED)
                .notes("Task completed successfully")
                .createdAt(Instant.parse("2026-01-26T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-26T10:55:00Z"))
                .employee(employee)
                .room(room)
                .build();

        CleaningTaskDto result = cleaningTaskMapper.toDto(cleaningTask);

        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result.getId()).isEqualTo(100L),
                () -> assertThat(result.getScheduledStart()).isEqualTo(LocalDateTime.of(2026, 1, 26, 10, 0)),
                () -> assertThat(result.getScheduledEnd()).isEqualTo(LocalDateTime.of(2026, 1, 26, 11, 0)),
                () -> assertThat(result.getActualStart()).isEqualTo(LocalDateTime.of(2026, 1, 26, 10, 5)),
                () -> assertThat(result.getActualEnd()).isEqualTo(LocalDateTime.of(2026, 1, 26, 10, 55)),
                () -> assertThat(result.getTaskType()).isEqualTo(TaskType.CHECKOUT_CLEAN),
                () -> assertThat(result.getStatus()).isEqualTo(TaskStatus.COMPLETED),
                () -> assertThat(result.getNotes()).isEqualTo("Task completed successfully"),
                () -> assertThat(result.getEmployeeId()).isEqualTo(1L),
                () -> assertThat(result.getEmployeeName()).isEqualTo("John Doe"),
                () -> assertThat(result.getRoomId()).isEqualTo(10L),
                () -> assertThat(result.getRoomNumber()).isEqualTo("101"));
    }

    @Test
    @DisplayName("toDto should correctly map CleaningTask entity to CleaningTaskDto without employee")
    public void toDto_whenCleaningTaskHasNoEmployee_correctlyMapsToDto() {
        Room room = Room.builder().id(20L).roomNumber("202").floor(2).build();

        CleaningTask cleaningTask = CleaningTask.builder()
                .id(200L)
                .scheduledStart(LocalDateTime.of(2026, 1, 26, 14, 0))
                .scheduledEnd(LocalDateTime.of(2026, 1, 26, 15, 0))
                .actualStart(null)
                .actualEnd(null)
                .taskType(TaskType.STAYOVER_CLEAN)
                .status(TaskStatus.PENDING)
                .notes(null)
                .createdAt(Instant.parse("2026-01-26T14:00:00Z"))
                .updatedAt(Instant.parse("2026-01-26T14:00:00Z"))
                .employee(null)
                .room(room)
                .build();

        CleaningTaskDto result = cleaningTaskMapper.toDto(cleaningTask);

        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result.getId()).isEqualTo(200L),
                () -> assertThat(result.getScheduledStart()).isEqualTo(LocalDateTime.of(2026, 1, 26, 14, 0)),
                () -> assertThat(result.getScheduledEnd()).isEqualTo(LocalDateTime.of(2026, 1, 26, 15, 0)),
                () -> assertThat(result.getActualStart()).isNull(), () -> assertThat(result.getActualEnd()).isNull(),
                () -> assertThat(result.getTaskType()).isEqualTo(TaskType.STAYOVER_CLEAN),
                () -> assertThat(result.getStatus()).isEqualTo(TaskStatus.PENDING),
                () -> assertThat(result.getNotes()).isNull(), () -> assertThat(result.getEmployeeId()).isNull(),
                () -> assertThat(result.getEmployeeName()).isNull(),
                () -> assertThat(result.getRoomId()).isEqualTo(20L),
                () -> assertThat(result.getRoomNumber()).isEqualTo("202"));
    }

    @Test
    @DisplayName("toDto should return null when cleaningTask is null")
    public void toDto_whenCleaningTaskIsNull_returnsNull() {
        CleaningTaskDto result = cleaningTaskMapper.toDto(null);

        assertThat(result).isNull();
    }
}
