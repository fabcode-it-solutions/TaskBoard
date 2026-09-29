import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { StatisticsStore } from '../../statistics.store';
import { StatisticsService } from '../../statistics.service';
import { MatIcon } from '@angular/material/icon';
import { EmployeeStatisticsResponse } from '../../interfaces/employee-statistics.response';

@Component({
    selector: 'tinn-employee-statistics',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, FormsModule, BaseChartDirective, MatIcon],
    templateUrl: './employee-statistics.component.html',
})
export class EmployeeStatisticsComponent {
    private readonly store = inject(StatisticsStore);
    private readonly service = inject(StatisticsService);
    private readonly fb = inject(FormBuilder);

    public readonly stats = this.store.employeeStats;
    public readonly allEmployees = this.store.allEmployees;
    public readonly loading = this.store.loading;

    private readonly storedDates = this.store.employeeDateRange();
    public readonly filterForm = this.fb.group({
        from: this.storedDates.from,
        to: this.storedDates.to,
    });

    // Add Employee Dropdown
    public selectedEmployeeToAdd = signal<number | null>(null);

    public readonly taskTypes = ['CHECKOUT_CLEAN', 'STAYOVER_CLEAN', 'DEEP_CLEAN'];

    // Employees available to be added
    public readonly availableEmployees = computed(() => {
        const all = this.allEmployees();
        const selected = this.store.selectedEmployeeIds();
        return all.filter((emp) => !selected.includes(Number(emp.id)));
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
        // Refresh data for ALL currently selected employees with NEW dates
        const currentIds = this.store.selectedEmployeeIds();
        this.store.setEmployeeDateRange(range?.from ?? null, range?.to ?? null);
        await this.service.fetchEmployeeStatistics(range, currentIds, false);
    }

    public async addEmployee(): Promise<void> {
        const id = this.selectedEmployeeToAdd();
        if (!id) {
            return;
        }

        const range = this.getFormRange();
        // Fetch ONLY this employee and append to store
        await this.service.fetchEmployeeStatistics(range, [id], true);

        // Reset dropdown
        this.selectedEmployeeToAdd.set(null);
    }

    public removeEmployee(id: number): void {
        this.store.removeEmployeeStat(id);
    }

    public getEmployeeName(id: number): string {
        const emp = this.allEmployees().find((e) => Number(e.id) === id);
        return emp ? `${emp.firstName} ${emp.lastName}` : `Employee #${id}`;
    }

    private getFormRange(): { from: string; to: string } | null {
        const { from, to } = this.filterForm.value;
        return from && to ? { from, to } : null;
    }

    // --- Chart Helpers ---

    public readonly barOptions: ChartConfiguration<'bar'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        plugins: { legend: { display: false } },
        scales: {
            x: { grid: { display: false } },
            y: { beginAtZero: true },
        },
    };

    public getTaskBarData(stat: EmployeeStatisticsResponse, taskType: string): ChartData<'bar'> {
        const roomMap = stat.avgDurationByTaskAndRoomType[taskType] ?? {};
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
    }

    public formatTaskType(taskType: string): string {
        return taskType
            .toLowerCase()
            .split('_')
            .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
            .join(' ');
    }
}
