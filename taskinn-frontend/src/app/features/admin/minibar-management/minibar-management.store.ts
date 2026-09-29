import { Injectable, signal, WritableSignal } from '@angular/core';
import { ErrorResponse } from '../../../shared/interfaces/error';
import { InventoryItemDto } from '../../mobile/minibar';

@Injectable({
    providedIn: 'root',
})
export class MinibarManagementStore {
    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);
    public items: WritableSignal<InventoryItemDto[] | null> = signal(null);

    public setLoading(loading: boolean): void {
        this.loading.set(loading);
    }

    public setError(error: ErrorResponse | null): void {
        this.error.set(error);
    }

    public setItems(items: InventoryItemDto[]): void {
        this.items.set(items);
    }

    public addItem(item: InventoryItemDto): void {
        const currentItems = this.items();
        if (currentItems) {
            this.items.set([...currentItems, item]);
        }
    }

    public updateItem(updatedItem: InventoryItemDto): void {
        const currentItems = this.items();
        if (currentItems) {
            this.items.set(currentItems.map((item) => (item.id === updatedItem.id ? updatedItem : item)));
        }
    }

    public removeItem(id: number): void {
        const currentItems = this.items();
        if (currentItems) {
            this.items.set(currentItems.filter((item) => item.id !== id));
        }
    }
}
