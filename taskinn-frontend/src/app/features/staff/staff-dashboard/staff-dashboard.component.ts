import { Component } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RouterLink } from '@angular/router';

interface ActionCard {
    title: string;
    icon: string;
    route: string;
}

@Component({
    selector: 'tinn-staff-dashboard',
    imports: [MatIcon, PageHeaderComponent, RouterLink],
    templateUrl: './staff-dashboard.component.html',
})
export class StaffDashboardComponent {
    public actionCards: ActionCard[] = [
        {
            title: 'Create Booking',
            icon: 'person_add',
            route: '/booking/date',
        },
        {
            title: 'Manage Bookings',
            icon: 'list_alt',
            route: '/booking/all',
        },
        {
            title: 'Check-in Booking',
            icon: 'login', // or 'assignment_turned_in'
            route: '/checkin',
        },
        {
            title: 'Check-out Booking',
            icon: 'logout',
            route: '/checkout',
        },
    ];
}
