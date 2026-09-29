package tuwien.taskinn.services;

import tuwien.taskinn.dto.CleaningTaskDto;
import tuwien.taskinn.dto.request.TaskScheduleSolveRequestDto;

import java.util.List;

public interface TaskSchedulingService {

    /**
     * Returns a list of all known cleaning tasks for all employees on a given day.
     *
     * @param year The year of the given day
     * @param month The month of the given day
     * @param day The month day of the given day
     * @return The list of all known cleaning tasks for all employees on the given day
     */
    List<CleaningTaskDto> getTasksForDay(Integer year, Integer month, Integer day);

    /**
     * Returns a list of all known cleaning tasks for a given employee on a given day.
     *
     * @param year The year of the given day
     * @param month The month of the given day
     * @param day The month day of the given day
     * @param employeeId The id of the given employee
     * @return A list of all known cleaning tasks for the given employee on the given day
     */
    List<CleaningTaskDto> getTasksForEmployeeAndDay(Integer year, Integer month, Integer day, Long employeeId);

    /**
     * Updates all cleaning tasks for a given day, replacing any previously known tasks for that same day.
     *
     * @param year The year of the given day
     * @param month The month of the given day
     * @param day The month day of the given day
     * @param tasks List of new cleaning tasks for the given day
     */
    void putTasksForDay(Integer year, Integer month, Integer day, List<CleaningTaskDto> tasks);

    /**
     * Tries to find a solution to the given task scheduling problem.
     *
     * @param request Description of the task scheduling problem
     * @return A solution to request
     */
    List<CleaningTaskDto> solveTaskScheduling(TaskScheduleSolveRequestDto request);

}
