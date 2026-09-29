import { HttpClient } from '@angular/common/http';
import { Injectable, WritableSignal } from '@angular/core';
import { inject } from '@angular/core';
import { CreatePtoRequest, Pto } from './pto/interfaces/pto';
import { environment } from '../../../environments/environment';
import { MonthShifts, StaffStore } from './staff.store';
import { firstValueFrom } from 'rxjs';
import { processError } from '../../shared/util/error.utils';
import { ErrorResponse } from '../../shared/interfaces/error';

@Injectable({
    providedIn: 'root',
})
export class StaffService {
    private readonly http = inject(HttpClient);
    private readonly staffStore = inject(StaffStore);
    // TODO: update path to 'staff' after backend is updated
    private readonly staffPath = `${environment.apiUrl}/employees`;

    public async getPTOsOfStaff(): Promise<Pto[]> {
        try {
            this.staffStore.setLoading(true);
            this.staffStore.setError(null);
            this.staffStore.setPtos([]);

            const response = await firstValueFrom(this.http.get<Pto[]>(`${this.staffPath}/pto`));
            this.staffStore.setPtos(response);
            return response;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to fetch PTOs', `${this.staffPath}/pto`);
            this.staffStore.setError(errorResponse);
            return [];
        } finally {
            this.staffStore.setLoading(false);
        }
    }

    public async createPTO(request: CreatePtoRequest): Promise<Pto | null> {
        try {
            this.staffStore.setLoading(true);
            this.staffStore.setError(null);

            const response = await firstValueFrom(this.http.post<Pto>(`${this.staffPath}/pto`, request));
            // Refresh the list after creation
            await this.getPTOsOfStaff();
            return response;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to create PTO', `${this.staffPath}/pto`);
            this.staffStore.setError(errorResponse);
            return null;
        } finally {
            this.staffStore.setLoading(false);
        }
    }

    public async deletePTO(ptoId: number): Promise<boolean> {
        try {
            this.staffStore.setLoading(true);
            this.staffStore.setError(null);

            await firstValueFrom(this.http.delete(`${this.staffPath}/pto/${ptoId}`));
            // Refresh the list after deletion
            await this.getPTOsOfStaff();
            return true;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to delete PTO', `${this.staffPath}/pto/${ptoId}`);
            this.staffStore.setError(errorResponse);
            return false;
        } finally {
            this.staffStore.setLoading(false);
        }
    }

    public async getMyShifts(year: number, month: number): Promise<number[]> {
        const url = `${this.staffPath}/shifts/${year}/${month}`;
        try {
            this.staffStore.setLoading(true);
            this.staffStore.setError(null);

            const response = await firstValueFrom(this.http.get<number[]>(url));
            this.staffStore.addMonthShifts({ year, month, dayIndices: response });
            return response;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to fetch shifts', url);
            this.staffStore.setError(errorResponse);
            return [];
        } finally {
            this.staffStore.setLoading(false);
        }
    }

    public get ptos(): WritableSignal<Pto[]> {
        return this.staffStore.ptos;
    }

    public get shifts(): WritableSignal<MonthShifts[]> {
        return this.staffStore.shifts;
    }

    public get error(): WritableSignal<ErrorResponse | null> {
        return this.staffStore.error;
    }
}
