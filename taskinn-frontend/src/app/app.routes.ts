import { Routes } from '@angular/router';
import { adminGuard } from './shared/guards/admin.guard';
import { unauthGuard } from './shared/guards/unauth.guard';
import { authGuard } from './shared/guards/auth.guard';

export const routes: Routes = [
    {
        path: 'auth',
        loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
        canActivate: [unauthGuard],
    },
    {
        path: 'admin',
        loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
        canActivate: [adminGuard],
    },
    {
        path: 'employee',
        loadChildren: () => import('./features/employee/employee.routes').then((m) => m.EMPLOYEE_ROUTES),
        canActivate: [adminGuard],
    },
    {
        path: 'staff',
        loadChildren: () => import('./features/staff/staff.routes').then((m) => m.STAFF_ROUTES),
        canActivate: [authGuard],
    },
    {
        path: 'booking',
        loadChildren: () => import('./features/booking/booking.routes').then((m) => m.BOOKING_ROUTES),
        canActivate: [],
    },
    {
        path: 'checkin',
        loadChildren: () => import('./features/check-in/check-in.routes').then((m) => m.CHECK_IN_ROUTES),
        canActivate: [authGuard],
    },
    {
        path: 'checkout',
        loadChildren: () => import('./features/check-out/check-out.routes').then((m) => m.CHECK_OUT_ROUTES),
        canActivate: [authGuard],
    },
    {
        path: 'maintenance',
        loadChildren: () => import('./features/maintenance/maintenance.routes').then((m) => m.MAINTENANCE_ROUTES),
        canActivate: [authGuard],
    },
    {
        path: 'rooms',
        loadChildren: () => import('./features/room/room.routes').then((m) => m.ROOM_ROUTES),
        canActivate: [adminGuard],
    },
    {
        path: 'room-dashboard',
        loadChildren: () =>
            import('./features/room-dashboard/room-dashboard.routes').then((m) => m.ROOM_DASHBOARD_ROUTES),
        canActivate: [authGuard],
    },
    {
        path: 'mobile/qr-auth',
        loadComponent: () =>
            import('./features/mobile/mobile-qr-auth/mobile-qr-auth.component').then((m) => m.MobileQrAuthComponent),
    },
    {
        path: 'mobile',
        loadChildren: () => import('./features/mobile/mobile.routes').then((m) => m.MOBILE_ROUTES),
        canActivate: [authGuard],
    },
    {
        path: 'statistics',
        loadChildren: () => import('./features/statistics/statistics.routes').then((m) => m.STATISTICS_ROUTES),
        canActivate: [adminGuard],
    },
    {
        path: '',
        redirectTo: '/auth/login',
        pathMatch: 'full',
    },
    {
        path: '**',
        redirectTo: '/auth/login',
    },
];
