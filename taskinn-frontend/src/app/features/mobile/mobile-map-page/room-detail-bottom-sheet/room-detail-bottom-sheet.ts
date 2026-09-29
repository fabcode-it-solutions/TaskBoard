import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_BOTTOM_SHEET_DATA, MatBottomSheetRef } from '@angular/material/bottom-sheet';
import { MatIcon } from '@angular/material/icon';
import { RoomDto, RoomStatus } from '../../../room/room';
import { getRoomStatusBgClass } from '../../../../shared/util/status-colors';

export interface RoomDetailData {
    room: RoomDto;
    roomTypeName: string;
    roomTypeCapacity: number;
}

@Component({
    selector: 'tinn-room-detail-bottom-sheet',
    imports: [CommonModule, MatIcon],
    templateUrl: './room-detail-bottom-sheet.html',
    styleUrl: './room-detail-bottom-sheet.scss',
})
export class RoomDetailBottomSheetComponent {
    public readonly data = inject<RoomDetailData>(MAT_BOTTOM_SHEET_DATA);
    private readonly bottomSheetRef = inject(MatBottomSheetRef<RoomDetailBottomSheetComponent>);

    public readonly RoomStatus = RoomStatus;

    public close(): void {
        this.bottomSheetRef.dismiss();
    }

    public getStatusColor(status: RoomStatus): string {
        return getRoomStatusBgClass(status);
    }

    public getStatusLabel(status: RoomStatus): string {
        return status.replace(/_/g, ' ');
    }
}
