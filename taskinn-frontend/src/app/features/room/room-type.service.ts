import { inject, Injectable, WritableSignal } from '@angular/core';
import { RoomTypeStore } from './room-type.store';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { firstValueFrom } from 'rxjs';
import { processError } from '../../shared/util/error.utils';
import { ErrorResponse } from '../../shared/interfaces/error';
import { LiteRoomType, RoomDto, RoomTypeDto, RoomTypeRequestDto } from './room';

@Injectable({ providedIn: 'root' })
export class RoomTypeService {
    private readonly roomTypeStore: RoomTypeStore = inject(RoomTypeStore);
    private readonly http = inject(HttpClient);

    private readonly roomTypePath = `${environment.apiUrl}/room-type`;

    public async getAllRoomTypes(): Promise<void> {
        try {
            this.roomTypeStore.setLoading(true);
            this.roomTypeStore.setError(null);

            const roomTypes = await firstValueFrom(this.http.get<Array<LiteRoomType>>(`${this.roomTypePath}`));
            this.roomTypeStore.setRoomTypeList(roomTypes);
            this.roomTypeStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to load room types', `${this.roomTypePath}`);
            this.roomTypeStore.setError(errorResponse);
            this.roomTypeStore.setLoading(false);
        }
    }

    public async getRoomTypeById(id: number): Promise<RoomTypeDto> {
        try {
            return await firstValueFrom(this.http.get<RoomTypeDto>(`${this.roomTypePath}/${id}`));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to load room type', `${this.roomTypePath}/${id}`);
            throw errorResponse;
        }
    }

    public async createRoomType(dto: RoomTypeRequestDto): Promise<RoomTypeDto> {
        try {
            return await firstValueFrom(this.http.post<RoomTypeDto>(`${this.roomTypePath}`, dto));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to create room type', `${this.roomTypePath}`);
            throw errorResponse;
        }
    }

    public async updateRoomType(id: number, dto: RoomTypeRequestDto): Promise<RoomTypeDto> {
        try {
            return await firstValueFrom(this.http.put<RoomTypeDto>(`${this.roomTypePath}/${id}`, dto));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to update room type', `${this.roomTypePath}/${id}`);
            throw errorResponse;
        }
    }

    public async getRoomsByRoomTypeId(id: number): Promise<RoomDto[]> {
        try {
            return await firstValueFrom(this.http.get<RoomDto[]>(`${this.roomTypePath}/${id}/rooms`));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to load rooms', `${this.roomTypePath}/${id}/rooms`);
            throw errorResponse;
        }
    }

    public async deleteRoomType(id: number): Promise<void> {
        try {
            await firstValueFrom(this.http.delete(`${this.roomTypePath}/${id}`));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to delete room type', `${this.roomTypePath}/${id}`);
            throw errorResponse;
        }
    }

    public get loading(): WritableSignal<boolean> {
        return this.roomTypeStore.loading;
    }

    public get error(): WritableSignal<ErrorResponse | null> {
        return this.roomTypeStore.error;
    }

    public get roomTypeList(): WritableSignal<LiteRoomType[] | null> {
        return this.roomTypeStore.roomTypeList;
    }
}
