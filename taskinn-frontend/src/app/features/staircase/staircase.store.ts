import { Injectable, signal, WritableSignal } from '@angular/core';
import { ErrorResponse } from '../../shared/interfaces/error';
import { StaircaseDto } from './staircase';

@Injectable({ providedIn: 'root' })
export class StaircaseStore {
    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);
    public staircases: WritableSignal<StaircaseDto[]> = signal([]);
    public staircasesByFloor: WritableSignal<StaircaseDto[]> = signal([]);

    public setLoading(loading: boolean): void {
        this.loading.set(loading);
    }

    public setError(error: ErrorResponse | null): void {
        this.error.set(error);
    }

    public setStaircases(staircases: StaircaseDto[]): void {
        this.staircases.set(staircases);
    }

    public setStaircasesByFloor(staircases: StaircaseDto[]): void {
        this.staircasesByFloor.set(staircases);
    }

    public addStaircase(staircase: StaircaseDto): void {
        this.staircases.update((current) => [...current, staircase]);
    }

    public updateStaircase(staircase: StaircaseDto): void {
        this.staircases.update((current) => current.map((s) => (s.id === staircase.id ? staircase : s)));
        this.staircasesByFloor.update((current) => current.map((s) => (s.id === staircase.id ? staircase : s)));
    }

    public removeStaircase(id: number): void {
        this.staircases.update((current) => current.filter((s) => s.id !== id));
        this.staircasesByFloor.update((current) => current.filter((s) => s.id !== id));
    }
}
