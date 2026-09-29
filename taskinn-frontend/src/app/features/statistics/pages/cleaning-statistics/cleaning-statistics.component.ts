import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { StatisticsStore } from '../../statistics.store';
import { StatisticsService } from '../../statistics.service';
import { MatIcon } from '@angular/material/icon';

@Component({
    selector: 'tinn-cleaning-statistics',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, BaseChartDirective, MatIcon],
    templateUrl: './cleaning-statistics.component.html',
})
export class CleaningStatisticsComponent {
    private readonly store = inject(StatisticsStore);
    private readonly service = inject(StatisticsService);
    private readonly fb = inject(FormBuilder);

    public readonly stats = this.store.cleaningStats;
    public readonly loading = this.store.loading;

    private readonly storedDates = this.store.cleaningDateRange();
    public readonly filterForm = this.fb.group({
        from: this.storedDates.from,
        to: this.storedDates.to,
    });

    public readonly taskTypes = ['CHECKOUT_CLEAN', 'STAYOVER_CLEAN', 'DEEP_CLEAN'];

    private readonly filterValues = toSignal(this.filterForm.valueChanges, {
        initialValue: this.filterForm.value,
    });

    public readonly canApply = computed(() => {
        const val = this.filterValues();

        const from = val?.from;
        const to = val?.to;

        if (!from && !to) {
            return true;
        }

        if (!from || !to) {
            return false;
        }

        return new Date(from) <= new Date(to);
    });

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

    public readonly pieOptions: ChartConfiguration<'pie'>['options'] = {
        responsive: true,
        maintainAspectRatio: true,
        aspectRatio: 2.5,
        plugins: {
            legend: {
                position: 'top',
            },
        },
    };

    public taskBarData = (taskType: string): ReturnType<typeof computed<ChartData<'bar'>>> =>
        computed<ChartData<'bar'>>(() => {
            const s = this.stats();
            if (!s) {
                return { labels: [], datasets: [] };
            }

            const roomMap = s.avgDurationByTaskAndRoomType[taskType] ?? {};
            const entries = Object.entries(roomMap).sort((a, b) => (b[1] ?? 0) - (a[1] ?? 0));

            return {
                labels: entries.map(([k]) => k),
                datasets: [
                    {
                        label: 'Minutes',
                        data: entries.map(([, v]) => v ?? 0),
                        borderRadius: 6,
                    },
                ],
            };
        });

    public readonly seasonalityBarData = computed<ChartData<'bar'>>(() => {
        const s = this.stats();
        if (!s) {
            return { labels: [], datasets: [] };
        }

        const monthKeys = Object.keys(s.avgHoursByMonth).sort();
        return {
            labels: monthKeys.map((k) => this.toMonthLabel(k)),
            datasets: [
                {
                    label: 'Avg hours',
                    data: monthKeys.map((k) => s.avgHoursByMonth[k] ?? 0),
                },
            ],
        };
    });

    public readonly dateRangeError = computed(() => {
        const val = this.filterValues();

        const from = val?.from;
        const to = val?.to;

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

    private toMonthLabel(month: string): string {
        const monthIndex = parseInt(month, 10) - 1;
        if (Number.isNaN(monthIndex) || monthIndex < 0 || monthIndex > 11) {
            return month;
        }
        return new Intl.DateTimeFormat('en', { month: 'short' }).format(new Date(2020, monthIndex, 1));
    }

    public readonly blockedPieData = computed<ChartData<'pie'>>(() => {
        const s = this.stats();
        if (!s) {
            return { labels: [], datasets: [] };
        }

        const blocked = s.blockedTasksPercentage ?? 0;
        return {
            labels: ['Skipped', 'Completed'],
            datasets: [{ data: [blocked, 100 - blocked] }],
        };
    });

    public formatTaskType(taskType: string): string {
        return taskType
            .toLowerCase()
            .split('_')
            .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
            .join(' ');
    }

    public async applyFilters(): Promise<void> {
        if (!this.canApply()) {
            return;
        }

        const { from, to } = this.filterForm.value;
        this.store.setCleaningDateRange(from ?? null, to ?? null);
        await this.service.fetchCleaningStatistics(from && to ? { from, to } : null);
    }
}
