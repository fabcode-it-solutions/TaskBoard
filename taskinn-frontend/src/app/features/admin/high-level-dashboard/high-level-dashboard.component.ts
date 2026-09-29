import { Component, inject, OnInit, signal, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIcon } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RoomService } from '../../room/room.service';
import { BookingService } from '../../booking/booking.service';
import { MaintenanceService } from '../../maintenance/maintenance.service';
import { FloorOccupancyDto } from '../../room/room';
import { MaintenanceTicketDto } from '../../maintenance/maintenance';
import { StatsCardComponent } from './stats-card/stats-card.component';
import { ToastrService } from 'ngx-toastr';

interface StatsCard {
    title: string;
    icon: string;
    color: 'primary' | 'success' | 'warning' | 'error';
    suffix?: string;
    value: Signal<number | null>;
}

@Component({
    selector: 'tinn-high-level-dashboard',
    imports: [CommonModule, PageHeaderComponent, MatIcon, StatsCardComponent],
    templateUrl: './high-level-dashboard.component.html',
})
export class HighLevelDashboardComponent implements OnInit {
    private readonly roomService = inject(RoomService);
    private readonly bookingService = inject(BookingService);
    private readonly maintenanceService = inject(MaintenanceService);
    private readonly toastrService = inject(ToastrService);

    public occupancyRate = signal<number | null>(null);
    public checkInsToday = signal<number | null>(null);
    public checkOutsToday = signal<number | null>(null);
    public urgentTickets = signal<number | null>(null);
    public floorOccupancy = signal<FloorOccupancyDto[]>([]);
    public openTickets = signal<MaintenanceTicketDto[]>([]);
    public isLoading = signal<boolean>(true);

    public statsCards: StatsCard[] = [
        {
            title: 'Occupancy Rate Today',
            icon: 'business',
            color: 'primary',
            suffix: '%',
            value: this.occupancyRate,
        },
        {
            title: 'Check-ins Today',
            icon: 'login',
            color: 'success',
            value: this.checkInsToday,
        },
        {
            title: 'Check-outs Today',
            icon: 'logout',
            color: 'warning',
            value: this.checkOutsToday,
        },
        {
            title: 'Critical Maintenance Tickets',
            icon: 'priority_high',
            color: 'error',
            value: this.urgentTickets,
        },
    ];

    async ngOnInit(): Promise<void> {
        await this.loadDashboardData();
    }

    private async loadDashboardData(): Promise<void> {
        this.isLoading.set(true);

        try {
            const [occupancyRate, checkIns, checkOuts, urgentCount, floorOccupancy, openTickets] = await Promise.all([
                this.roomService.getTodaysOccupancyRate(),
                this.bookingService.getTodaysCheckInCount(),
                this.bookingService.getTodaysCheckOutCount(),
                this.maintenanceService.countHighPriorityOpenTickets(),
                this.roomService.getFloorOccupancyDistribution(),
                this.maintenanceService.getAllOpenMaintenanceTickets(),
            ]);

            this.occupancyRate.set(occupancyRate);
            this.checkInsToday.set(checkIns);
            this.checkOutsToday.set(checkOuts);
            this.urgentTickets.set(urgentCount);
            this.floorOccupancy.set(floorOccupancy);

            // Sort tickets by priority: URGENT > HIGH > MEDIUM > LOW
            const priorityOrder: Record<string, number> = { URGENT: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
            const sortedTickets = [...openTickets].sort((a, b) => {
                const aPriority = priorityOrder[a.priority?.toUpperCase()] ?? 999;
                const bPriority = priorityOrder[b.priority?.toUpperCase()] ?? 999;
                return aPriority - bPriority;
            });
            this.openTickets.set(sortedTickets);
            // eslint-disable-next-line unused-imports/no-unused-vars
        } catch (error) {
            this.toastrService.error('Failed to load dashboard data. Please try again later.', 'Error');
        } finally {
            this.isLoading.set(false);
        }
    }

    public async refreshDashboard(): Promise<void> {
        await this.loadDashboardData();
    }

    public getStatusColor(priority: string): string {
        switch (priority?.toUpperCase()) {
            case 'URGENT':
                return 'text-error-900';
            case 'HIGH':
                return 'text-error-600';
            case 'MEDIUM':
                return 'text-warning-600';
            case 'LOW':
                return 'text-success-600';
            default:
                return 'text-neutral-600';
        }
    }

    public getStatusBgColor(priority: string): string {
        switch (priority?.toUpperCase()) {
            case 'URGENT':
                return 'bg-error-100';
            case 'HIGH':
                return 'bg-error-50';
            case 'MEDIUM':
                return 'bg-warning-50';
            case 'LOW':
                return 'bg-success-50';
            default:
                return 'bg-neutral-50';
        }
    }
}
