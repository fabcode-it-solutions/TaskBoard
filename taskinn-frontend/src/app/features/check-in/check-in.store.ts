import { Injectable, signal, WritableSignal } from '@angular/core';
import { ErrorResponse } from '../../shared/interfaces/error';
import { RoomForCheckInResponse } from './interfaces/room-for-check-in.response';
import { BookerInfo } from './interfaces/check-in.request';

@Injectable({ providedIn: 'root' })
export class CheckInStore {
    private readonly _bookingId: WritableSignal<number | null> = signal<number | null>(null);
    private readonly _rooms: WritableSignal<RoomForCheckInResponse[] | null> = signal<RoomForCheckInResponse[] | null>(
        null,
    );
    private readonly _bookerInfo: WritableSignal<BookerInfo | null> = signal<BookerInfo | null>(null);
    private readonly _loading: WritableSignal<boolean> = signal(false);
    private readonly _error: WritableSignal<ErrorResponse | null> = signal(null);

    public readonly bookingId = this._bookingId.asReadonly();
    public readonly rooms = this._rooms.asReadonly();
    public readonly bookerInfo = this._bookerInfo.asReadonly();
    public readonly loading = this._loading.asReadonly();
    public readonly error = this._error.asReadonly();

    public setBookingId(id: number): void {
        this._bookingId.set(id);
    }
    public setRooms(rooms: RoomForCheckInResponse[]): void {
        this._rooms.set(rooms);
    }
    public setBookerInfo(bookerInfo: BookerInfo | null): void {
        this._bookerInfo.set(bookerInfo);
    }
    public setLoading(value: boolean): void {
        this._loading.set(value);
    }
    public setError(error: ErrorResponse | null): void {
        this._error.set(error);
    }

    public getBookingId(): number | null {
        return this._bookingId();
    }
    public getRooms(): RoomForCheckInResponse[] | null {
        return this._rooms();
    }
    public getBookerInfo(): BookerInfo | null {
        return this._bookerInfo();
    }
    public getLoading(): boolean {
        return this._loading();
    }
    public getError(): ErrorResponse | null {
        return this._error();
    }

    public reset(): void {
        this._bookingId.set(null);
        this._rooms.set(null);
        this._bookerInfo.set(null);
        this._loading.set(false);
        this._error.set(null);
    }
}
