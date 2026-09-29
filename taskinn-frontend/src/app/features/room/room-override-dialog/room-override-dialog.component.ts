import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { ToastrService } from 'ngx-toastr';
import { RoomService } from '../room.service';
import { LightRoomDto, RoomDto, RoomRequestDto } from '../room';
import { ErrorResponse } from '../../../shared/interfaces/error';

export interface RoomOverrideDialogData {
    room: LightRoomDto;
}

@Component({
    selector: 'tinn-room-override-dialog',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatIcon],
    templateUrl: './room-override-dialog.component.html',
})
export class RoomOverrideDialogComponent implements OnInit {
    private readonly fb = inject(FormBuilder);
    private readonly dialogRef = inject(MatDialogRef<RoomOverrideDialogComponent>);
    private readonly roomService = inject(RoomService);
    private readonly toastr = inject(ToastrService);

    public readonly data = inject<RoomOverrideDialogData>(MAT_DIALOG_DATA);
    public loading = false;
    public roomDetails: RoomDto | null = null;

    public overrideForm = this.fb.group({
        capacityOverride: [1, [Validators.required, Validators.min(1), Validators.max(10)]],
        basePriceOverride: [0, [Validators.required, Validators.min(0.01)]],
        singleBedOverride: [0, [Validators.min(0), Validators.max(5)]],
        doubleBedOverride: [0, [Validators.min(0), Validators.max(5)]],
    });

    public async ngOnInit(): Promise<void> {
        this.loading = true;
        try {
            this.roomDetails = await this.roomService.fetchRoomById(this.data.room.id);

            // Initialize form with existing override values (or defaults from room type)
            this.overrideForm.patchValue({
                capacityOverride: this.roomDetails.capacityOverride ?? this.data.room.roomType.capacity,
                basePriceOverride: this.roomDetails.basePriceOverride ?? this.data.room.roomType.basePrice,
                singleBedOverride: this.roomDetails.singleBedOverride ?? this.data.room.roomType.singleBed ?? 0,
                doubleBedOverride: this.roomDetails.doubleBedOverride ?? this.data.room.roomType.doubleBed ?? 0,
            });
        } catch (error: unknown) {
            const err = error as ErrorResponse;
            this.toastr.error(err?.message || 'Failed to load room details', 'Error');
            this.dialogRef.close(false);
        } finally {
            this.loading = false;
        }
    }

    public onReset(): void {
        // Reset all override values to the room type defaults
        this.overrideForm.patchValue({
            capacityOverride: this.data.room.roomType.capacity,
            basePriceOverride: this.data.room.roomType.basePrice ?? 0,
            singleBedOverride: this.data.room.roomType.singleBed ?? 0,
            doubleBedOverride: this.data.room.roomType.doubleBed ?? 0,
        });
    }

    public async onSubmit(): Promise<void> {
        if (!this.roomDetails) {
            return;
        }

        if (this.overrideForm.invalid) {
            this.overrideForm.markAllAsTouched();
            return;
        }

        const formValue = this.overrideForm.getRawValue();
        const singleBed = formValue.singleBedOverride || 0;
        const doubleBed = formValue.doubleBedOverride || 0;

        if (singleBed + doubleBed === 0) {
            this.toastr.error('At least one bed (single or double) must be specified', 'Validation Error');
            this.overrideForm.get('singleBedOverride')?.markAsTouched();
            this.overrideForm.get('doubleBedOverride')?.markAsTouched();
            return;
        }

        this.loading = true;

        // Determine which values are different from the room type defaults (those are the overrides)
        const roomType = this.data.room.roomType;
        const capacityOverride = formValue.capacityOverride !== roomType.capacity ? formValue.capacityOverride : null;
        const basePriceOverride =
            formValue.basePriceOverride !== roomType.basePrice ? formValue.basePriceOverride : null;
        const singleBedOverride =
            formValue.singleBedOverride !== (roomType.singleBed ?? 0) ? formValue.singleBedOverride : null;
        const doubleBedOverride =
            formValue.doubleBedOverride !== (roomType.doubleBed ?? 0) ? formValue.doubleBedOverride : null;

        const roomRequest: RoomRequestDto = {
            id: this.roomDetails.id,
            roomNumber: this.roomDetails.number,
            floor: this.roomDetails.floor,
            status: this.roomDetails.status,
            active: this.roomDetails.active,
            svgPoints: this.roomDetails.svgPoints,
            roomTypeId: this.roomDetails.roomTypeId!,
            capacityOverride,
            basePriceOverride,
            singleBedOverride,
            doubleBedOverride,
        };

        try {
            await this.roomService.updateRoom(this.roomDetails.id, roomRequest);
            this.toastr.success('Room overrides saved successfully', 'Success');
            this.dialogRef.close(true);
        } catch (error: unknown) {
            const err = error as ErrorResponse;

            if (err?.validationErrors && err.validationErrors.length > 0) {
                const firstError = err.validationErrors[0];
                this.toastr.error(firstError.message, 'Validation Error');
            } else {
                const errorMessage = err?.message || 'An error occurred';
                this.toastr.error(errorMessage, 'Error');
            }
        } finally {
            this.loading = false;
        }
    }

    public onCancel(): void {
        this.dialogRef.close(false);
    }

    public get roomTypeName(): string {
        return this.data.room.roomType?.name || 'Unknown';
    }
}
