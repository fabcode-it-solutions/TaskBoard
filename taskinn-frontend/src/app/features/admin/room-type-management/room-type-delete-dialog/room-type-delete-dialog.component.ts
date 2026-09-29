import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { RoomDto, RoomTypeDto } from '../../../room/room';

export interface RoomTypeDeleteDialogData {
    roomType: RoomTypeDto;
    rooms: RoomDto[];
}

@Component({
    selector: 'tinn-room-type-delete-dialog',
    imports: [CommonModule, MatDialogModule, MatIcon],
    templateUrl: './room-type-delete-dialog.component.html',
})
export class RoomTypeDeleteDialogComponent {
    private readonly dialogRef = inject(MatDialogRef<RoomTypeDeleteDialogComponent>);

    public readonly data = inject<RoomTypeDeleteDialogData>(MAT_DIALOG_DATA);

    public onCancel(): void {
        this.dialogRef.close(false);
    }

    public onConfirm(): void {
        this.dialogRef.close(true);
    }

    public get hasRooms(): boolean {
        return this.data.rooms && this.data.rooms.length > 0;
    }
}
