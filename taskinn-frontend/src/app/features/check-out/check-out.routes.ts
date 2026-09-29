import { Routes } from '@angular/router';

export const CHECK_OUT_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () => import('./check-out-page/check-out-page.component').then((m) => m.CheckOutPageComponent),
    },
    {
        path: '**',
        redirectTo: '',
    },
];
