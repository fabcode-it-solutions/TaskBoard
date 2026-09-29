import { signal, Injectable, WritableSignal } from '@angular/core';
import { ErrorResponse } from '../../shared/interfaces/error';
import { Pto } from './pto/interfaces/pto';

export interface MonthShifts {
    year: number;
    month: number;
    dayIndices: number[];
}

@Injectable({ providedIn: 'root' })
export class StaffStore {
    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);
    public ptos: WritableSignal<Pto[]> = signal([]);
    public shifts: WritableSignal<MonthShifts[]> = signal([]);

    public setLoading(loading: boolean): void {
        this.loading.set(loading);
    }

    public setError(error: ErrorResponse | null): void {
        this.error.set(error);
    }

    public setPtos(ptos: Pto[]): void {
        this.ptos.set(ptos);
    }

    public setShifts(shifts: MonthShifts[]): void {
        this.shifts.set(shifts);
    }

    public addMonthShifts(monthShifts: MonthShifts): void {
        const current = this.shifts();
        // Remove existing entry for same year/month if exists
        const filtered = current.filter((s) => !(s.year === monthShifts.year && s.month === monthShifts.month));
        this.shifts.set([...filtered, monthShifts]);
    }
}
