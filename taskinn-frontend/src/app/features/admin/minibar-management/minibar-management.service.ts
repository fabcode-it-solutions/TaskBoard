import { HttpClient } from '@angular/common/http';
import { inject, Injectable, WritableSignal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ErrorResponse } from '../../../shared/interfaces/error';
import { processError } from '../../../shared/util/error.utils';
import { InventoryItemDto, InventoryItemRequestDto } from '../../mobile/minibar';
import { MinibarManagementStore } from './minibar-management.store';

@Injectable({
    providedIn: 'root',
})
export class MinibarManagementService {
    private readonly http = inject(HttpClient);
    private readonly store = inject(MinibarManagementStore);
    private readonly basePath = `${environment.apiUrl}/minibar/items`;

    public async getAllItems(): Promise<void> {
        try {
            this.store.setLoading(true);
            this.store.setError(null);
            const data = await firstValueFrom(this.http.get<InventoryItemDto[]>(this.basePath));
            this.store.setItems(data);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to load minibar items', this.basePath);
            this.store.setError(errorResponse);
        } finally {
            this.store.setLoading(false);
        }
    }

    public async createItem(dto: InventoryItemRequestDto): Promise<InventoryItemDto | null> {
        try {
            this.store.setLoading(true);
            this.store.setError(null);
            const created = await firstValueFrom(this.http.post<InventoryItemDto>(this.basePath, dto));
            this.store.addItem(created);
            return created;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to create minibar item', this.basePath);
            this.store.setError(errorResponse);
            return null;
        } finally {
            this.store.setLoading(false);
        }
    }

    public async updateItem(id: number, dto: InventoryItemRequestDto): Promise<InventoryItemDto | null> {
        try {
            this.store.setLoading(true);
            this.store.setError(null);
            const updated = await firstValueFrom(this.http.put<InventoryItemDto>(`${this.basePath}/${id}`, dto));
            this.store.updateItem(updated);
            return updated;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to update minibar item', `${this.basePath}/${id}`);
            this.store.setError(errorResponse);
            return null;
        } finally {
            this.store.setLoading(false);
        }
    }

    public async deleteItem(id: number): Promise<boolean> {
        try {
            this.store.setLoading(true);
            this.store.setError(null);
            await firstValueFrom(this.http.delete<void>(`${this.basePath}/${id}`));
            this.store.removeItem(id);
            return true;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to delete minibar item', `${this.basePath}/${id}`);
            this.store.setError(errorResponse);
            return false;
        } finally {
            this.store.setLoading(false);
        }
    }

    public get loading(): WritableSignal<boolean> {
        return this.store.loading;
    }

    public get error(): WritableSignal<ErrorResponse | null> {
        return this.store.error;
    }

    public get items(): WritableSignal<InventoryItemDto[] | null> {
        return this.store.items;
    }
}
