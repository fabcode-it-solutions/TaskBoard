import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

interface ActionCard {
    title: string;
    icon: string;
    route: string;
}

@Component({
    selector: 'tinn-manager-dashboard',
    imports: [PageHeaderComponent, RouterLink, MatIcon],
    templateUrl: './manager-dashboard.component.html',
})
export class ManagerDashboardComponent {
    public actionCards: ActionCard[] = [
        {
            title: 'Create Employee/Admin Account',
            icon: 'person_add',
            route: '/admin/create-account',
        },
        {
            title: 'Manage Personnel',
            icon: 'groups',
            route: '/employee/all',
        },
        {
            title: 'Hotel Structure Management',
            icon: 'apartment',
            route: '/admin/floor-plan',
        },
        {
            title: 'Create Shift Layout',
            icon: 'calendar_month',
            route: '/admin/create-shift-layout',
        },
        {
            title: 'Manage Shift Scheduling Templates',
            icon: 'calendar_month',
            route: '/admin/manage-shift-templates',
        },
        {
            title: 'Task Cleaning Schedule',
            icon: 'cleaning_services',
            route: '/admin/task-scheduling',
        },
        /*
         *{
         *  title: 'Manage Active Shift',
         *  icon: 'schedule',
         *  route: '/admin/manage-shift',
         *},
         */
        {
            title: 'Manage Room Presets',
            icon: 'category',
            route: '/admin/room-types',
        },
        {
            title: 'Minibar Inventory Management',
            icon: 'inventory_2',
            route: '/admin/minibar-management',
        },
        {
            title: 'Room Management',
            icon: 'meeting_room',
            route: '/rooms',
        },
    ];
}
