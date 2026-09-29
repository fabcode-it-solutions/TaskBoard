package tuwien.taskinn.services.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tuwien.taskinn.dto.response.CleaningTaskDto;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.CleaningTask;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.enums.TaskStatus;
import tuwien.taskinn.enums.TaskType;
import tuwien.taskinn.events.CleaningTaskUpdatedEvent;
import tuwien.taskinn.mapper.CleaningTaskMapper;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.CleaningTaskRepository;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.services.CleaningTaskService;

@Service
@RequiredArgsConstructor
public class CleaningTaskServiceImpl implements CleaningTaskService {

    private final CleaningTaskRepository cleaningTaskRepository;
    private final EmployeeRepository employeeRepository;
    private final RoomRepository roomRepository;
    private final CleaningTaskMapper cleaningTaskMapper;
    private final BookingRepository bookingRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CleaningTaskDto startCleaning(Long roomId, String employeeEmail) {
        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new IllegalStateException("Employee not found: " + employeeEmail));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));

        cleaningTaskRepository.findByRoomIdAndStatus(roomId, TaskStatus.IN_PROGRESS).ifPresent(task -> {
            throw new IllegalStateException(
                    "Room " + room.getRoomNumber() + " already has a cleaning task in progress");
        });

        // Check if employee already has an active cleaning task
        List<CleaningTask> employeeInProgressTasks =
                cleaningTaskRepository.findByEmployeeIdAndStatus(employee.getId(), TaskStatus.IN_PROGRESS);
        if (!employeeInProgressTasks.isEmpty()) {
            CleaningTask activeTask = employeeInProgressTasks.get(0);
            throw new IllegalStateException(
                    "You already have a cleaning task in progress for Room " + activeTask.getRoom().getRoomNumber()
                            + ". Please finish the current cleaning before starting a new one.");
        }

        List<CleaningTask> pendingTasks = cleaningTaskRepository.findPendingTasksForRoom(roomId, TaskStatus.PENDING,
                LocalDate.now().atTime(23, 59, 59));

        if (pendingTasks.isEmpty()) {
            throw new IllegalStateException("No scheduled cleaning task found for room " + room.getRoomNumber()
                    + ". Please ensure a cleaning task is scheduled before starting.");
        }

        CleaningTask taskToStart = pendingTasks.getFirst();

        if (taskToStart.getEmployee() != null && !taskToStart.getEmployee().getId().equals(employee.getId())) {
            throw new IllegalStateException("Employee " + employeeEmail + " is not assigned to this cleaning task. "
                    + "Assigned employee: " + taskToStart.getEmployee().getEmail());
        }

        taskToStart.setStatus(TaskStatus.IN_PROGRESS);
        taskToStart.setEmployee(employee);
        taskToStart.setTaskType(determineTaskType(room.getStatus()));
        taskToStart.setActualStart(LocalDateTime.now());

        room.setStatus(RoomStatus.CLEANING);
        roomRepository.save(room);

        this.eventPublisher.publishEvent(new CleaningTaskUpdatedEvent(room.getId(), RoomStatus.CLEANING));

        CleaningTask saved = cleaningTaskRepository.save(taskToStart);
        return cleaningTaskMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CleaningTaskDto finishCleaning(Long roomId, String employeeEmail, String notes) {
        CleaningTask task = cleaningTaskRepository.findByRoomIdAndStatus(roomId, TaskStatus.IN_PROGRESS)
                .orElseThrow(() -> new IllegalStateException("No cleaning task in progress for room ID: " + roomId));

        task.setStatus(TaskStatus.COMPLETED);
        task.setActualEnd(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) {
            task.setNotes(notes);
        }
        CleaningTask saved = cleaningTaskRepository.save(task);

        Room room = task.getRoom();

        Optional<Booking> booking = bookingRepository.findCurrentBookingForRoom(room, LocalDate.now());
        boolean isOccupied = booking.isPresent();

        if (isOccupied) {
            room.setStatus(RoomStatus.OCCUPIED);
        } else {
            room.setStatus(RoomStatus.AVAILABLE);
        }

        Room updatedRoom = roomRepository.save(room);

        this.eventPublisher.publishEvent(new CleaningTaskUpdatedEvent(updatedRoom.getId(), updatedRoom.getStatus()));

        return cleaningTaskMapper.toDto(saved);
    }

    @Override
    public CleaningTaskDto getCurrentTaskForRoom(Long roomId) {
        return cleaningTaskRepository.findByRoomIdAndStatus(roomId, TaskStatus.IN_PROGRESS)
                .map(cleaningTaskMapper::toDto)
                .orElse(null);
    }

    @Override
    public List<CleaningTaskDto> getAllInProgressTasks() {
        return cleaningTaskRepository.findAll()
                .stream()
                .filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS)
                .map(cleaningTaskMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CleaningTaskDto skipCleaning(Long roomId, String employeeEmail, String reason) {
        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new IllegalStateException("Employee not found: " + employeeEmail));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));

        // Check for existing IN_PROGRESS task
        CleaningTask existingTask =
                cleaningTaskRepository.findByRoomIdAndStatus(roomId, TaskStatus.IN_PROGRESS).orElse(null);

        CleaningTask taskToSkip;

        if (existingTask != null) {
            // If there's an in-progress task, block it
            taskToSkip = existingTask;
        } else {
            // Find a pending task
            List<CleaningTask> pendingTasks = cleaningTaskRepository.findPendingTasksForRoom(roomId, TaskStatus.PENDING,
                    LocalDate.now().atTime(23, 59, 59));

            if (pendingTasks.isEmpty()) {
                throw new IllegalStateException("No scheduled cleaning task found for room " + room.getRoomNumber()
                        + ". Cannot skip a task that hasn't been scheduled.");
            }

            taskToSkip = pendingTasks.getFirst();
        }

        if (taskToSkip.getEmployee() != null && !taskToSkip.getEmployee().getId().equals(employee.getId())) {
            throw new IllegalStateException("Employee " + employeeEmail + " is not assigned to this cleaning task. "
                    + "Assigned employee: " + taskToSkip.getEmployee().getEmail());
        }

        // Mark as blocked with reason
        taskToSkip.setStatus(TaskStatus.BLOCKED);
        taskToSkip.setEmployee(employee);
        taskToSkip.setNotes(reason != null && !reason.isBlank() ? reason : "Do Not Disturb");
        if (existingTask != null) {
            taskToSkip.setActualEnd(LocalDateTime.now());
        }

        // Revert room status if it was set to CLEANING
        if (room.getStatus() == RoomStatus.CLEANING) {
            room.setStatus(RoomStatus.DIRTY);
            roomRepository.save(room);
        }

        CleaningTask saved = cleaningTaskRepository.save(taskToSkip);
        return cleaningTaskMapper.toDto(saved);
    }

    /**
     * Determine task type based on room status
     */
    private TaskType determineTaskType(RoomStatus roomStatus) {
        return switch (roomStatus) {
            case DIRTY -> TaskType.CHECKOUT_CLEAN;
            case OCCUPIED -> TaskType.STAYOVER_CLEAN;
            default -> TaskType.CHECKOUT_CLEAN;
        };
    }
}
