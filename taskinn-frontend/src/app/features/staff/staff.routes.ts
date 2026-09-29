import { Routes } from '@angular/router';
import { notAdminGuard } from '../../shared/guards/not-admin.guard';

export const STAFF_ROUTES: Routes = [
    {
        path: 'dashboard',
        loadComponent: () =>
            import('./staff-dashboard/staff-dashboard.component').then((m) => m.StaffDashboardComponent),
    },
    {
        path: 'my-schedule',
        canActivate: [notAdminGuard],
        loadComponent: () => import('./my-work/my-work-page.component').then((m) => m.MyWorkPageComponent),
    },
    {
        path: 'task-schedule',
        canActivate: [notAdminGuard],
        loadComponent: () =>
            import('../employee/my-task-schedule/my-task-schedule').then((m) => m.EmployeeTaskScheduleComponent),
    },
    {
        path: '**',
        redirectTo: 'dashboard',
    },
];
