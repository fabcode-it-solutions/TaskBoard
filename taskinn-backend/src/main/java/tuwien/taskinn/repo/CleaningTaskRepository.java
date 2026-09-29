package tuwien.taskinn.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tuwien.taskinn.entity.CleaningTask;
import tuwien.taskinn.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.Collection;

public interface CleaningTaskRepository extends JpaRepository<CleaningTask, Long> {

    @Query("""
            SELECT ct FROM CleaningTask ct WHERE
                (ct.scheduledStart >= :start AND ct.scheduledEnd < :end) AND
                (ct.scheduledEnd >= :start AND ct.scheduledEnd < :end)
            """)
    List<CleaningTask> findAllTasksInTimeframe(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT ct FROM CleaningTask ct WHERE
                (ct.employee.id = :id) AND
                (ct.scheduledStart >= :start AND ct.scheduledEnd < :end) AND
                (ct.scheduledEnd >= :start AND ct.scheduledEnd < :end)
            """)
    List<CleaningTask> findAllEmployeeTasksInTimeframe(@Param("id") Long employeeId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Modifying
    @Query("""
            DELETE FROM CleaningTask ct WHERE
                (ct.scheduledStart >= :start AND ct.scheduledEnd < :end) AND
                (ct.scheduledEnd >= :start AND ct.scheduledEnd < :end)
            """)
    void deleteAllTasksInTimeframe(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Find all cleaning tasks for a specific room with PENDING status scheduled for today or earlier
     */
    @Query("SELECT ct FROM CleaningTask ct WHERE ct.room.id = :roomId " + "AND ct.status = :status "
            + "AND ct.scheduledStart < :todayEnd " + "ORDER BY ct.scheduledStart ASC")
    List<CleaningTask> findPendingTasksForRoom(@Param("roomId") Long roomId, @Param("status") TaskStatus status,
            @Param("todayEnd") LocalDateTime todayEnd);

    /**
     * Find cleaning task IN_PROGRESS for a specific room
     */
    Optional<CleaningTask> findByRoomIdAndStatus(Long roomId, TaskStatus status);

    /**
     * Find all tasks assigned to a specific employee with a given status
     */
    List<CleaningTask> findByEmployeeIdAndStatus(Long employeeId, TaskStatus status);

    /**
     * Check if any cleaning tasks are scheduled between the given start and end times
     *
     * @param start the start time
     * @param end the end time
     * @return true if any tasks exist in the timeframe, false otherwise
     */
    boolean existsByScheduledStartBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Retrieves all completed cleaning tasks with their task type, room type, and actual start/end timestamps. Used to
     * compute average task duration grouped by task type and room type without any date filtering.
     *
     * @return a list of Object arrays where: [0] = taskType (enum), [1] = roomType name (String), [2] = actualStart
     *         (LocalDateTime), [3] = actualEnd (LocalDateTime)
     */
    @Query("""
                SELECT ct.taskType, r.roomType.name, ct.actualStart, ct.actualEnd
                FROM CleaningTask ct
                JOIN ct.room r
                WHERE ct.status = tuwien.taskinn.enums.TaskStatus.COMPLETED
                  AND ct.actualStart IS NOT NULL
                  AND ct.actualEnd IS NOT NULL
                ORDER BY ct.taskType, r.roomType.name
            """)
    List<Object[]> findCompletedTasksWithTimestamps();

    /**
     * Retrieves completed cleaning tasks with their task type, room type, and actual start/end timestamps limited to a
     * given time range. Used to compute average task duration grouped by task type and room type within a period.
     *
     * @param from start of the date-time range (inclusive)
     * @param to end of the date-time range (inclusive)
     * @return a list of Object arrays where: [0] = taskType (enum), [1] = roomType name (String), [2] = actualStart
     *         (LocalDateTime), [3] = actualEnd (LocalDateTime)
     */
    @Query("""
                SELECT ct.taskType, r.roomType.name, ct.actualStart, ct.actualEnd
                FROM CleaningTask ct
                JOIN ct.room r
                WHERE ct.status = tuwien.taskinn.enums.TaskStatus.COMPLETED
                  AND ct.actualStart BETWEEN :from AND :to
                  AND ct.actualEnd IS NOT NULL
                ORDER BY ct.taskType, r.roomType.name
            """)
    List<Object[]> findCompletedTasksWithTimestampsInRange(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Retrieves actual start and end timestamps for all completed cleaning tasks. Used to compute total and monthly
     * working hours without any date filtering.
     *
     * @return a list of Object arrays where: [0] = actualStart (LocalDateTime), [1] = actualEnd (LocalDateTime)
     */
    @Query("""
                SELECT ct.actualStart, ct.actualEnd
                FROM CleaningTask ct
                WHERE ct.status = tuwien.taskinn.enums.TaskStatus.COMPLETED
                  AND ct.actualStart IS NOT NULL
                  AND ct.actualEnd IS NOT NULL
                ORDER BY ct.actualStart
            """)
    List<Object[]> findAllCompletedTasksWithTimestamps();

    /**
     * Retrieves actual start and end timestamps for completed cleaning tasks within a given time range. Used to compute
     * total and monthly working hours for a specific period.
     *
     * @param from start of the date-time range (inclusive)
     * @param to end of the date-time range (inclusive)
     * @return a list of Object arrays where: [0] = actualStart (LocalDateTime), [1] = actualEnd (LocalDateTime)
     */
    @Query("""
                SELECT ct.actualStart, ct.actualEnd
                FROM CleaningTask ct
                WHERE ct.status = tuwien.taskinn.enums.TaskStatus.COMPLETED
                  AND ct.actualStart BETWEEN :from AND :to
                  AND ct.actualEnd IS NOT NULL
                ORDER BY ct.actualStart
            """)
    List<Object[]> findCompletedTasksWithTimestampsInRangeForHours(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Counts all cleaning tasks that are currently in BLOCKED status. Used to calculate the blocked-task percentage for
     * the dashboard pie chart.
     *
     * @return the total number of blocked cleaning tasks
     */
    @Query("""
                SELECT COUNT(ct)
                FROM CleaningTask ct
                WHERE ct.status = tuwien.taskinn.enums.TaskStatus.BLOCKED
            """)
    Long countBlockedTasks();

    /**
     * Counts all cleaning tasks regardless of status. Used as the denominator when calculating blocked-task percentage.
     *
     * @return the total number of cleaning tasks
     */
    @Query("""
                SELECT COUNT(ct)
                FROM CleaningTask ct
            """)
    Long countAllTasks();

    /**
     * Counts all cleaning tasks in BLOCKED status within a given time range. Used to calculate blocked-task percentage
     * for a filtered period.
     *
     * @param from start of the date-time range (inclusive)
     * @param to end of the date-time range (inclusive)
     * @return the number of blocked cleaning tasks within the given period
     */
    @Query("""
                SELECT COUNT(ct)
                FROM CleaningTask ct
                WHERE ct.actualStart BETWEEN :from AND :to
                  AND ct.status = tuwien.taskinn.enums.TaskStatus.BLOCKED
            """)
    Long countBlockedTasksInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Counts all cleaning tasks within a given time range regardless of status. Used as the denominator when
     * calculating blocked-task percentage for a filtered period.
     *
     * @param from start of the date-time range (inclusive)
     * @param to end of the date-time range (inclusive)
     * @return the total number of cleaning tasks within the given period
     */
    @Query("""
                SELECT COUNT(ct)
                FROM CleaningTask ct
                WHERE ct.actualStart BETWEEN :from AND :to
            """)
    Long countAllTasksInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Retrieves completed cleaning tasks with their task type, room type, and actual timestamps for a specific set of
     * employees.
     *
     * @param employeeIds a collection of employee IDs to filter the tasks by
     * @return a list of Object arrays representing the task data, where: [0] = employee ID (Long), [1] = taskType
     *         (TaskType enum), [2] = roomType name (String), [3] = actualStart (LocalDateTime), [4] = actualEnd
     *         (LocalDateTime)
     */
    @Query("""
            SELECT ct.employee.id, ct.taskType, r.roomType.name, ct.actualStart, ct.actualEnd
            FROM CleaningTask ct
            JOIN ct.room r
            WHERE ct.status = tuwien.taskinn.enums.TaskStatus.COMPLETED
              AND ct.actualStart IS NOT NULL
              AND ct.actualEnd IS NOT NULL
              AND ct.employee.id IN :employeeIds
            ORDER BY ct.employee.id, ct.taskType, r.roomType.name
            """)
    List<Object[]> findCompletedTasksForEmployees(@Param("employeeIds") Collection<Long> employeeIds);

    /**
     * Retrieves completed cleaning tasks with their task type, room type, and actual timestamps for a specific set of
     * employees within a defined time range.
     *
     * @param employeeIds a collection of employee IDs to filter the tasks by
     * @param from the start of the date-time range (inclusive)
     * @param to the end of the date-time range (inclusive)
     * @return a list of Object arrays representing the task data, where: [0] = employee ID (Long), [1] = taskType
     *         (TaskType enum), [2] = roomType name (String), [3] = actualStart (LocalDateTime), [4] = actualEnd
     *         (LocalDateTime)
     */
    @Query("""
            SELECT ct.employee.id, ct.taskType, r.roomType.name, ct.actualStart, ct.actualEnd
            FROM CleaningTask ct
            JOIN ct.room r
            WHERE ct.status = tuwien.taskinn.enums.TaskStatus.COMPLETED
              AND ct.actualStart BETWEEN :from AND :to
              AND ct.actualEnd IS NOT NULL
              AND ct.employee.id IN :employeeIds
            ORDER BY ct.employee.id, ct.taskType, r.roomType.name
            """)
    List<Object[]> findCompletedTasksForEmployeesInRange(@Param("employeeIds") Collection<Long> employeeIds,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

}
