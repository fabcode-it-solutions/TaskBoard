import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
    {
        path: 'create-account',
        loadComponent: () => import('./create-account/create-account.component').then((m) => m.CreateAccountComponent),
    },
    {
        path: 'manager-dashboard',
        loadComponent: () =>
            import('./manager-dashboard/manager-dashboard.component').then((m) => m.ManagerDashboardComponent),
    },
    {
        path: 'floor-plan',
        loadComponent: () =>
            import('../hotel/floor-plan-editor/floor-plan-editor.component').then((m) => m.FloorPlanEditorComponent),
    },
    {
        path: 'overview-dashboard',
        loadComponent: () =>
            import('./high-level-dashboard/high-level-dashboard.component').then((m) => m.HighLevelDashboardComponent),
    },
    {
        path: 'create-shift-layout',
        loadComponent: () => import('./shift-scheduling/shift-scheduling').then((m) => m.ShiftSchedulingComponent),
    },
    {
        path: 'manage-shift-templates',
        loadComponent: () =>
            import('./shift-scheduling-templates/shift-scheduling-templates').then(
                (m) => m.ShiftSchedulingTemplatesComponent,
            ),
    },
    {
        path: 'room-types',
        loadComponent: () =>
            import('./room-type-management/room-type-management.component').then((m) => m.RoomTypeManagementComponent),
    },
    {
        path: 'minibar-management',
        loadComponent: () =>
            import('./minibar-management/minibar-management.component').then((m) => m.MinibarManagementComponent),
    },
    {
        path: 'task-scheduling',
        loadComponent: () => import('./task-scheduling/task-scheduling').then((m) => m.TaskSchedulingComponent),
    },
    {
        path: 'user-parameters',
        loadComponent: () => import('./user-settings/user-settings').then((m) => m.UserSettingsComponent),
    },
];
