package tuwien.taskinn.mapper;

import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.response.CleaningTaskDto;
import tuwien.taskinn.entity.CleaningTask;
import tuwien.taskinn.entity.Employee;

@Component
public class CleaningTaskMapper {

    public CleaningTaskDto toDto(CleaningTask cleaningTask) {
        if (cleaningTask == null) {
            return null;
        }

        final Employee employee = cleaningTask.getEmployee();
        final String employeeName = (employee != null) ? employee.getFirstName() + " " + employee.getLastName() : null;

        return CleaningTaskDto.builder()
                .id(cleaningTask.getId())
                .scheduledStart(cleaningTask.getScheduledStart())
                .scheduledEnd(cleaningTask.getScheduledEnd())
                .actualStart(cleaningTask.getActualStart())
                .actualEnd(cleaningTask.getActualEnd())
                .taskType(cleaningTask.getTaskType())
                .status(cleaningTask.getStatus())
                .notes(cleaningTask.getNotes())
                .createdAt(cleaningTask.getCreatedAt())
                .updatedAt(cleaningTask.getUpdatedAt())
                .employeeId(employee != null ? employee.getId() : null)
                .employeeName(employeeName)
                .roomId(cleaningTask.getRoom().getId())
                .roomNumber(cleaningTask.getRoom().getRoomNumber())
                .build();
    }

    public tuwien.taskinn.dto.CleaningTaskDto toTaskScheduleDto(CleaningTask cleaningTask) {
        if (cleaningTask == null) {
            return null;
        }

        final Employee employee = cleaningTask.getEmployee();
        final String employeeName = (employee != null) ? employee.getFirstName() + " " + employee.getLastName() : null;

        return tuwien.taskinn.dto.CleaningTaskDto.builder()
                .id(cleaningTask.getId())
                .scheduledStart(cleaningTask.getScheduledStart())
                .scheduledEnd(cleaningTask.getScheduledEnd())
                .actualStart(cleaningTask.getActualStart())
                .actualEnd(cleaningTask.getActualEnd())
                .taskType(cleaningTask.getTaskType())
                .status(cleaningTask.getStatus())
                .notes(cleaningTask.getNotes())
                .employeeId(employee != null ? employee.getId() : null)
                .employeeName(employeeName)
                .roomId(cleaningTask.getRoom().getId())
                .roomNumber(cleaningTask.getRoom().getRoomNumber())
                .build();
    }

}
