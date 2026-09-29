import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { ToastrService } from 'ngx-toastr';

import { MinibarService } from '../../minibar.service';
import { InventoryItemDto, MinibarConsumptionItem } from '../../minibar';

export interface MinibarDialogData {
    roomId: number;
    roomNumber: string;
}

interface ItemWithQuantity extends InventoryItemDto {
    selectedQuantity: number;
}

@Component({
    selector: 'tinn-minibar-dialog',
    imports: [CommonModule, MatIcon],
    templateUrl: './minibar-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MinibarDialogComponent implements OnInit {
    private readonly minibarService = inject(MinibarService);
    private readonly toastr = inject(ToastrService);
    private readonly dialogRef = inject(MatDialogRef<MinibarDialogComponent>);
    private readonly data = inject<MinibarDialogData>(MAT_DIALOG_DATA);

    public readonly loading = this.minibarService.loading;
    public readonly roomNumber = this.data.roomNumber;

    public items = signal<ItemWithQuantity[]>([]);

    public categorizedItems = computed(() => {
        const allItems = this.items();
        const categories = new Map<string, ItemWithQuantity[]>();

        for (const item of allItems) {
            const category = item.category || 'Other';
            if (!categories.has(category)) {
                categories.set(category, []);
            }
            categories.get(category)!.push(item);
        }

        return Array.from(categories.entries()).map(([category, categoryItems]) => ({
            category,
            items: categoryItems,
        }));
    });

    public hasSelectedItems = computed(() => {
        return this.items().some((item) => item.selectedQuantity > 0);
    });

    public totalSelectedItems = computed(() => {
        return this.items().reduce((total, item) => total + item.selectedQuantity, 0);
    });

    public async ngOnInit(): Promise<void> {
        const inventoryItems = await this.minibarService.getInventoryItems();

        const itemsWithQuantity: ItemWithQuantity[] = inventoryItems
            .filter((item) => item.currentStock > 0)
            .map((item) => ({
                ...item,
                selectedQuantity: 0,
            }));

        this.items.set(itemsWithQuantity);
    }

    public incrementQuantity(itemId: number): void {
        this.items.update((items) =>
            items.map((item) => {
                if (item.id === itemId && item.selectedQuantity < item.currentStock) {
                    return { ...item, selectedQuantity: item.selectedQuantity + 1 };
                }
                return item;
            }),
        );
    }

    public decrementQuantity(itemId: number): void {
        this.items.update((items) =>
            items.map((item) => {
                if (item.id === itemId && item.selectedQuantity > 0) {
                    return { ...item, selectedQuantity: item.selectedQuantity - 1 };
                }
                return item;
            }),
        );
    }

    public async onSubmit(): Promise<void> {
        const selectedItems: MinibarConsumptionItem[] = this.items()
            .filter((item) => item.selectedQuantity > 0)
            .map((item) => ({
                itemId: item.id,
                quantity: item.selectedQuantity,
            }));

        if (selectedItems.length === 0) {
            this.toastr.warning('Please select at least one item');
            return;
        }

        const success = await this.minibarService.recordConsumption(this.data.roomId, selectedItems);

        if (success) {
            this.toastr.success('Minibar consumption recorded successfully');
            this.dialogRef.close(true);
        } else {
            this.toastr.error(this.minibarService.error()?.message || 'Failed to record minibar consumption');
        }
    }

    public onCancel(): void {
        this.dialogRef.close(false);
    }
}
