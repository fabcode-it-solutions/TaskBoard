import { inject, Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';
import { DashboardRoomDto } from './interfaces/room-dashboard.interfaces';
import { RoomDashboardStore } from './room-dashboard.store';
import { processError } from '../../shared/util/error.utils';
import { AuthService } from '../auth/auth.service';

@Injectable({
    providedIn: 'root',
})
export class RoomDashboardService {
    private readonly http = inject(HttpClient);
    private readonly store = inject(RoomDashboardStore);
    private readonly ngZone = inject(NgZone);
    private readonly authService = inject(AuthService);

    private readonly dashboardPath = `${environment.apiUrl}/dashboard/rooms`;
    private stompClient: Client | null = null;
    private roomUpdateSubject = new Subject<DashboardRoomDto>();
    public roomUpdates$ = this.roomUpdateSubject.asObservable();

    public async fetchInitialRoomStates(): Promise<DashboardRoomDto[]> {
        this.store.setLoading(true);
        this.store.setError(null);
        try {
            const rooms = await firstValueFrom(this.http.get<DashboardRoomDto[]>(this.dashboardPath));
            this.store.setAllRooms(rooms);
            return rooms;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to fetch room states', this.dashboardPath);
            this.store.setError(errorResponse);
            return [];
        } finally {
            this.store.setLoading(false);
        }
    }

    public connect(): void {
        if (this.stompClient?.active) {
            return;
        }

        const token = this.authService.getToken();
        if (!token) {
            console.error('Cannot connect to WebSocket: No auth token available');
            return;
        }

        this.stompClient = new Client({
            webSocketFactory: (): WebSocket => new SockJS(`${environment.wsUrl}/ws`),
            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },
            reconnectDelay: 5000,
            onConnect: (): void => {
                this.stompClient?.subscribe('/topic/dashboard-updates', (message: IMessage) => {
                    try {
                        const updatedRoom: DashboardRoomDto = JSON.parse(message.body);
                        this.ngZone.run(() => {
                            this.roomUpdateSubject.next(updatedRoom);
                        });
                    } catch (error) {
                        console.error('Error parsing room update message:', error);
                    }
                });
            },
            onStompError: (frame): void => {
                console.error('Broker reported error: ' + frame.headers['message']);
                console.error('Additional details: ' + frame.body);
            },
        });

        this.stompClient.activate();
    }

    public disconnect(): void {
        this.stompClient?.deactivate();
        this.stompClient = null;
    }
}
