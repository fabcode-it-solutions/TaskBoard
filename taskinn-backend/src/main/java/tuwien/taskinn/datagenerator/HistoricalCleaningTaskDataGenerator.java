package tuwien.taskinn.datagenerator;

import static java.util.Map.entry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tuwien.taskinn.entity.CleaningTask;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.Shift;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.enums.TaskStatus;
import tuwien.taskinn.enums.TaskType;
import tuwien.taskinn.repo.CleaningTaskRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.repo.ShiftRepository;

@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
@Transactional
public class HistoricalCleaningTaskDataGenerator {

    private final CleaningTaskRepository cleaningTaskRepository;
    private final ShiftRepository shiftRepository;
    private final RoomRepository roomRepository;

    private static final LocalDate START_DATE = LocalDate.of(2023, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 12, 31);
    private static final LocalTime TASK_START_TIME = LocalTime.of(6, 0);
    private static final LocalTime TASK_END_TIME = LocalTime.of(14, 0);
    private static final int BATCH_SIZE = 500;
    private static final int MIN_TASKS_PER_EMPLOYEE = 4;
    private static final int MAX_TASKS_PER_EMPLOYEE = 8;
    private static final int GAP_BETWEEN_TASKS_MINUTES = 15;

    // Task type distribution: 60% STAYOVER_CLEAN, 30% CHECKOUT_CLEAN, 10% DEEP_CLEAN
    private static final double CHECKOUT_CLEAN_PROBABILITY = 0.30;
    private static final double STAYOVER_CLEAN_PROBABILITY = 0.60;

    // Probability that a STAYOVER_CLEAN is blocked
    private static final double BLOCKED_STAYOVER_PROBABILITY = 0.15;

    // Base task durations in minutes
    private static final int CHECKOUT_CLEAN_DURATION = 40;
    private static final int STAYOVER_CLEAN_DURATION = 20;
    private static final int DEEP_CLEAN_DURATION = 80;

    private static final Map<String, Double> ROOM_TYPE_MULTIPLIERS =
            Map.ofEntries(entry("Single Room", 1.0), entry("Double Room", 1.2), entry("Superior Room", 1.3),
                    entry("Deluxe Double", 1.4), entry("Triple Room", 1.5), entry("Junior Suite", 1.6),
                    entry("Family Suite", 1.8), entry("Executive Suite", 2.0));

    private final Random random = new Random(42); // Fixed seed for reproducibility

