import { inject, Injectable, WritableSignal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { StaircaseStore } from './staircase.store';
import { StaircaseDto, StaircaseRequestDto } from './staircase';
import { processError } from '../../shared/util/error.utils';
import { ErrorResponse } from '../../shared/interfaces/error';
import { environment } from '../../../environments/environment';
import { firstValueFrom } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class StaircaseService {
    private readonly staircaseStore = inject(StaircaseStore);
    private readonly http = inject(HttpClient);

    private readonly staircasePath = `${environment.apiUrl}/staircase`;

    public async getStaircasesByFloor(floor: number): Promise<StaircaseDto[]> {
        try {
            this.staircaseStore.setLoading(true);
            this.staircaseStore.setError(null);

            const staircases = await firstValueFrom(
                this.http.get<StaircaseDto[]>(`${this.staircasePath}/floor/${floor}`),
            );
            this.staircaseStore.setStaircasesByFloor(staircases);
            return staircases;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to load staircases for floor ${floor}`,
                `${this.staircasePath}/floor/${floor}`,
            );
            this.staircaseStore.setError(errorResponse);
            return [];
        } finally {
            this.staircaseStore.setLoading(false);
        }
    }

    public async createStaircase(staircaseData: StaircaseRequestDto): Promise<StaircaseDto | null> {
        try {
            this.staircaseStore.setLoading(true);
            this.staircaseStore.setError(null);

            const createdStaircase = await firstValueFrom(
                this.http.post<StaircaseDto>(this.staircasePath, staircaseData),
            );
            this.staircaseStore.addStaircase(createdStaircase);
            return createdStaircase;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to create staircase', this.staircasePath);
            this.staircaseStore.setError(errorResponse);
            return null;
        } finally {
            this.staircaseStore.setLoading(false);
        }
    }

    public async deleteStaircase(id: number): Promise<boolean> {
        try {
            this.staircaseStore.setLoading(true);
            this.staircaseStore.setError(null);

            await firstValueFrom(this.http.delete<void>(`${this.staircasePath}/${id}`));
            this.staircaseStore.removeStaircase(id);
            return true;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to delete staircase with ID ${id}`,
                `${this.staircasePath}/${id}`,
            );
            this.staircaseStore.setError(errorResponse);
            return false;
        } finally {
            this.staircaseStore.setLoading(false);
        }
    }

    public get loading(): WritableSignal<boolean> {
        return this.staircaseStore.loading;
    }

    public get error(): WritableSignal<ErrorResponse | null> {
        return this.staircaseStore.error;
    }

    public get staircases(): WritableSignal<StaircaseDto[]> {
        return this.staircaseStore.staircases;
    }

    public get staircasesByFloor(): WritableSignal<StaircaseDto[]> {
        return this.staircaseStore.staircasesByFloor;
    }
}
