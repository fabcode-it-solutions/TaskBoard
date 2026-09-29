import { Routes } from '@angular/router';

export const CHECK_IN_ROUTES: Routes = [
    {
        path: 'initial',
        loadComponent: () =>
            import('./check-in-initial-page/check-in-initial-page.component').then(
                (m) => m.CheckInInitialPageComponent,
            ),
    },
    {
        path: 'form/:bookingId',
        loadComponent: () =>
            import('./check-in-form-page/check-in-form-page.component').then((m) => m.CheckInFormPageComponent),
    },
    {
        path: '**',
        redirectTo: 'initial',
    },
];
