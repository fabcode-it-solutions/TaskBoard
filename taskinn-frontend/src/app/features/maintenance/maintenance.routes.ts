import { Routes } from '@angular/router';

export const MAINTENANCE_ROUTES: Routes = [
    {
        path: 'board',
        loadComponent: () =>
            import('./maintenance-board/maintenance-board.component').then((m) => m.MaintenanceBoardComponent),
    },
    {
        path: '**',
        redirectTo: 'board',
    },
];
