import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ToastrService } from 'ngx-toastr';
import { ConfirmationDialogComponent } from '../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { InventoryItemDto } from '../../mobile/minibar';
import { MinibarFormDialogComponent } from './minibar-form-dialog/minibar-form-dialog.component';
import { MinibarManagementService } from './minibar-management.service';

@Component({
    selector: 'tinn-minibar-management',
    imports: [PageHeaderComponent, MatIconModule, FormsModule],
    templateUrl: './minibar-management.component.html',
})
export class MinibarManagementComponent implements OnInit {
    private readonly minibarService = inject(MinibarManagementService);
    private readonly dialog = inject(MatDialog);
    private readonly toastr = inject(ToastrService);

    protected readonly loading = this.minibarService.loading;
    protected readonly items = this.minibarService.items;
    protected readonly searchTerm = signal('');

    protected readonly filteredItems = computed(() => {
        const items = this.items();
        const search = this.searchTerm().toLowerCase().trim();

        if (!items) {
            return [];
        }

        if (!search) {
            return items;
        }

        return items.filter(
            (item) =>
                item.name.toLowerCase().includes(search) ||
                item.category?.toLowerCase().includes(search) ||
                item.id.toString().includes(search),
        );
    });

    async ngOnInit(): Promise<void> {
        await this.loadItems();
    }

    protected async loadItems(): Promise<void> {
        await this.minibarService.getAllItems();
    }

    protected onSearchChange(value: string): void {
        this.searchTerm.set(value);
    }

    protected clearSearch(): void {
        this.searchTerm.set('');
    }

    protected isLowStock(item: InventoryItemDto): boolean {
        return item.currentStock <= item.reorderStock;
    }

    protected openCreateDialog(): void {
        const dialogRef = this.dialog.open(MinibarFormDialogComponent, {
            width: '700px',
            data: { mode: 'create' },
            disableClose: true,
        });

        dialogRef.afterClosed().subscribe(async (result) => {
            if (result) {
                await this.loadItems();
            }
        });
    }

    protected openEditDialog(item: InventoryItemDto): void {
        const dialogRef = this.dialog.open(MinibarFormDialogComponent, {
            width: '700px',
            data: { mode: 'edit', item },
            disableClose: true,
        });

        dialogRef.afterClosed().subscribe(async (result) => {
            if (result) {
                await this.loadItems();
            }
        });
    }

    protected openDeleteDialog(item: InventoryItemDto): void {
        const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
            width: '450px',
            data: {
                title: 'Delete Minibar Item',
                message: `Are you sure you want to delete "${item.name}"? This action cannot be undone.`,
                confirmText: 'Delete',
                cancelText: 'Cancel',
                danger: true,
            },
        });

        dialogRef.afterClosed().subscribe(async (confirmed) => {
            if (confirmed) {
                const success = await this.minibarService.deleteItem(item.id);

                const error = this.minibarService.error();
                if (error) {
                    let errorMessage = error.message || 'Failed to delete minibar item';
                    if (error.validationErrors && error.validationErrors.length > 0) {
                        errorMessage = error.validationErrors.map((e) => e.message).join(', ');
                    }
                    this.toastr.error(errorMessage);
                } else if (success) {
                    this.toastr.success('Minibar item deleted successfully');
                }
            }
        });
    }
}
