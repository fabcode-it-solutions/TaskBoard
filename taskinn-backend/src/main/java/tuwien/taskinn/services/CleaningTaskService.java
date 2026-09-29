package tuwien.taskinn.services;

import java.util.List;
import tuwien.taskinn.dto.response.CleaningTaskDto;

public interface CleaningTaskService {

    /**
     * Start a cleaning task for a room
     *
     * @param roomId The room ID
     * @param employeeEmail The email of the employee starting the task
     * @return The started cleaning task
     */
    CleaningTaskDto startCleaning(Long roomId, String employeeEmail);

    /**
     * Finish a cleaning task for a room
     *
     * @param roomId The room ID
     * @param employeeEmail The email of the employee finishing the task
     * @param notes Optional completion notes
     * @return The completed cleaning task
     */
    CleaningTaskDto finishCleaning(Long roomId, String employeeEmail, String notes);

    /**
     * Get the current cleaning task for a room (if any)
     *
     * @param roomId The room ID
     * @return The cleaning task or null
     */
    CleaningTaskDto getCurrentTaskForRoom(Long roomId);

    /**
     * Get all cleaning tasks that are currently in progress
     *
     * @return List of cleaning tasks with IN_PROGRESS status
     */
    List<CleaningTaskDto> getAllInProgressTasks();

    /**
     * Skip a cleaning task (e.g., due to "Do Not Disturb")
     *
     * @param roomId The room ID
     * @param employeeEmail The email of the employee reporting the skip
     * @param reason The reason for skipping (e.g., "Do Not Disturb")
     * @return The blocked cleaning task
     */
    CleaningTaskDto skipCleaning(Long roomId, String employeeEmail, String reason);
}