    /**
     * Generates historical cleaning task data for housekeeping employees.
     */
    public void generateData() {
        // Check if historical tasks already exist (before today)
        LocalDateTime historicalEnd = LocalDate.now().atStartOfDay();
        if (cleaningTaskRepository.existsByScheduledStartBetween(START_DATE.atStartOfDay(),
                historicalEnd.minusDays(1))) {
            log.info("Historical cleaning tasks already exist. Skipping generation.");
            return;
        }

        log.info("Generating historical cleaning task data from {} to {}...", START_DATE, END_DATE);

        // Load all rooms (excluding out-of-order and permanently inactive)
        List<Room> allRooms = roomRepository.findAll()
                .stream()
                .filter(room -> room.getStatus() != RoomStatus.OUT_OF_ORDER
                        && room.getStatus() != RoomStatus.PERMANENTLY_INACTIVE)
                .collect(Collectors.toList());

        if (allRooms.isEmpty()) {
            log.warn("No active rooms found. Skipping historical cleaning task generation.");
            return;
        }

        List<CleaningTask> tasksToSave = new ArrayList<>();
        int totalTasks = 0;
        int roomIndex = 0;

        LocalDate currentDate = START_DATE;
        while (!currentDate.isAfter(END_DATE)) {
            // Find housekeeping shifts for this day
            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.atTime(23, 59, 59);

            List<Shift> housekeepingShifts = shiftRepository.findAllShiftsInTimeframe(dayStart, dayEnd)
                    .stream()
                    .filter(shift -> "Housekeeping".equals(shift.getDepartment()))
                    .collect(Collectors.toList());

            // Generate tasks for each housekeeping employee on this day
            for (Shift shift : housekeepingShifts) {
                Employee employee = shift.getEmployee();
                int numTasks =
                        MIN_TASKS_PER_EMPLOYEE + random.nextInt(MAX_TASKS_PER_EMPLOYEE - MIN_TASKS_PER_EMPLOYEE + 1);

                LocalTime currentTaskTime = TASK_START_TIME;

                for (int i = 0; i < numTasks; i++) {
                    // Check if we still have time in the work window
                    if (currentTaskTime.isAfter(TASK_END_TIME.minusMinutes(STAYOVER_CLEAN_DURATION))) {
                        break;
                    }

                    // Get room in round-robin fashion
                    Room room = allRooms.get(roomIndex % allRooms.size());
                    roomIndex++;

                    // Select task type based on distribution
                    TaskType taskType = selectTaskType();
                    int duration = getTaskDuration(taskType, room);

                    // Calculate scheduled times
                    LocalDateTime scheduledStart = LocalDateTime.of(currentDate, currentTaskTime);
                    LocalDateTime scheduledEnd = scheduledStart.plusMinutes(duration);

                    // Calculate actual times with variance (+/- 5-10 minutes)
                    int startVariance = random.nextInt(11) - 5; // -5 to +5 minutes
                    int durationVariance = random.nextInt(11) - 5; // -5 to +5 minutes
                    LocalDateTime actualStart = scheduledStart.plusMinutes(startVariance);
                    LocalDateTime actualEnd = actualStart.plusMinutes(duration + durationVariance);

                    // Determine status: Default COMPLETED, but STAYOVER_CLEAN has chance to be BLOCKED
                    TaskStatus status = TaskStatus.COMPLETED;
                    if (taskType == TaskType.STAYOVER_CLEAN && random.nextDouble() < BLOCKED_STAYOVER_PROBABILITY) {
                        status = TaskStatus.BLOCKED;
                    }

                    CleaningTask task = CleaningTask.builder()
                            .scheduledStart(scheduledStart)
                            .scheduledEnd(scheduledEnd)
                            .actualStart(actualStart)
                            .actualEnd(actualEnd)
                            .taskType(taskType)
                            .status(status)
                            .employee(employee)
                            .room(room)
                            .build();

                    tasksToSave.add(task);

                    // Batch save for performance
                    if (tasksToSave.size() >= BATCH_SIZE) {
                        cleaningTaskRepository.saveAll(tasksToSave);
                        totalTasks += tasksToSave.size();
                        tasksToSave.clear();

                        if (totalTasks % 10000 == 0) {
                            log.info("Generated {} cleaning tasks so far...", totalTasks);
                        }
                    }

                    // Move to next task time slot
                    currentTaskTime = currentTaskTime.plusMinutes(duration + GAP_BETWEEN_TASKS_MINUTES);
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        // Save remaining tasks
        if (!tasksToSave.isEmpty()) {
            cleaningTaskRepository.saveAll(tasksToSave);
            totalTasks += tasksToSave.size();
        }

        log.info("Historical cleaning task data generation complete. Created {} cleaning task records.", totalTasks);
    }

    /**
     * Selects a task type based on the defined distribution.
     */
    private TaskType selectTaskType() {
        double rand = random.nextDouble();
        if (rand < CHECKOUT_CLEAN_PROBABILITY) {
            return TaskType.CHECKOUT_CLEAN;
        } else if (rand < CHECKOUT_CLEAN_PROBABILITY + STAYOVER_CLEAN_PROBABILITY) {
            return TaskType.STAYOVER_CLEAN;
        } else {
            return TaskType.DEEP_CLEAN;
        }
    }

    /**
     * Returns the duration in minutes for a given task type, adjusted by room type.
     */
    private int getTaskDuration(TaskType type, Room room) {
        int baseDuration = switch (type) {
            case CHECKOUT_CLEAN -> CHECKOUT_CLEAN_DURATION;
            case STAYOVER_CLEAN -> STAYOVER_CLEAN_DURATION;
            case DEEP_CLEAN -> DEEP_CLEAN_DURATION;
        };
        return (int) Math.round(baseDuration * getRoomTypeMultiplier(room));
    }

    private double getRoomTypeMultiplier(Room room) {
        if (room == null || room.getRoomType() == null) {
            return 1.0;
        }
        String typeName = room.getRoomType().getName();
        Double multiplier = ROOM_TYPE_MULTIPLIERS.get(typeName);
        if (multiplier != null) {
            return multiplier;
        }
        // Fallback: use capacity-based multiplier (0.2 per capacity, min 1.0)
        int capacity = room.getEffectiveCapacity();
        return Math.max(1.0, 0.8 + (capacity * 0.2));
    }
}
