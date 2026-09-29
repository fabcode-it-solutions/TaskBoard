import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { StatisticsStore } from '../../statistics.store';
import { StatisticsService } from '../../statistics.service';
import { MatIcon } from '@angular/material/icon';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tinn-minibar-statistics',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, FormsModule, BaseChartDirective, MatIcon],
    templateUrl: './minibar-statistics.component.html',
})
export class MinibarStatisticsComponent {
    private readonly store = inject(StatisticsStore);
    private readonly service = inject(StatisticsService);
    private readonly fb = inject(FormBuilder);

    public readonly stats = this.store.minibarStats;
    public readonly allItems = this.store.allMinibarItems;
    public readonly selectedIds = this.store.selectedMinibarItemIds;
    public readonly loading = this.store.loading;

    private readonly storedDates = this.store.minibarDateRange();
    public readonly filterForm = this.fb.group({
        from: this.storedDates.from,
        to: this.storedDates.to,
    });

    public selectedItemToAdd = signal<number | null>(null);

    // Filter available items for dropdown (exclude already selected)
    public readonly availableItems = computed(() => {
        const all = this.allItems();
        const selected = this.selectedIds();
        return all.filter((item) => !selected.includes(item.id));
    });

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

    public async applyFilters(): Promise<void> {
        if (!this.canApply()) {
            return;
        }
        const range = this.getFormRange();
        this.store.setMinibarDateRange(range?.from ?? null, range?.to ?? null);
        await this.service.fetchMinibarStatistics(range, this.selectedIds(), false);
    }

    public async addItem(): Promise<void> {
        const id = this.selectedItemToAdd();
        if (!id) {
            return;
        }

        const range = this.getFormRange();
        // Fetch only specific item and append
        await this.service.fetchMinibarStatistics(range, [id], true);
        this.selectedItemToAdd.set(null);
    }

    public removeItem(id: number): void {
        this.store.removeMinibarItemStat(id);
    }

    public getItemName(id: number): string {
        const item = this.allItems().find((i) => i.id === id);
        return item ? item.name : `Item #${id}`;
    }

    private getFormRange(): { from: string; to: string } | null {
        const { from, to } = this.filterForm.value;
        return from && to ? { from, to } : null;
    }

    // --- Chart Data Helpers ---

    public getEarningsPieData(): ChartData<'pie'> {
        const dataMap = this.stats()?.salesByEarnings ?? {};
        // Sort by value desc
        const entries = Object.entries(dataMap).sort((a, b) => b[1] - a[1]);
        return {
            labels: entries.map(([k]) => k),
            datasets: [
                {
                    data: entries.map(([, v]) => v),
                },
            ],
        };
    }

    public getQuantityPieData(): ChartData<'pie'> {
        const dataMap = this.stats()?.salesByQuantity ?? {};
        const entries = Object.entries(dataMap).sort((a, b) => b[1] - a[1]);
        return {
            labels: entries.map(([k]) => k),
            datasets: [
                {
                    data: entries.map(([, v]) => v),
                },
            ],
        };
    }

    public getDailyLineData(itemId: number): ChartData<'line'> {
        const dailyMap = this.stats()?.dailyItemSales?.[itemId] ?? {};
        const { from, to } = this.filterForm.value;

        let allDates: string[];

        if (from && to) {
            // Generate complete date range from filter
            allDates = this.generateDateRange(from, to);
        } else {
            // No filter applied, use dates from backend
            allDates = Object.keys(dailyMap).sort();
        }

        return {
            labels: allDates,
            datasets: [
                {
                    label: 'Units Sold',
                    data: allDates.map((d) => dailyMap[d] ?? 0),
                    borderColor: '#1f2937', // neutral-800
                    backgroundColor: 'rgba(31, 41, 55, 0.1)',
                    fill: true,
                    tension: 0.3,
                },
            ],
        };
    }

    private generateDateRange(from: string, to: string): string[] {
        const dates: string[] = [];
        const current = new Date(from);
        const end = new Date(to);

        while (current <= end) {
            const year = current.getFullYear();
            const month = String(current.getMonth() + 1).padStart(2, '0');
            const day = String(current.getDate()).padStart(2, '0');
            dates.push(`${year}-${month}-${day}`);
            current.setDate(current.getDate() + 1);
        }

        return dates;
    }

    // --- Chart Options ---

    public readonly pieOptions: ChartOptions<'pie'> = {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        plugins: { legend: { display: false } },
    };

    public readonly lineOptions: ChartOptions<'line'> = {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        scales: {
            y: { beginAtZero: true, ticks: { stepSize: 1 } },
            x: { grid: { display: false } },
        },
        plugins: { legend: { display: false } },
    };
}
