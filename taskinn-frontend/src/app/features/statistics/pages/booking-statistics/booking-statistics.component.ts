import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormsModule } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { StatisticsStore } from '../../statistics.store';
import { StatisticsService } from '../../statistics.service';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIcon } from '@angular/material/icon';

@Component({
    selector: 'tinn-booking-statistics',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, FormsModule, BaseChartDirective, MatIcon],
    templateUrl: './booking-statistics.component.html',
})
export class BookingStatisticsComponent {
    private readonly store = inject(StatisticsStore);
    private readonly service = inject(StatisticsService);
    private readonly fb = inject(FormBuilder);

    public readonly stats = this.store.bookingStats;
    public readonly loading = this.store.loading;

    private readonly storedDates = this.store.bookingDateRange();
    public readonly filterForm = this.fb.group({
        periodFrom: this.storedDates.from,
        periodTo: this.storedDates.to,
        month: this.store.selectedMonth(), // "YYYY-MM"
    });

    private readonly filterValues = toSignal(this.filterForm.valueChanges, {
        initialValue: this.filterForm.value,
    });

    public readonly canApply = computed(() => {
        const val = this.filterValues();

        const from = val?.periodFrom;
        const to = val?.periodTo;

        if (!from && !to) {
            return true;
        }

        if (!from || !to) {
            return false;
        }

        return new Date(from) <= new Date(to);
    });

    public readonly dateRangeError = computed(() => {
        const val = this.filterValues();

        const from = val?.periodFrom;
        const to = val?.periodTo;

        if (!from && to) {
            return 'Please select a "From" date';
        }

        if (from && !to) {
            return 'Please select a "To" date';
        }

        if (from && to && new Date(from) > new Date(to)) {
            return '"From" date must be before or equal to "To" date';
        }

        return null;
    });

    public readonly showCalendar = signal(false);
    public readonly calendarYear = signal(new Date().getFullYear());

    public readonly calendarYears = computed(() => {
        const years: number[] = [];
        for (let y = 2010; y <= new Date().getFullYear() + 100; y++) {
            years.push(y);
        }
        return years;
    });

    public readonly lineOptions: ChartConfiguration<'line'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        plugins: {
            legend: { display: true },
            tooltip: { mode: 'index', intersect: false },
        },
        scales: {
            x: {
                ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 10 },
                grid: { display: false },
            },
            y: { beginAtZero: true },
        },
    };

    public readonly barOptions: ChartConfiguration<'bar'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        plugins: {
            legend: { display: false },
            tooltip: { mode: 'index', intersect: false },
        },
        scales: {
            x: { grid: { display: false } },
            y: { beginAtZero: true },
        },
    };

    public readonly allHistoryLineData = computed<ChartData<'line'>>(() => {
        const s = this.stats();
        if (!s) {
            return { labels: [], datasets: [] };
        }

        const { labels, values } = this.toSortedSeries(s.allBookingsByDay);
        return {
            labels,
            datasets: [
                {
                    label: 'All bookings (by check-in day)',
                    data: values,
                    tension: 0.25,
                    pointRadius: 0,
                },
            ],
        };
    });

    public readonly monthLineData = computed<ChartData<'line'>>(() => {
        const s = this.stats();
        if (!s) {
            return { labels: [], datasets: [] };
        }

        const { labels, values } = this.toSortedSeries(s.monthBookingsByDay);
        return {
            labels,
            datasets: [
                {
                    label: `Selected month bookings`,
                    data: values,
                    tension: 0.25,
                    pointRadius: 0,
                },
            ],
        };
    });

    public readonly bookingsByMonthBarData = computed<ChartData<'bar'>>(() => {
        const s = this.stats();
        if (!s) {
            return { labels: [], datasets: [] };
        }

        const monthKeys = Object.keys(s.bookingsByMonth).sort();
        const labels = monthKeys.map((k) => this.toMonthLabel(k));
        const values = monthKeys.map((k) => s.bookingsByMonth[k] ?? 0);

        return {
            labels,
            datasets: [{ label: 'Bookings', data: values }],
        };
    });

    public readonly bookingsByRoomTypeBarData = computed<ChartData<'bar'>>(() => {
        const s = this.stats();
        if (!s) {
            return { labels: [], datasets: [] };
        }

        const entries = Object.entries(s.bookingsByRoomType).sort((a, b) => (b[1] ?? 0) - (a[1] ?? 0));
        return {
            labels: entries.map(([k]) => k),
            datasets: [{ label: 'Bookings', data: entries.map(([, v]) => v ?? 0) }],
        };
    });

    public toggleCalendar(): void {
        this.showCalendar.update((v) => !v);
    }

    private readonly monthData = [
        { short: 'Jan', index: 0 },
        { short: 'Feb', index: 1 },
        { short: 'Mar', index: 2 },
        { short: 'Apr', index: 3 },
        { short: 'May', index: 4 },
        { short: 'Jun', index: 5 },
        { short: 'Jul', index: 6 },
        { short: 'Aug', index: 7 },
        { short: 'Sep', index: 8 },
        { short: 'Oct', index: 9 },
        { short: 'Nov', index: 10 },
        { short: 'Dec', index: 11 },
    ];

    public getMonths(): typeof this.monthData {
        return this.monthData;
    }

    public isMonthSelected(monthIndex: number): boolean {
        const currentValue = this.filterForm.value.month as string;
        if (!currentValue) {
            return false;
        }
        const [year, month] = currentValue.split('-');
        return parseInt(month, 10) === monthIndex + 1 && parseInt(year, 10) === this.calendarYear();
    }

    public selectMonth(year: number, month: number): void {
        const m = String(month + 1).padStart(2, '0');
        this.filterForm.controls.month.setValue(`${year}-${m}`, { emitEvent: false });
        this.showCalendar.set(false);
        void this.applyFilters();
    }

    public previousYear(): void {
        this.calendarYear.update((y) => y - 1);
    }

    public nextYear(): void {
        this.calendarYear.update((y) => y + 1);
    }

    public async applyFilters(): Promise<void> {
        if (!this.canApply()) {
            return;
        }
        const month = (this.filterForm.get('month')?.value ?? this.store.selectedMonth()) as string;
        const periodFrom = this.filterForm.get('periodFrom')?.value ?? null;
        const periodTo = this.filterForm.get('periodTo')?.value ?? null;

        this.store.setSelectedMonth(month);
        this.store.setBookingDateRange(periodFrom, periodTo);

        const range = periodFrom && periodTo ? { from: periodFrom, to: periodTo } : null;
        await this.service.fetchBookingStatistics(month, range);
    }

    private toSortedSeries(map: Record<string, number>): { labels: string[]; values: number[] } {
        const labels = Object.keys(map).sort();
        const values = labels.map((k) => map[k] ?? 0);
        return { labels, values };
    }

    private toMonthLabel(month: string): string {
        const monthIndex = parseInt(month, 10) - 1;
        if (Number.isNaN(monthIndex) || monthIndex < 0 || monthIndex > 11) {
            return month;
        }
        return new Intl.DateTimeFormat('en', { month: 'short' }).format(new Date(2020, monthIndex, 1));
    }
}
