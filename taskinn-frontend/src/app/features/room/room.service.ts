import { inject, Injectable, WritableSignal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RoomStore } from './room.store';
import { FloorOccupancyDto, LightRoomDto, RoomDto, RoomRequestDto } from './room';
import { processError } from '../../shared/util/error.utils';
import { ErrorResponse } from '../../shared/interfaces/error';
import { environment } from '../../../environments/environment';
import { firstValueFrom } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class RoomService {
    private readonly roomStore = inject(RoomStore);
    private readonly http = inject(HttpClient);

    private readonly roomPath = `${environment.apiUrl}/room`;

    /**
     * Fetches all active light rooms from the backend and updates the store.
     */
    public async getActiveRooms(): Promise<void> {
        await this.fetchRooms(false);
    }

    /**
     * Fetches ALL rooms, including those that are permanently inactive.
     * Ideal for admin management pages.
     */
    public async getAllRooms(): Promise<void> {
        await this.fetchRooms(true);
    }

    /**
     * Private helper method to fetch rooms with an optional parameter.
     * @param includeInactive - If true, fetches all rooms. If false, fetches only active rooms.
     */
    private async fetchRooms(includeInactive: boolean): Promise<void> {
        try {
            this.roomStore.setLoading(true);
            this.roomStore.setError(null);

            let params = new HttpParams();
            if (includeInactive) {
                params = params.set('includeInactive', 'true');
            }

            const rooms = await firstValueFrom(this.http.get<LightRoomDto[]>(this.roomPath, { params }));
            this.roomStore.setRoomList(rooms);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to load rooms', this.roomPath);
            this.roomStore.setError(errorResponse);
        } finally {
            this.roomStore.setLoading(false);
        }
    }

    /**
     * Sends a request to the backend to permanently disable a room.
     * @param roomId The ID of the room to disable.
     */
    public async disableRoom(roomId: number): Promise<void> {
        try {
            this.roomStore.setLoading(true);
            this.roomStore.setError(null);

            // The endpoint for permanent deactivation
            await firstValueFrom(this.http.delete<void>(`${this.roomPath}/${roomId}/disable`));
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to disable room ${roomId}`,
                `${this.roomPath}/${roomId}/disable`,
            );
            this.roomStore.setError(errorResponse);
            // Re-throw the error so the component knows the operation failed
            throw errorResponse;
        } finally {
            this.roomStore.setLoading(false);
        }
    }

    public async getRoomById(roomId: number): Promise<void> {
        try {
            this.roomStore.setLoading(true);
            this.roomStore.setError(null);

            const room = await firstValueFrom(this.http.get<RoomDto>(`${this.roomPath}/${roomId}`));
            this.roomStore.setSelectedRoom(room);
            this.roomStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to load room with ID ${roomId}`,
                `${this.roomPath}/${roomId}`,
            );
            this.roomStore.setError(errorResponse);
            this.roomStore.setLoading(false);
        }
    }

    /**
     * Fetches a room by ID and returns it directly.
     * @param roomId The ID of the room to fetch.
     * @returns The room DTO.
     */
    public async fetchRoomById(roomId: number): Promise<RoomDto> {
        try {
            return await firstValueFrom(this.http.get<RoomDto>(`${this.roomPath}/${roomId}`));
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to load room with ID ${roomId}`,
                `${this.roomPath}/${roomId}`,
            );
            throw errorResponse;
        }
    }

    public async createRoom(roomData: RoomRequestDto): Promise<void> {
        try {
            this.roomStore.setLoading(true);
            this.roomStore.setError(null);

            const createdRoom = await firstValueFrom(this.http.post<RoomDto>(`${this.roomPath}`, roomData));
            this.roomStore.setSelectedRoom(createdRoom);
            this.roomStore.setLoading(false);

            // Refresh room list after creation
            await this.getAllRooms();
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to create room', `${this.roomPath}`);
            this.roomStore.setError(errorResponse);
            this.roomStore.setLoading(false);
        }
    }

    public async updateRoom(roomId: number, roomData: RoomRequestDto): Promise<void> {
        try {
            this.roomStore.setLoading(true);
            this.roomStore.setError(null);

            const updatedRoom = await firstValueFrom(this.http.put<RoomDto>(`${this.roomPath}/${roomId}`, roomData));
            this.roomStore.setSelectedRoom(updatedRoom);
            this.roomStore.setLoading(false);

            // Refresh room list after update
            await this.getAllRooms();
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to update room with ID ${roomId}`,
                `${this.roomPath}/${roomId}`,
            );
            this.roomStore.setError(errorResponse);
            this.roomStore.setLoading(false);
        }
    }

    public async getRoomsByFloor(floor: number): Promise<void> {
        try {
            this.roomStore.setLoading(true);
            this.roomStore.setError(null);

            const rooms = await firstValueFrom(this.http.get<Array<RoomDto>>(`${this.roomPath}/floor/${floor}`));
            this.roomStore.setRoomsByFloor(rooms);
            this.roomStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to load rooms for floor ${floor}`,
                `${this.roomPath}/floor/${floor}`,
            );
            this.roomStore.setError(errorResponse);
            this.roomStore.setLoading(false);
        }
    }

    public async getMaxFloor(): Promise<void> {
        try {
            this.roomStore.setLoading(true);
            this.roomStore.setError(null);

            const maxFloor = await firstValueFrom(this.http.get<number>(`${this.roomPath}/max-floor`));
            this.roomStore.setMaxFloor(maxFloor);
            this.roomStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(error, `Failed to load max floor`, `${this.roomPath}/max-floor`);
            this.roomStore.setError(errorResponse);
            this.roomStore.setLoading(false);
        }
    }

    public async updateSingleRoomInFloorList(roomId: number): Promise<void> {
        try {
            const room = await firstValueFrom(this.http.get<RoomDto>(`${this.roomPath}/${roomId}`));
            this.roomStore.updateRoomInFloorList(room);
        } catch (error: unknown) {
            console.warn(`Failed to update room ${roomId} in floor list`, error);
        }
    }

    /**
     * Fetches available rooms for a specific date range (for manual booking)
     * @param checkInDate Check-in date in dd-MM-yyyy format
     * @param checkOutDate Check-out date in dd-MM-yyyy format
     * @returns List of available rooms
     */
    public async getAvailableRooms(checkInDate: string, checkOutDate: string): Promise<LightRoomDto[]> {
        try {
            this.roomStore.setLoading(true);
            this.roomStore.setError(null);

            const formattedCheckIn = this.convertToIsoDate(checkInDate);
            const formattedCheckOut = this.convertToIsoDate(checkOutDate);

            const params = new HttpParams().set('checkInDate', formattedCheckIn).set('checkOutDate', formattedCheckOut);

            return await firstValueFrom(this.http.get<LightRoomDto[]>(`${this.roomPath}/available`, { params }));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to fetch available rooms', `${this.roomPath}/available`);
            this.roomStore.setError(errorResponse);
            return [];
        } finally {
            this.roomStore.setLoading(false);
        }
    }

    public async copyFloor(fromFloor: number, toFloor: number): Promise<RoomDto[]> {
        try {
            this.roomStore.setLoading(true);
            this.roomStore.setError(null);

            const params = new HttpParams().set('fromFloor', fromFloor.toString()).set('toFloor', toFloor.toString());

            return await firstValueFrom(this.http.post<RoomDto[]>(`${this.roomPath}/copy-floor`, null, { params }));
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to copy floor ${fromFloor} to floor ${toFloor}`,
                `${this.roomPath}/copy-floor`,
            );
            this.roomStore.setError(errorResponse);
            throw errorResponse;
        } finally {
            this.roomStore.setLoading(false);
        }
    }

    /**
     * Fetches today's occupancy rate
     * @returns Today's occupancy rate as a percentage (0-100)
     */
    public async getTodaysOccupancyRate(): Promise<number | null> {
        try {
            this.roomStore.setLoading(true);
            this.roomStore.setError(null);

            const occupancyRate = await firstValueFrom(this.http.get<number>(`${this.roomPath}/occupancy-rate/today`));
            return occupancyRate;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                "Failed to fetch today's occupancy rate",
                `${this.roomPath}/occupancy-rate/today`,
            );
            this.roomStore.setError(errorResponse);
            return null;
        } finally {
            this.roomStore.setLoading(false);
        }
    }

    /**
     * Fetches the floor occupancy distribution
     * @returns List of floor occupancy data
     */
    public async getFloorOccupancyDistribution(): Promise<FloorOccupancyDto[]> {
        try {
            this.roomStore.setLoading(true);
            this.roomStore.setError(null);

            const floorOccupancy = await firstValueFrom(
                this.http.get<FloorOccupancyDto[]>(`${this.roomPath}/floor-occupancy`),
            );
            return floorOccupancy;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to fetch floor occupancy distribution',
                `${this.roomPath}/floor-occupancy`,
            );
            this.roomStore.setError(errorResponse);
            return [];
        } finally {
            this.roomStore.setLoading(false);
        }
    }

    // --- Store Accessors ---

    public get loading(): WritableSignal<boolean> {
        return this.roomStore.loading;
    }

    public get error(): WritableSignal<ErrorResponse | null> {
        return this.roomStore.error;
    }

    public get roomList(): WritableSignal<LightRoomDto[] | null> {
        return this.roomStore.roomList;
    }

    public get selectedRoom(): WritableSignal<RoomDto | null> {
        return this.roomStore.selectedRoom;
    }

    public get roomsByFloor(): WritableSignal<RoomDto[] | null> {
        return this.roomStore.roomsByFloor;
    }

    public get maxFloor(): WritableSignal<number | null> {
        return this.roomStore.maxFloor;
    }

    /**
     * Converts date from dd-MM-yyyy format to yyyy-MM-dd format
     */
    private convertToIsoDate(dateStr: string): string {
        const parts = dateStr.split('-');
        if (parts.length === 3) {
            return `${parts[2]}-${parts[1]}-${parts[0]}`;
        }
        return dateStr;
    }
}
