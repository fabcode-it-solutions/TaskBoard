import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
    {
        path: 'login',
        loadComponent: () => import('./login-component/login-component').then((m) => m.LoginComponent),
    },
    {
        path: 'forgot-password',
        loadComponent: () =>
            import('./forgot-password/forgot-password.component').then((m) => m.ForgotPasswordComponent),
    },
    {
        path: 'reset-password',
        loadComponent: () => import('./reset-password/reset-password.component').then((m) => m.ResetPasswordComponent),
    },
    {
        path: '**',
        redirectTo: 'login',
    },
];
