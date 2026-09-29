import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { processError } from '../../shared/util/error.utils';
import { StatisticsStore } from './statistics.store';
import { BookingStatisticsResponse } from './interfaces/booking-statistics.response';
import { CleaningStatisticsResponse } from './interfaces/cleaning-statistics.response';
import { EmployeeStatisticsResponse } from './interfaces/employee-statistics.response';
import { EmployeeResponse } from '../employee/interfaces/employee';
import { MinibarStatisticsResponse, InventoryItemSelectionResponse } from './interfaces/minibar-statistics.response';

@Injectable({ providedIn: 'root' })
export class StatisticsService {
    private readonly http = inject(HttpClient);
    private readonly store = inject(StatisticsStore);

    private readonly statisticsPath = `${environment.apiUrl}/statistics`;
    private readonly minibarPath = `${environment.apiUrl}/minibar`;
    private readonly employeePath = `${environment.apiUrl}/employees`;

    public async fetchBookingStatistics(
        month: string,
        range: { from: string; to: string } | null,
    ): Promise<BookingStatisticsResponse | null> {
        try {
            this.store.setLoading(true);
            this.store.setError(null);

            let params = new HttpParams();

            if (month) {
                params = params.set('month', month);
            }

            if (range) {
                params = params.set('periodFrom', range.from).set('periodTo', range.to);
            }

            const url = `${this.statisticsPath}/bookings`;
            const response = await firstValueFrom(this.http.get<BookingStatisticsResponse>(url, { params }));

            this.store.setBookingStats(response);
            return response;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to fetch booking statistics',
                `${this.statisticsPath}/bookings`,
            );
            this.store.setError(errorResponse);
            this.store.setBookingStats(null);
            return null;
        } finally {
            this.store.setLoading(false);
        }
    }

    public async fetchCleaningStatistics(
        range: { from: string; to: string } | null,
    ): Promise<CleaningStatisticsResponse | null> {
        try {
            this.store.setLoading(true);
            this.store.setError(null);

            let params = new HttpParams();
            if (range) {
                params = params.set('periodFrom', range.from).set('periodTo', range.to);
            }

            const url = `${this.statisticsPath}/cleaning`;
            const response = await firstValueFrom(this.http.get<CleaningStatisticsResponse>(url, { params }));

            this.store.setCleaningStats(response);
            return response;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to fetch cleaning statistics',
                `${this.statisticsPath}/cleaning`,
            );
            this.store.setError(errorResponse);
            this.store.setCleaningStats(null);
            return null;
        } finally {
            this.store.setLoading(false);
        }
    }

    public async fetchAllMinibarItems(): Promise<void> {
        try {
            this.store.setLoading(true);
            const url = `${this.minibarPath}/items/selection`;
            const response = await firstValueFrom(this.http.get<InventoryItemSelectionResponse[]>(url));
            this.store.setAllMinibarItems(response);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to load minibar items', this.minibarPath);
            this.store.setError(errorResponse);
        } finally {
            this.store.setLoading(false);
        }
    }

    public async fetchMinibarStatistics(
        range: { from: string; to: string } | null,
        specificItemIds: number[] | null = null,
        append: boolean = false,
    ): Promise<void> {
        try {
            this.store.setLoading(true);
            this.store.setError(null);

            let params = new HttpParams();

            if (range) {
                params = params.set('periodFrom', range.from).set('periodTo', range.to);
            }

            let idsToFetch: number[] = [];

            if (append && specificItemIds) {
                // Adding a specific item from dropdown
                idsToFetch = specificItemIds;
            } else if (!append) {
                /*
                 * Changing Dates (Apply Filter) or Initial Load -> refresh data for all currently visible items.
                 * If specificItemIds is passed use those, otherwise use store state.
                 */
                idsToFetch = specificItemIds ?? this.store.selectedMinibarItemIds();
            }

            if (idsToFetch.length > 0) {
                idsToFetch.forEach((id) => {
                    params = params.append('itemIds', id);
                });
            }

            const url = `${this.statisticsPath}/minibar`;
            const response = await firstValueFrom(this.http.get<MinibarStatisticsResponse>(url, { params }));

            this.store.updateMinibarStats(response, append);

            if (append && specificItemIds) {
                specificItemIds.forEach((id) => this.store.addSelectedMinibarItemId(id));
            } else if (!append) {
                const returnedIds = Object.keys(response.dailyItemSales).map(Number);
                this.store.selectedMinibarItemIds.set(returnedIds);
            }
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to fetch minibar statistics',
                `${this.statisticsPath}/minibar`,
            );
            this.store.setError(errorResponse);
        } finally {
            this.store.setLoading(false);
        }
    }

    public async fetchAllEmployees(): Promise<void> {
        try {
            this.store.setLoading(true);
            this.store.setError(null);

            const response = await firstValueFrom(this.http.get<EmployeeResponse[]>(this.employeePath));
            this.store.setAllEmployees(response);
            this.store.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to load staff members', `${this.employeePath}`);
            this.store.setError(errorResponse);
            this.store.setLoading(false);
        }
    }

    public async fetchEmployeeStatistics(
        range: { from: string; to: string } | null,
        employeeIds: number[] | null = null,
        append: boolean = false,
    ): Promise<void> {
        try {
            this.store.setLoading(true);
            this.store.setError(null);

            let params = new HttpParams();
            if (range) {
                params = params.set('periodFrom', range.from).set('periodTo', range.to);
            }

            if (employeeIds && employeeIds.length > 0) {
                employeeIds.forEach((id) => {
                    params = params.append('employeeIds', id);
                });
            }

            const url = `${this.statisticsPath}/employees`;
            const response = await firstValueFrom(this.http.get<EmployeeStatisticsResponse[]>(url, { params }));

            if (append) {
                response.forEach((stat) => {
                    if (!this.store.selectedEmployeeIds().includes(stat.employeeId)) {
                        this.store.addSelectedEmployeeId(stat.employeeId);
                        this.store.appendEmployeeStat(stat);
                    }
                });
            } else {
                this.store.setEmployeeStats(response);
                if (!employeeIds) {
                    const returnedIds = response.map((r) => r.employeeId);
                    this.store.selectedEmployeeIds.set(returnedIds);
                }
            }
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to fetch employee statistics',
                `${this.statisticsPath}/employees`,
            );
            this.store.setError(errorResponse);
            this.store.setEmployeeStats([]);
        } finally {
            this.store.setLoading(false);
        }
    }

    private toIsoDate(d: Date): string {
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${day}`;
    }
}
