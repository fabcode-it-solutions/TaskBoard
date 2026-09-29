import { Injectable, signal, WritableSignal } from '@angular/core';
import { ErrorResponse } from '../../shared/interfaces/error';

@Injectable({ providedIn: 'root' })
export class CheckOutStore {
    public readonly bookingId: WritableSignal<number | null> = signal<number | null>(null);
    public readonly loading: WritableSignal<boolean> = signal(false);
    public readonly error: WritableSignal<ErrorResponse | null> = signal(null);

    public setBookingId(id: number | null): void {
        this.bookingId.set(id);
    }

    public setLoading(value: boolean): void {
        this.loading.set(value);
    }

    public setError(error: ErrorResponse | null): void {
        this.error.set(error);
    }

    public reset(): void {
        this.bookingId.set(null);
        this.loading.set(false);
        this.error.set(null);
    }
}
