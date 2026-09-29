import { Routes } from '@angular/router';

export const MOBILE_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () =>
            import('./mobile-landing-page/mobile-landing-page.component').then((m) => m.MobileLandingPageComponent),
    },
    {
        path: 'report',
        loadComponent: () =>
            import('./mobile-report-page/mobile-report-page.component').then((m) => m.MobileReportPageComponent),
    },
    {
        path: 'map',
        loadComponent: () => import('./mobile-map-page/mobile-map-page').then((m) => m.MobileMapPageComponent),
    },
    {
        path: 'tasks',
        loadComponent: () =>
            import('./mobile-task-list-page/mobile-task-list-page').then((m) => m.MobileTaskListPageComponent),
    },
    {
        path: '**',
        redirectTo: '',
    },
];
