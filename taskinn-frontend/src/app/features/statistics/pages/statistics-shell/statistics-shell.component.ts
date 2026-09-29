import { Component, inject, OnInit, effect, signal } from '@angular/core';
import { StatisticsStore } from '../../statistics.store';
import { StatisticsService } from '../../statistics.service';
import { RouterOutlet, Router, ActivatedRoute } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { NgClass } from '@angular/common';

@Component({
    selector: 'tinn-statistics-shell',
    imports: [MatIcon, PageHeaderComponent, NgClass, RouterOutlet],
    templateUrl: './statistics-shell.component.html',
    standalone: true,
    providers: [provideCharts(withDefaultRegisterables())],
})
export class StatisticsShellComponent implements OnInit {
    private readonly store = inject(StatisticsStore);
    private readonly service = inject(StatisticsService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);

    public readonly loading = this.store.loading;
    public readonly error = this.store.error;

    public activeTab = signal<'bookings' | 'minibar' | 'cleaning' | 'employees'>('bookings');
    public criticalError: string | null = null;

    constructor() {
        effect(() => {
            const tab = this.activeTab();
            switch (tab) {
                case 'cleaning':
                    if (!this.store.cleaningStats()) {
                        this.service.fetchCleaningStatistics(null);
                    }
                    break;
                case 'bookings':
                    if (!this.store.bookingStats()) {
                        this.service.fetchBookingStatistics(this.store.selectedMonth(), null);
                    }
                    break;
                case 'employees':
                    if (this.store.allEmployees().length === 0) {
                        this.service.fetchAllEmployees();
                    }
                    if (this.store.employeeStats().length === 0) {
                        this.service.fetchEmployeeStatistics(null, null, false);
                    }
                    break;
                case 'minibar':
                    if (this.store.allMinibarItems().length === 0) {
                        this.service.fetchAllMinibarItems();
                    }
                    if (!this.store.minibarStats()) {
                        this.service.fetchMinibarStatistics(null, null, false);
                    }
                    break;
                default:
                    this.service.fetchBookingStatistics(this.store.selectedMonth(), null);
                    break;
            }
        });

        effect(() => {
            const err = this.error();
            if (err) {
                this.criticalError = err.message ?? 'Failed to load statistics.';
            }
        });
    }

    ngOnInit(): void {
        const childPath = this.route.snapshot.firstChild?.routeConfig?.path;
        if (
            childPath === 'bookings' ||
            childPath === 'minibar' ||
            childPath === 'cleaning' ||
            childPath === 'employees'
        ) {
            this.activeTab.set(childPath as 'bookings' | 'minibar' | 'cleaning' | 'employees');
        } else {
            this.activeTab.set('bookings');
            this.router.navigate(['bookings'], { relativeTo: this.route });
        }
    }

    public setActiveTab(tab: 'bookings' | 'minibar' | 'cleaning' | 'employees'): void {
        this.activeTab.set(tab);
        this.router.navigate([tab], { relativeTo: this.route });
    }
}
