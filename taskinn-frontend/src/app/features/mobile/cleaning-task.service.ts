import { computed, inject, Injectable, Signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { processError } from '../../shared/util/error.utils';
import { ErrorResponse } from '../../shared/interfaces/error';
import { CleaningTaskStore } from './cleaning-task.store';
import {
    CleaningTaskDto,
    StartCleaningRequestDto,
    FinishCleaningRequestDto,
    SkipCleaningRequestDto,
} from './cleaning-task';
import { RoomService } from '../room/room.service';
import { RoomStatus } from '../room/room';

@Injectable({ providedIn: 'root' })
export class CleaningTaskService {
    private readonly cleaningTaskStore = inject(CleaningTaskStore);
    private readonly http = inject(HttpClient);
    private readonly roomService = inject(RoomService);

    private readonly cleaningTaskPath = `${environment.apiUrl}/cleaning-task`;

    public async startCleaning(roomId: number): Promise<CleaningTaskDto | null> {
        try {
            this.cleaningTaskStore.setLoading(true);
            this.cleaningTaskStore.setError(null);

            const requestDto: StartCleaningRequestDto = { roomId };

            const task = await firstValueFrom(
                this.http.post<CleaningTaskDto>(`${this.cleaningTaskPath}/start`, requestDto),
            );

            this.cleaningTaskStore.setCurrentTask(task);
            return task;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to start cleaning for room ID ${roomId}`,
                `${this.cleaningTaskPath}/start`,
            );
            this.cleaningTaskStore.setError(errorResponse);
            return null;
        } finally {
            this.cleaningTaskStore.setLoading(false);
        }
    }

    public async finishCleaning(roomId: number, notes?: string): Promise<CleaningTaskDto | null> {
        try {
            this.cleaningTaskStore.setLoading(true);
            this.cleaningTaskStore.setError(null);

            const requestDto: FinishCleaningRequestDto = { roomId, notes };

            const task = await firstValueFrom(
                this.http.post<CleaningTaskDto>(`${this.cleaningTaskPath}/finish`, requestDto),
            );

            this.cleaningTaskStore.setCurrentTask(null); // Clear current task
            return task;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to finish cleaning for room ID ${roomId}`,
                `${this.cleaningTaskPath}/finish`,
            );
            this.cleaningTaskStore.setError(errorResponse);
            return null;
        } finally {
            this.cleaningTaskStore.setLoading(false);
        }
    }

    public async getCurrentTaskForRoom(roomId: number): Promise<CleaningTaskDto | null> {
        try {
            this.cleaningTaskStore.setLoading(true);
            this.cleaningTaskStore.setError(null);

            const task = await firstValueFrom(
                this.http.get<CleaningTaskDto>(`${this.cleaningTaskPath}/room/${roomId}`),
            );

            this.cleaningTaskStore.setCurrentTask(task);
            return task;
        } catch (error: unknown) {
            // 204 No Content is expected if no task exists - don't treat as error
            const errorResponse = processError(
                error,
                `Failed to fetch cleaning task for room ID ${roomId}`,
                `${this.cleaningTaskPath}/room/${roomId}`,
            );

            if (errorResponse.status === 204) {
                this.cleaningTaskStore.setCurrentTask(null);
                return null;
            }

            this.cleaningTaskStore.setError(errorResponse);
            return null;
        } finally {
            this.cleaningTaskStore.setLoading(false);
        }
    }

    public async getAllInProgressTasks(): Promise<CleaningTaskDto[]> {
        try {
            const tasks = await firstValueFrom(
                this.http.get<CleaningTaskDto[]>(`${this.cleaningTaskPath}/in-progress`),
            );

            this.cleaningTaskStore.setInProgressTasks(tasks);
            return tasks;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to fetch in-progress cleaning tasks',
                `${this.cleaningTaskPath}/in-progress`,
            );
            this.cleaningTaskStore.setError(errorResponse);
            return [];
        }
    }

    /**
     * Skip a cleaning task (e.g., due to "Do Not Disturb")
     * @param roomId The room ID
     * @param reason Optional reason for skipping
     * @returns The blocked cleaning task or null on error
     */
    public async skipCleaning(roomId: number, reason?: string): Promise<CleaningTaskDto | null> {
        try {
            this.cleaningTaskStore.setLoading(true);
            this.cleaningTaskStore.setError(null);

            const requestDto: SkipCleaningRequestDto = {
                roomId,
                reason: reason || 'Do Not Disturb',
            };

            const task = await firstValueFrom(
                this.http.post<CleaningTaskDto>(`${this.cleaningTaskPath}/skip`, requestDto),
            );

            this.cleaningTaskStore.setCurrentTask(null);
            return task;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to skip cleaning for room ID ${roomId}`,
                `${this.cleaningTaskPath}/skip`,
            );
            this.cleaningTaskStore.setError(errorResponse);
            return null;
        } finally {
            this.cleaningTaskStore.setLoading(false);
        }
    }

    /**
     * Get daily tasks for a specific employee
     * @param employeeId The employee ID
     * @param year The year (min 2024)
     * @param month The month (1-12)
     * @param day The day (1-31)
     * @returns List of cleaning tasks for the employee on the specified day
     */
    public async getDailyTasksForEmployee(
        employeeId: number,
        year: number,
        month: number,
        day: number,
    ): Promise<CleaningTaskDto[]> {
        try {
            this.cleaningTaskStore.setLoading(true);
            this.cleaningTaskStore.setError(null);

            const tasks = await firstValueFrom(
                this.http.get<CleaningTaskDto[]>(
                    `${environment.apiUrl}/task-scheduling/schedule/${year}/${month}/${day}/${employeeId}`,
                ),
            );

            const tasksWithRoomStatus = await this.augmentTasksWithRoomStatus(tasks);

            this.cleaningTaskStore.setDailyTasks(tasksWithRoomStatus);
            return tasksWithRoomStatus;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to fetch tasks for employee ${employeeId}`,
                `/task-scheduling/schedule/${year}/${month}/${day}/${employeeId}`,
            );
            this.cleaningTaskStore.setError(errorResponse);
            return [];
        } finally {
            this.cleaningTaskStore.setLoading(false);
        }
    }

    /**
     * Augment tasks with room status information
     * @param tasks The tasks to augment
     * @returns Tasks with roomStatus field populated
     */
    private async augmentTasksWithRoomStatus(tasks: CleaningTaskDto[]): Promise<CleaningTaskDto[]> {
        if (tasks.length === 0) {
            return tasks;
        }

        try {
            const uniqueRoomIds = [...new Set(tasks.map((task) => task.roomId))];

            const roomPromises = uniqueRoomIds.map((roomId) =>
                this.roomService.fetchRoomById(roomId).catch(() => null),
            );
            const rooms = await Promise.all(roomPromises);

            const roomStatusMap = new Map<number, RoomStatus>();
            rooms.forEach((room) => {
                if (room !== null) {
                    roomStatusMap.set(room.id, room.status);
                }
            });

            return tasks.map((task) => ({
                ...task,
                roomStatus: roomStatusMap.get(task.roomId),
            }));
        } catch (error: unknown) {
            console.warn('Failed to augment tasks with room status:', error);
            return tasks;
        }
    }

    /**
     * Check if a specific employee has any active cleaning task
     */
    public getActiveTaskForEmployee(employeeId: number): CleaningTaskDto | null {
        const inProgressTasks = this.inProgressTasks();
        return inProgressTasks.find((task) => task.employeeId === employeeId) ?? null;
    }

    /**
     * Clear the error state
     */
    public clearError(): void {
        this.cleaningTaskStore.setError(null);
    }

    // Store accessors
    public readonly loading: Signal<boolean> = computed(() => this.cleaningTaskStore.loading());

    public readonly error: Signal<ErrorResponse | null> = computed(() => this.cleaningTaskStore.error());

    public readonly currentTask: Signal<CleaningTaskDto | null> = computed(() => this.cleaningTaskStore.currentTask());

    public readonly inProgressTasks: Signal<CleaningTaskDto[]> = computed(() =>
        this.cleaningTaskStore.inProgressTasks(),
    );

    public readonly dailyTasks: Signal<CleaningTaskDto[]> = computed(() => this.cleaningTaskStore.dailyTasks());
}
