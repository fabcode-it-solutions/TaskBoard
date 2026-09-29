import { Routes } from '@angular/router';

export const BOOKING_ROUTES: Routes = [
    {
        path: 'date',
        loadComponent: () =>
            import('./1-booking-date-page/booking-date-page.component').then((m) => m.BookingDatePageComponent),
    },
    {
        path: 'guests',
        loadComponent: () =>
            import('./2-booking-guests-page/booking-guests-page.component').then((m) => m.BookingGuestsPageComponent),
    },
    {
        path: 'room-options',
        loadComponent: () =>
            import('./3-booking-room-options-page/booking-room-options-page.component').then(
                (m) => m.BookingRoomOptionsPageComponent,
            ),
    },
    {
        path: 'checkout',
        loadComponent: () =>
            import('./4-booking-checkout-page/booking-checkout-page.component').then(
                (m) => m.BookingCheckoutPageComponent,
            ),
    },
    {
        path: 'success',
        loadComponent: () =>
            import('./5-booking-success-page/booking-success-page.component').then(
                (m) => m.BookingSuccessPageComponent,
            ),
    },
    {
        path: 'all',
        loadComponent: () => import('./booking-list/booking-list.component').then((m) => m.BookingListComponent),
    },
    {
        path: '**',
        redirectTo: 'date',
    },
];
