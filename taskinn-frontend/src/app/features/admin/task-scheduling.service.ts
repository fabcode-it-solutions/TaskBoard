import { inject, Injectable, signal, WritableSignal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CleaningTaskDto, CleaningTaskSaveDto, TaskScheduleSolveRequestDto } from './interfaces/task-scheduling';
import { firstValueFrom } from 'rxjs';
import { ErrorResponse } from '../../shared/interfaces/error';
import { processError } from '../../shared/util/error.utils';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TaskSchedulingService {
    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);

    private readonly taskSchedulingPath = `${environment.apiUrl}/task-scheduling`;
    private readonly http: HttpClient = inject(HttpClient);

    public async fetchTaskScheduleForDay(year: number, month: number, day: number): Promise<CleaningTaskDto[]> {
        const url = `${this.taskSchedulingPath}/schedule/${year}/${month}/${day}`;
        try {
            this.loading.set(true);
            this.error.set(null);
            return await firstValueFrom(this.http.get<CleaningTaskDto[]>(url));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to fetch task schedule', url);
            this.error.set(errorResponse);
            return [];
        } finally {
            this.loading.set(false);
        }
    }

    public async fetchTaskScheduleForEmployee(
        year: number,
        month: number,
        day: number,
        employeeId: number,
    ): Promise<CleaningTaskDto[]> {
        const url = `${this.taskSchedulingPath}/schedule/${year}/${month}/${day}/${employeeId}`;
        try {
            this.loading.set(true);
            this.error.set(null);
            return await firstValueFrom(this.http.get<CleaningTaskDto[]>(url));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to fetch employee task schedule', url);
            this.error.set(errorResponse);
            return [];
        } finally {
            this.loading.set(false);
        }
    }

    public async saveTaskSchedule(
        year: number,
        month: number,
        day: number,
        assignments: CleaningTaskSaveDto[],
    ): Promise<void> {
        const url = `${this.taskSchedulingPath}/schedule/${year}/${month}/${day}`;
        try {
            this.loading.set(true);
            this.error.set(null);
            await firstValueFrom(this.http.put<void>(url, assignments));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to save task schedule', url);
            this.error.set(errorResponse);
        } finally {
            this.loading.set(false);
        }
    }

    public async fetchOptimizedTaskSchedule(request: TaskScheduleSolveRequestDto): Promise<CleaningTaskDto[]> {
        const url = `${this.taskSchedulingPath}/optimized-schedule`;
        try {
            this.loading.set(true);
            this.error.set(null);
            return await firstValueFrom(this.http.post<CleaningTaskDto[]>(url, request));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to generate optimized schedule', url);
            this.error.set(errorResponse);
            return [];
        } finally {
            this.loading.set(false);
        }
    }
}
