import { inject, Injectable, signal, WritableSignal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import {
    EmployeeMonthShift,
    PackedShiftScheduleTemplate,
    SchedulableEmployee,
    SchedulingRole,
    ShiftScheduleSolveRequest,
    ShiftScheduleTemplate,
} from './interfaces/shift-scheduling';
import { firstValueFrom } from 'rxjs';
import { ErrorResponse } from '../../shared/interfaces/error';
import { processError } from '../../shared/util/error.utils';

@Injectable({ providedIn: 'root' })
export class ShiftSchedulingService {
    private readonly http: HttpClient = inject(HttpClient);

    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);

    private readonly shiftSchedulingPath = `${environment.apiUrl}/shift-scheduling`;

    public async fetchSchedulableEmployees(): Promise<SchedulableEmployee[]> {
        const url = `${this.shiftSchedulingPath}/employees`;
        try {
            this.loading.set(true);
            this.error.set(null);
            return await firstValueFrom(this.http.get<SchedulableEmployee[]>(url));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to fetch schedulable employees', url);
            this.error.set(errorResponse);
            return [];
        } finally {
            this.loading.set(false);
        }
    }

    public async fetchShiftsForMonth(year: number, month: number): Promise<EmployeeMonthShift[]> {
        const url = `${this.shiftSchedulingPath}/shifts/${year}/${month}`;
        try {
            this.loading.set(true);
            this.error.set(null);
            return await firstValueFrom(this.http.get<EmployeeMonthShift[]>(url));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to fetch monthly shifts', url);
            this.error.set(errorResponse);
            return [];
        } finally {
            this.loading.set(false);
        }
    }

    public async fetchOptimizedShiftSchedule(reqeust: ShiftScheduleSolveRequest): Promise<EmployeeMonthShift[]> {
        const url = `${this.shiftSchedulingPath}/optimized-shift`;
        try {
            this.loading.set(true);
            this.error.set(null);
            return await firstValueFrom(this.http.post<EmployeeMonthShift[]>(url, reqeust));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to retrieve optimized schedule', url);
            this.error.set(errorResponse);
            return [];
        } finally {
            this.loading.set(false);
        }
    }

    public async putShiftScheduleForMonth(year: number, month: number, request: EmployeeMonthShift[]): Promise<void> {
        const url = `${this.shiftSchedulingPath}/shifts/${year}/${month}`;
        try {
            this.loading.set(true);
            this.error.set(null);

            await firstValueFrom(this.http.put<void>(url, request));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to save schedule', url);
            this.error.set(errorResponse);
        } finally {
            this.loading.set(false);
        }
    }

    public async fetchSchedulingRoles(): Promise<SchedulingRole[]> {
        const url = `${this.shiftSchedulingPath}/roles`;
        try {
            this.loading.set(true);
            this.error.set(null);
            return await firstValueFrom(this.http.get<SchedulingRole[]>(url));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to fetch scheduling roles', url);
            this.error.set(errorResponse);
            return [];
        } finally {
            this.loading.set(false);
        }
    }

    public async addSchedulingRole(request: SchedulingRole): Promise<void> {
        const url = `${this.shiftSchedulingPath}/roles`;
        try {
            this.loading.set(true);
            this.error.set(null);
            await firstValueFrom(this.http.put<void>(url, request));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to add new scheduling role', url);
            this.error.set(errorResponse);
        } finally {
            this.loading.set(false);
        }
    }

    public async deleteSchedulingRole(id: number): Promise<void> {
        const url = `${this.shiftSchedulingPath}/roles/${id}`;
        try {
            this.loading.set(true);
            this.error.set(null);
            await firstValueFrom(this.http.delete<void>(url));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to delete scheduling role', url);
            this.error.set(errorResponse);
        } finally {
            this.loading.set(false);
        }
    }

    public async updateSchedulingRole(id: number, request: SchedulingRole): Promise<void> {
        const url = `${this.shiftSchedulingPath}/roles/${id}`;
        try {
            this.loading.set(true);
            this.error.set(null);
            await firstValueFrom(this.http.post<void>(url, request));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to update scheduling role', url);
            this.error.set(errorResponse);
        } finally {
            this.loading.set(false);
        }
    }

    public async fetchAllShiftTemplates(): Promise<ShiftScheduleTemplate[]> {
        const url = `${this.shiftSchedulingPath}/shift-templates`;
        try {
            this.loading.set(true);
            this.error.set(null);
            const response = await firstValueFrom(this.http.get<PackedShiftScheduleTemplate[]>(url));
            return response.map(this.unpackShiftScheduleTemplate);
        } catch (error: unknown) {
            console.error(error);
            const errorResponse = processError(error, 'Failed to fetch shift templates', url);
            this.error.set(errorResponse);
            return [];
        } finally {
            this.loading.set(false);
        }
    }

    public async addShiftTemplate(request: ShiftScheduleTemplate): Promise<void> {
        const url = `${this.shiftSchedulingPath}/shift-templates`;
        try {
            this.loading.set(true);
            this.error.set(null);
            await firstValueFrom(this.http.put<void>(url, this.packShiftScheduleTemplate(request)));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to add new shift template', url);
            this.error.set(errorResponse);
        } finally {
            this.loading.set(false);
        }
    }

    public async deleteShiftTemplate(id: number): Promise<void> {
        const url = `${this.shiftSchedulingPath}/shift-templates/${id}`;
        try {
            this.loading.set(true);
            this.error.set(null);
            await firstValueFrom(this.http.delete<void>(url));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to delete shift template', url);
            this.error.set(errorResponse);
        } finally {
            this.loading.set(false);
        }
    }

    public async fetchShiftSchedulePdf(year: number, month: number): Promise<Blob | null> {
        const url = `${this.shiftSchedulingPath}/shifts/${year}/${month}/pdf`;
        try {
            this.loading.set(true);
            this.error.set(null);
            return await firstValueFrom(this.http.get(url, { responseType: 'blob' }));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to download PDF', url);
            this.error.set(errorResponse);
            return null;
        } finally {
            this.loading.set(false);
        }
    }

    public async updateShiftTemplate(id: number, request: ShiftScheduleTemplate): Promise<void> {
        const url = `${this.shiftSchedulingPath}/shift-templates/${id}`;
        try {
            this.loading.set(true);
            this.error.set(null);
            await firstValueFrom(this.http.post<void>(url, this.packShiftScheduleTemplate(request)));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to update shift template', url);
            this.error.set(errorResponse);
        } finally {
            this.loading.set(false);
        }
    }

    public unpackShiftScheduleTemplate(packed: PackedShiftScheduleTemplate): ShiftScheduleTemplate {
        return {
            id: packed.id,
            name: packed.name,
            content: JSON.parse(packed.content),
        };
    }
    public packShiftScheduleTemplate(unpacked: ShiftScheduleTemplate): PackedShiftScheduleTemplate {
        return {
            id: unpacked.id,
            name: unpacked.name,
            content: JSON.stringify(unpacked.content),
        };
    }

    public compareEmployeeNames(left: SchedulableEmployee, right: SchedulableEmployee): number {
        if (!left.firstName || !left.lastName || !right.firstName || !right.lastName) {
            return 0;
        }
        if (left.lastName < right.lastName || (left.lastName === right.lastName && left.firstName < right.firstName)) {
            return -1;
        }
        if (left.lastName > right.lastName || left.firstName > right.firstName) {
            return 1;
        }
        return 0;
    }
}
