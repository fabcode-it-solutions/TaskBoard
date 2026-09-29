import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { MatIcon } from '@angular/material/icon';
import { ToastrService } from 'ngx-toastr';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { RoomTypeService } from '../../room/room-type.service';
import { RoomTypeDto } from '../../room/room';
import {
    RoomTypeDialogData,
    RoomTypeFormDialogComponent,
} from './room-type-form-dialog/room-type-form-dialog.component';
import {
    RoomTypeDeleteDialogComponent,
    RoomTypeDeleteDialogData,
} from './room-type-delete-dialog/room-type-delete-dialog.component';
import { ErrorResponse } from '../../../shared/interfaces/error';

@Component({
    selector: 'tinn-room-type-management',
    standalone: true,
    imports: [CommonModule, PageHeaderComponent, MatIcon],
    templateUrl: './room-type-management.component.html',
})
export class RoomTypeManagementComponent implements OnInit {
    private readonly roomTypeService = inject(RoomTypeService);
    private readonly toastr = inject(ToastrService);
    private readonly dialog = inject(MatDialog);

    public readonly loading = this.roomTypeService.loading;
    public roomTypes = signal<RoomTypeDto[]>([]);
    public searchTerm = signal('');

    public filteredRoomTypes = computed(() => {
        const search = this.searchTerm().toLowerCase().trim();
        const types = this.roomTypes();

        if (!search) {
            return types;
        }

        return types.filter(
            (type) => type.name.toLowerCase().includes(search) || type.description?.toLowerCase().includes(search),
        );
    });

    async ngOnInit(): Promise<void> {
        await this.loadRoomTypes();
    }

    private async loadRoomTypes(): Promise<void> {
        try {
            await this.roomTypeService.getAllRoomTypes();
            const liteTypes = this.roomTypeService.roomTypeList();

            if (liteTypes) {
                const fullTypes = await Promise.all(
                    liteTypes.map((lite) => this.roomTypeService.getRoomTypeById(lite.id)),
                );
                this.roomTypes.set(fullTypes);
            }
        } catch (error: unknown) {
            const errorMessage = (error as ErrorResponse)?.message || 'Failed to load room types';
            this.toastr.error(errorMessage, 'Error');
        }
    }

    public async onCreateRoomType(): Promise<void> {
        const dialogData: RoomTypeDialogData = {
            mode: 'create',
        };

        const dialogRef = this.dialog.open(RoomTypeFormDialogComponent, {
            width: '600px',
            data: dialogData,
            disableClose: true,
        });

        const result = await firstValueFrom(dialogRef.afterClosed());
        if (result) {
            await this.loadRoomTypes();
        }
    }

    public async onEditRoomType(roomType: RoomTypeDto): Promise<void> {
        const dialogData: RoomTypeDialogData = {
            mode: 'edit',
            roomType: roomType,
        };

        const dialogRef = this.dialog.open(RoomTypeFormDialogComponent, {
            width: '600px',
            data: dialogData,
            disableClose: true,
        });

        const result = await firstValueFrom(dialogRef.afterClosed());
        if (result) {
            await this.loadRoomTypes();
        }
    }

    public onSearchChange(value: string): void {
        this.searchTerm.set(value);
    }

    public clearSearch(): void {
        this.searchTerm.set('');
    }

    public formatPrice(price: number): string {
        return `€${price.toFixed(2)}`;
    }

    public async onDeleteRoomType(roomType: RoomTypeDto): Promise<void> {
        try {
            // First, check if there are rooms using this room type
            const rooms = await this.roomTypeService.getRoomsByRoomTypeId(roomType.id);

            const dialogData: RoomTypeDeleteDialogData = {
                roomType: roomType,
                rooms: rooms,
            };

            const dialogRef = this.dialog.open(RoomTypeDeleteDialogComponent, {
                width: '500px',
                data: dialogData,
                disableClose: true,
            });

            const result = await firstValueFrom(dialogRef.afterClosed());
            if (result) {
                try {
                    await this.roomTypeService.deleteRoomType(roomType.id);
                    this.toastr.success('Room type deleted successfully', 'Success');
                    await this.loadRoomTypes();
                } catch (error: unknown) {
                    const err = error as ErrorResponse;
                    const errorMessage = err?.message || 'Failed to delete room type';
                    this.toastr.error(errorMessage, 'Error');
                }
            }
        } catch (error: unknown) {
            const err = error as ErrorResponse;
            const errorMessage = err?.message || 'Failed to check rooms';
            this.toastr.error(errorMessage, 'Error');
        }
    }
}
