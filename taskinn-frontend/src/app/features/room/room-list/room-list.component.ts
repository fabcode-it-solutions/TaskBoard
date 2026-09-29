import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { MatIcon } from '@angular/material/icon';
import { PageChangeEvent, PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { ToastrService } from 'ngx-toastr';
import { FormsModule } from '@angular/forms';
import { LightRoomDto } from '../room';
import {
    ConfirmationDialogComponent,
    ConfirmationDialogData,
} from '../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { ErrorResponse } from '../../../shared/interfaces/error';
import { RoomService } from '../room.service';
import {
    RoomOverrideDialogComponent,
    RoomOverrideDialogData,
} from '../room-override-dialog/room-override-dialog.component';
import { firstValueFrom } from 'rxjs';

@Component({
    selector: 'tinn-room-list',
    standalone: true,
    imports: [CommonModule, PageHeaderComponent, MatIcon, FormsModule, PaginatorComponent],
    templateUrl: './room-list.component.html',
})
export class RoomListComponent implements OnInit {
    private readonly roomService = inject(RoomService);
    private readonly toastr = inject(ToastrService);
    private readonly dialog = inject(MatDialog);

    public readonly loading = this.roomService.loading;
    public readonly rooms = this.roomService.roomList;

    public searchTerm = signal('');
    // New status filter to show/hide inactive rooms in the UI
    public statusFilter = signal<'active' | 'inactive' | 'all'>('active');
    public pageSize = signal(9);
    public pageIndex = signal(0);
    public pageSizeOptions = [9, 18, 36];

    public filteredRooms = computed(() => {
        const search = this.searchTerm().toLowerCase().trim();
        const status = this.statusFilter();
        const roomList = this.rooms();

        if (!roomList) {
            return [];
        }

        let filtered = roomList;

        // Filter by active/inactive status
        if (status === 'active') {
            filtered = filtered.filter((room) => room.active);
        } else if (status === 'inactive') {
            filtered = filtered.filter((room) => !room.active);
        }

        // Filter by search term
        if (search) {
            filtered = filtered.filter((room) => room.roomNumber.toLowerCase().includes(search));
        }

        return filtered;
    });

    public paginatedRooms = computed(() => {
        const filtered = this.filteredRooms();
        const startIndex = this.pageIndex() * this.pageSize();
        return filtered.slice(startIndex, startIndex + this.pageSize());
    });

    async ngOnInit(): Promise<void> {
        // Fetch ALL rooms for the admin management page
        await this.roomService.getAllRooms();
    }

    public async onDisableRoom(room: LightRoomDto): Promise<void> {
        const confirmData: ConfirmationDialogData = {
            title: 'Disable Room',
            message: `Are you sure you want to disable the room ${room.roomNumber}? This action cannot be undone.`,
            confirmText: 'Disable',
            cancelText: 'Cancel',
            danger: true,
        };

        const confirmDialogRef = this.dialog.open(ConfirmationDialogComponent, {
            width: '500px',
            data: confirmData,
        });

        const confirmed = await confirmDialogRef.afterClosed().toPromise();
        if (!confirmed) {
            return;
        }

        try {
            await this.roomService.disableRoom(room.id);
            this.toastr.success(`Room ${room.roomNumber} has been successfully disabled.`, 'Success');
            // Refresh the list to reflect the new state (room will now be inactive)
            await this.roomService.getAllRooms();
        } catch (error: unknown) {
            const errorMessage = (error as ErrorResponse)?.message || 'An unknown error occurred.';
            this.toastr.error(errorMessage, 'Failed to disable room');
        }
    }

    public onSearchChange(value: string): void {
        this.searchTerm.set(value);
        this.pageIndex.set(0);
    }

    public clearSearch(): void {
        this.searchTerm.set('');
        this.pageIndex.set(0);
    }

    public onStatusFilterChange(status: 'active' | 'inactive' | 'all'): void {
        this.statusFilter.set(status);
        this.pageIndex.set(0);
    }

    public onPageChange(event: PageChangeEvent): void {
        this.pageIndex.set(event.pageIndex);
        this.pageSize.set(event.pageSize);
    }

    public async onEditOverrides(room: LightRoomDto): Promise<void> {
        const dialogData: RoomOverrideDialogData = {
            room: room,
        };

        const dialogRef = this.dialog.open(RoomOverrideDialogComponent, {
            width: '550px',
            data: dialogData,
            disableClose: true,
        });

        const result = await firstValueFrom(dialogRef.afterClosed());
        if (result) {
            await this.roomService.getAllRooms();
        }
    }

    public formatPrice(price: number | undefined): string {
        return price != null ? `€${price.toFixed(2)}` : '€0.00';
    }
}
