import { Routes } from '@angular/router';

export const EMPLOYEE_ROUTES: Routes = [
    {
        path: 'all',
        loadComponent: () => import('./staff-list/staff-list.component').then((m) => m.StaffListComponent),
    },
    {
        path: 'edit/:id',
        loadComponent: () => import('./staff-form/staff-form.component').then((m) => m.StaffFormComponent),
    },
    {
        path: '**',
        redirectTo: 'all',
    },
];
