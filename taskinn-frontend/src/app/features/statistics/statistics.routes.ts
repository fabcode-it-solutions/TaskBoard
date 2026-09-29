import { Routes } from '@angular/router';

export const STATISTICS_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () =>
            import('./pages/statistics-shell/statistics-shell.component').then((m) => m.StatisticsShellComponent),
        children: [
            {
                path: 'bookings',
                loadComponent: () =>
                    import('./pages/booking-statistics/booking-statistics.component').then(
                        (m) => m.BookingStatisticsComponent,
                    ),
            },
            {
                path: 'cleaning',
                loadComponent: () =>
                    import('./pages/cleaning-statistics/cleaning-statistics.component').then(
                        (m) => m.CleaningStatisticsComponent,
                    ),
            },
            {
                path: 'minibar',
                loadComponent: () =>
                    import('./pages/minibar-statistics/minibar-statistics.component').then(
                        (m) => m.MinibarStatisticsComponent,
                    ),
            },
            {
                path: 'employees',
                loadComponent: () =>
                    import('./pages/employee-statistics/employee-statistics.component').then(
                        (m) => m.EmployeeStatisticsComponent,
                    ),
            },
            {
                path: '',
                pathMatch: 'full',
                redirectTo: 'bookings',
            },
            {
                path: '**',
                redirectTo: 'bookings',
            },
        ],
    },
];
