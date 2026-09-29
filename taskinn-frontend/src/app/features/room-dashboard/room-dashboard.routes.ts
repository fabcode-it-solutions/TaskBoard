import { Routes } from '@angular/router';

export const ROOM_DASHBOARD_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () =>
            import('./live-room-dashboard/live-room-dashboard.component').then((m) => m.LiveRoomDashboardComponent),
    },
];
