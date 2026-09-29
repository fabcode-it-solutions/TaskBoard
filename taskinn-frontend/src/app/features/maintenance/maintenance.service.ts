import { inject, Injectable, NgZone, WritableSignal } from '@angular/core';
import { MaintenanceStore } from './maintenance.store';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { MaintenanceTicketCreationUpdateDto, MaintenanceTicketDto } from './maintenance';
import { processError } from '../../shared/util/error.utils';
import { ErrorResponse } from '../../shared/interfaces/error';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';

@Injectable({ providedIn: 'root' })
export class MaintenanceService {
    private readonly maintenanceStore = inject(MaintenanceStore);
    private readonly http = inject(HttpClient);
    private readonly ngZone = inject(NgZone);
    private readonly authService = inject(AuthService);

    private readonly maintenancePath = `${environment.apiUrl}/maintenance`;
    private stompClient: Client | null = null;
    private ticketUpdateSubject = new Subject<MaintenanceTicketDto[]>();
    public ticketUpdates$ = this.ticketUpdateSubject.asObservable();

    public async createMaintenanceTicket(ticketData: MaintenanceTicketCreationUpdateDto): Promise<void> {
        try {
            this.maintenanceStore.setLoading(true);
            this.maintenanceStore.setError(null);

            const newTicket = await firstValueFrom(
                this.http.post<MaintenanceTicketDto>(`${this.maintenancePath}`, ticketData),
            );
            this.maintenanceStore.setMaintenanceTicket(newTicket);
            this.maintenanceStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to create maintenance ticket', `${this.maintenancePath}`);
            this.maintenanceStore.setError(errorResponse);
            this.maintenanceStore.setLoading(false);
        }
    }

    public async getAllMaintenanceTickets(): Promise<void> {
        try {
            this.maintenanceStore.setLoading(true);
            this.maintenanceStore.setError(null);

            const maintenanceTickets = await firstValueFrom(
                this.http.get<Array<MaintenanceTicketDto>>(`${this.maintenancePath}`),
            );
            this.maintenanceStore.setMaintenanceTicketList(maintenanceTickets);
            this.maintenanceStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to load maintenance tickets', `${this.maintenancePath}`);
            this.maintenanceStore.setError(errorResponse);
            this.maintenanceStore.setLoading(false);
        }
    }

    public async getMaintenanceTicketById(ticketId: number): Promise<void> {
        try {
            this.maintenanceStore.setLoading(true);
            this.maintenanceStore.setError(null);

            const maintenanceTicket = await firstValueFrom(
                this.http.get<MaintenanceTicketDto>(`${this.maintenancePath}/${ticketId}`),
            );
            this.maintenanceStore.setMaintenanceTicket(maintenanceTicket);
            this.maintenanceStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to load maintenance ticket with ID ${ticketId}`,
                `${this.maintenancePath}/${ticketId}`,
            );
            this.maintenanceStore.setError(errorResponse);
            this.maintenanceStore.setLoading(false);
        }
    }

    public async updateMaintenanceTicket(
        ticketId: number,
        ticketData: MaintenanceTicketCreationUpdateDto,
    ): Promise<void> {
        try {
            this.maintenanceStore.setLoading(true);
            this.maintenanceStore.setError(null);

            const updatedTicket = await firstValueFrom(
                this.http.put<MaintenanceTicketDto>(`${this.maintenancePath}/${ticketId}`, ticketData),
            );
            this.maintenanceStore.setMaintenanceTicket(updatedTicket);
            this.maintenanceStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to update maintenance ticket with ID ${ticketId}`,
                `${this.maintenancePath}/${ticketId}`,
            );
            this.maintenanceStore.setError(errorResponse);
            this.maintenanceStore.setLoading(false);
        }
    }

    public async deleteMaintenanceTicket(ticketId: number): Promise<void> {
        try {
            this.maintenanceStore.setLoading(true);
            this.maintenanceStore.setError(null);

            await firstValueFrom(this.http.delete<void>(`${this.maintenancePath}/${ticketId}`));
            this.maintenanceStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to delete maintenance ticket with ID ${ticketId}`,
                `${this.maintenancePath}/${ticketId}`,
            );
            this.maintenanceStore.setError(errorResponse);
            this.maintenanceStore.setLoading(false);
        }
    }

    /**
     * Fetches all open maintenance tickets
     * @returns List of open maintenance tickets
     */
    public async getAllOpenMaintenanceTickets(): Promise<MaintenanceTicketDto[]> {
        try {
            this.maintenanceStore.setLoading(true);
            this.maintenanceStore.setError(null);

            const openTickets = await firstValueFrom(
                this.http.get<MaintenanceTicketDto[]>(`${this.maintenancePath}/open`),
            );
            return openTickets;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to fetch open maintenance tickets',
                `${this.maintenancePath}/open`,
            );
            this.maintenanceStore.setError(errorResponse);
            return [];
        } finally {
            this.maintenanceStore.setLoading(false);
        }
    }

    /**
     * Fetches the count of high priority open tickets needing attention
     * @returns Count of high priority open tickets
     */
    public async countHighPriorityOpenTickets(): Promise<number | null> {
        try {
            this.maintenanceStore.setLoading(true);
            this.maintenanceStore.setError(null);

            const count = await firstValueFrom(
                this.http.get<number>(`${this.maintenancePath}/open/high-priority/count`),
            );
            return count;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to fetch high priority open tickets count',
                `${this.maintenancePath}/open/high-priority/count`,
            );
            this.maintenanceStore.setError(errorResponse);
            return null;
        } finally {
            this.maintenanceStore.setLoading(false);
        }
    }

    public get loading(): WritableSignal<boolean> {
        return this.maintenanceStore.loading;
    }

    public get error(): WritableSignal<ErrorResponse | null> {
        return this.maintenanceStore.error;
    }

    public get maintenanceTicket(): WritableSignal<MaintenanceTicketDto | null> {
        return this.maintenanceStore.maintenanceTicket;
    }

    public get maintenanceTicketList(): WritableSignal<MaintenanceTicketDto[] | null> {
        return this.maintenanceStore.maintenanceTicketList;
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
                this.stompClient?.subscribe('/topic/maintenance-updates', (message: IMessage) => {
                    try {
                        const updatedTickets: MaintenanceTicketDto[] = JSON.parse(message.body);
                        this.ngZone.run(() => {
                            this.ticketUpdateSubject.next(updatedTickets);
                        });
                    } catch (error) {
                        console.error('Error parsing maintenance update message:', error);
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
