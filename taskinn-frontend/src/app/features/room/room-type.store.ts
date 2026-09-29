import { Injectable, signal, WritableSignal } from '@angular/core';
import { ErrorResponse } from '../../shared/interfaces/error';
import { LiteRoomType } from './room';

@Injectable({ providedIn: 'root' })
export class RoomTypeStore {
    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);
    public roomTypeList: WritableSignal<LiteRoomType[] | null> = signal(null);

    public setLoading(loading: boolean): void {
        this.loading.set(loading);
    }

    public setError(error: ErrorResponse | null): void {
        this.error.set(error);
    }

    public setRoomTypeList(roomTypeList: LiteRoomType[] | null): void {
        this.roomTypeList.set(roomTypeList);
    }
}
