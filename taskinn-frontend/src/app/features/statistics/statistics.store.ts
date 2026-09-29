import { Injectable, signal, WritableSignal } from '@angular/core';
import { ErrorResponse } from '../../shared/interfaces/error';
import { BookingStatisticsResponse } from './interfaces/booking-statistics.response';
import { CleaningStatisticsResponse } from './interfaces/cleaning-statistics.response';
import { EmployeeStatisticsResponse } from './interfaces/employee-statistics.response';
import { EmployeeResponse } from '../employee/interfaces/employee';
import { MinibarStatisticsResponse, InventoryItemSelectionResponse } from './interfaces/minibar-statistics.response';

@Injectable({ providedIn: 'root' })
export class StatisticsStore {
    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);

    public bookingStats: WritableSignal<BookingStatisticsResponse | null> = signal(null);
    public cleaningStats: WritableSignal<CleaningStatisticsResponse | null> = signal(null);

    public employeeStats: WritableSignal<EmployeeStatisticsResponse[]> = signal([]);
    public allEmployees: WritableSignal<EmployeeResponse[]> = signal([]);
    public selectedEmployeeIds: WritableSignal<number[]> = signal([]);

    public selectedMonth: WritableSignal<string> = signal(this.getCurrentYearMonth());

    public minibarStats: WritableSignal<MinibarStatisticsResponse | null> = signal(null);
    public allMinibarItems: WritableSignal<InventoryItemSelectionResponse[]> = signal([]);
    public selectedMinibarItemIds: WritableSignal<number[]> = signal([]);

    public bookingDateRange: WritableSignal<{ from: string | null; to: string | null }> = signal({
        from: null,
        to: null,
    });
    public cleaningDateRange: WritableSignal<{ from: string | null; to: string | null }> = signal({
        from: null,
        to: null,
    });
    public minibarDateRange: WritableSignal<{ from: string | null; to: string | null }> = signal({
        from: null,
        to: null,
    });
    public employeeDateRange: WritableSignal<{ from: string | null; to: string | null }> = signal({
        from: null,
        to: null,
    });

    public setLoading(v: boolean): void {
        this.loading.set(v);
    }

    public setError(v: ErrorResponse | null): void {
        this.error.set(v);
    }

    public setBookingStats(v: BookingStatisticsResponse | null): void {
        this.bookingStats.set(v);
    }

    public setCleaningStats(v: CleaningStatisticsResponse | null): void {
        this.cleaningStats.set(v);
    }

    public setSelectedMonth(v: string): void {
        this.selectedMonth.set(v);
    }

    public setEmployeeStats(v: EmployeeStatisticsResponse[]): void {
        this.employeeStats.set(v);
    }

    public appendEmployeeStat(v: EmployeeStatisticsResponse): void {
        this.employeeStats.update((current) => [...current, v]);
    }

    public setAllEmployees(v: EmployeeResponse[]): void {
        this.allEmployees.set(v);
    }

    public addSelectedEmployeeId(id: number): void {
        this.selectedEmployeeIds.update((ids) => (ids.includes(id) ? ids : [...ids, id]));
    }

    public removeEmployeeStat(id: number): void {
        this.employeeStats.update((stats) => stats.filter((s) => s.employeeId !== id));
        this.selectedEmployeeIds.update((ids) => ids.filter((i) => i !== id));
    }

    public setMinibarStats(v: MinibarStatisticsResponse | null): void {
        this.minibarStats.set(v);
    }

    public updateMinibarStats(response: MinibarStatisticsResponse, append: boolean): void {
        this.minibarStats.update((current) => {
            const newEarnings = response.salesByEarnings;
            const newQuantities = response.salesByQuantity;

            // Handle Line Charts (dailyItemSales)
            let newDailySales: Record<number, Record<string, number>>;

            if (append && current) {
                /*
                 * Adding a new item
                 * We keep existing line charts and add/overwrite the specific ones requested
                 */
                newDailySales = {
                    ...current.dailyItemSales,
                    ...response.dailyItemSales,
                };
            } else {
                /*
                 * Date filter applied OR Initial Load
                 * We replace the daily records entirely to ensure they match the new date range
                 */
                newDailySales = response.dailyItemSales;
            }

            return {
                salesByEarnings: newEarnings,
                salesByQuantity: newQuantities,
                dailyItemSales: newDailySales,
            };
        });
    }

    public setAllMinibarItems(v: InventoryItemSelectionResponse[]): void {
        this.allMinibarItems.set(v);
    }

    public addSelectedMinibarItemId(id: number): void {
        this.selectedMinibarItemIds.update((ids) => (ids.includes(id) ? ids : [...ids, id]));
    }

    public removeMinibarItemStat(id: number): void {
        this.selectedMinibarItemIds.update((ids) => ids.filter((i) => i !== id));
    }

    public setBookingDateRange(from: string | null, to: string | null): void {
        this.bookingDateRange.set({ from, to });
    }

    public setCleaningDateRange(from: string | null, to: string | null): void {
        this.cleaningDateRange.set({ from, to });
    }

    public setMinibarDateRange(from: string | null, to: string | null): void {
        this.minibarDateRange.set({ from, to });
    }

    public setEmployeeDateRange(from: string | null, to: string | null): void {
        this.employeeDateRange.set({ from, to });
    }

    private getCurrentYearMonth(): string {
        const now = new Date();
        const y = now.getFullYear();
        const m = String(now.getMonth() + 1).padStart(2, '0');
        return `${y}-${m}`;
    }
}
