import { Routes } from '@angular/router';

export const ROOM_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () => import('./room-list/room-list.component').then((m) => m.RoomListComponent),
    },
    {
        path: '**',
        redirectTo: '',
    },
];
