import { Injectable, signal, WritableSignal } from '@angular/core';
import { ErrorResponse } from '../../shared/interfaces/error';
import { CleaningTaskDto } from './cleaning-task';

@Injectable({ providedIn: 'root' })
export class CleaningTaskStore {
    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);
    public currentTask: WritableSignal<CleaningTaskDto | null> = signal(null);
    public inProgressTasks: WritableSignal<CleaningTaskDto[]> = signal([]);
    public dailyTasks: WritableSignal<CleaningTaskDto[]> = signal([]);

    public setLoading(loading: boolean): void {
        this.loading.set(loading);
    }

    public setError(error: ErrorResponse | null): void {
        this.error.set(error);
    }

    public setCurrentTask(task: CleaningTaskDto | null): void {
        this.currentTask.set(task);
    }

    public setInProgressTasks(tasks: CleaningTaskDto[]): void {
        this.inProgressTasks.set(tasks);
    }

    public setDailyTasks(tasks: CleaningTaskDto[]): void {
        this.dailyTasks.set(tasks);
    }
}
