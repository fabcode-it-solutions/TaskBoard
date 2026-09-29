import { Component, computed, EventEmitter, inject, input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIcon } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import {
    ConfirmationDialogComponent,
    ConfirmationDialogData,
} from '../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { RoomDto } from '../../room/room';

@Component({
    selector: 'tinn-room-list',
    imports: [CommonModule, FormsModule, MatIcon],
    templateUrl: './registered-rooms.component.html',
})
export class RegisteredRoomsComponent {
    private readonly dialog = inject(MatDialog);

    public rooms = input.required<RoomDto[]>();

    @Output() public readonly editRoom = new EventEmitter<RoomDto>();
    @Output() public readonly deleteRoom = new EventEmitter<number>();

    public searchTerm = signal<string>('');

    public filteredRooms = computed(() => {
        const allRooms = [...this.rooms()].sort((a, b) => a.number.localeCompare(b.number));
        const search = this.searchTerm().toLowerCase().trim();
        if (!search) {
            return allRooms;
        }
        return allRooms.filter((room) => room.number.toLowerCase().includes(search));
    });

    public onEdit(room: RoomDto): void {
        this.editRoom.emit(room);
    }

    public async onDelete(roomId: number): Promise<void> {
        const confirmData: ConfirmationDialogData = {
            title: 'Delete Room',
            message: 'Are you sure you want to delete this room? This action cannot be undone.',
            confirmText: 'Delete',
            cancelText: 'Cancel',
            danger: true,
        };

        const confirmDialogRef = this.dialog.open(ConfirmationDialogComponent, {
            width: '500px',
            data: confirmData,
        });

        const confirmed = await firstValueFrom(confirmDialogRef.afterClosed());
        if (confirmed) {
            this.deleteRoom.emit(roomId);
        }
    }
}
