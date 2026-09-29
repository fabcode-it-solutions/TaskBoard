import { Injectable, signal, WritableSignal } from '@angular/core';
import { DashboardRoomDto } from './interfaces/room-dashboard.interfaces';
import { ErrorResponse } from '../../shared/interfaces/error';

@Injectable({
    providedIn: 'root',
})
export class RoomDashboardStore {
    public rooms: WritableSignal<Map<number, DashboardRoomDto>> = signal(new Map());
    public loading: WritableSignal<boolean> = signal(true);
    public error: WritableSignal<ErrorResponse | null> = signal(null);

    public setAllRooms(rooms: DashboardRoomDto[]): void {
        const roomMap = new Map(rooms.map((room) => [room.roomId, room]));
        this.rooms.set(roomMap);
    }

    public updateSingleRoom(updatedRoom: DashboardRoomDto): void {
        this.rooms.update((currentMap) => {
            const newMap = new Map(currentMap);
            newMap.set(updatedRoom.roomId, updatedRoom);
            return newMap;
        });
    }

    public setLoading(isLoading: boolean): void {
        this.loading.set(isLoading);
    }

    public setError(error: ErrorResponse | null): void {
        this.error.set(error);
    }
}
