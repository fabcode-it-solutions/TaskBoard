import { Injectable, signal, WritableSignal } from '@angular/core';
import { ErrorResponse } from '../../shared/interfaces/error';
import { MaintenanceTicketDto } from './maintenance';

@Injectable({ providedIn: 'root' })
export class MaintenanceStore {
    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);
    public maintenanceTicket: WritableSignal<MaintenanceTicketDto | null> = signal(null);
    public maintenanceTicketList: WritableSignal<MaintenanceTicketDto[] | null> = signal(null);

    public setLoading(loading: boolean): void {
        this.loading.set(loading);
    }

    public setError(error: ErrorResponse | null): void {
        this.error.set(error);
    }

    public setMaintenanceTicket(maintenanceTicket: MaintenanceTicketDto | null): void {
        this.maintenanceTicket.set(maintenanceTicket);
    }

    public setMaintenanceTicketList(maintenanceTicketList: MaintenanceTicketDto[] | null): void {
        this.maintenanceTicketList.set(maintenanceTicketList);
    }
}
