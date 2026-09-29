import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { processError } from '../../shared/util/error.utils';
import { ErrorResponse } from '../../shared/interfaces/error';
import { InventoryItemDto, MinibarConsumptionItem } from './minibar';

@Injectable({ providedIn: 'root' })
export class MinibarService {
    private readonly http = inject(HttpClient);
    private readonly minibarPath = `${environment.apiUrl}/minibar`;

    public loading = signal(false);
    public error = signal<ErrorResponse | null>(null);
    public inventoryItems = signal<InventoryItemDto[]>([]);

    public async getInventoryItems(): Promise<InventoryItemDto[]> {
        try {
            this.loading.set(true);
            this.error.set(null);

            const items = await firstValueFrom(this.http.get<InventoryItemDto[]>(`${this.minibarPath}/items`));

            this.inventoryItems.set(items);
            return items;
        } catch (err: unknown) {
            const errorResponse = processError(err, 'Failed to fetch inventory items', `${this.minibarPath}/items`);
            this.error.set(errorResponse);
            return [];
        } finally {
            this.loading.set(false);
        }
    }

    public async recordConsumption(roomId: number, items: MinibarConsumptionItem[]): Promise<boolean> {
        try {
            this.loading.set(true);
            this.error.set(null);

            await firstValueFrom(
                this.http.post<void>(`${this.minibarPath}/rooms/${roomId}/consume-batch?allowRecentBooking=true`, {
                    items,
                }),
            );

            return true;
        } catch (err: unknown) {
            const errorResponse = processError(
                err,
                'Failed to record minibar consumption',
                `${this.minibarPath}/rooms/${roomId}/consume-batch`,
            );
            this.error.set(errorResponse);
            return false;
        } finally {
            this.loading.set(false);
        }
    }

    public async canReportMinibar(roomId: number): Promise<boolean> {
        try {
            return await firstValueFrom(this.http.get<boolean>(`${this.minibarPath}/rooms/${roomId}/can-report`));
        } catch {
            return false;
        }
    }
}
