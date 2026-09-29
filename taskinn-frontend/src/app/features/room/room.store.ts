import { Injectable, signal, WritableSignal } from '@angular/core';
import { ErrorResponse } from '../../shared/interfaces/error';
import { LightRoomDto, RoomDto } from './room';

@Injectable({ providedIn: 'root' })
export class RoomStore {
    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);
    public roomList: WritableSignal<LightRoomDto[] | null> = signal(null);
    public selectedRoom: WritableSignal<RoomDto | null> = signal(null);
    public roomsByFloor: WritableSignal<RoomDto[] | null> = signal(null);
    public maxFloor: WritableSignal<number | null> = signal(null);

    public setLoading(loading: boolean): void {
        this.loading.set(loading);
    }

    public setError(error: ErrorResponse | null): void {
        this.error.set(error);
    }

    public setRoomList(roomList: LightRoomDto[] | null): void {
        this.roomList.set(roomList);
    }

    public setSelectedRoom(room: RoomDto | null): void {
        this.selectedRoom.set(room);
    }

    public setRoomsByFloor(rooms: RoomDto[] | null): void {
        this.roomsByFloor.set(rooms);
    }

    public setMaxFloor(floor: number | null): void {
        this.maxFloor.set(floor);
    }

    public updateRoomInFloorList(updatedRoom: RoomDto): void {
        const currentRooms = this.roomsByFloor();
        if (currentRooms) {
            const updatedRooms = currentRooms.map((room) => (room.id === updatedRoom.id ? updatedRoom : room));
            this.roomsByFloor.set(updatedRooms);
        }
    }
}
