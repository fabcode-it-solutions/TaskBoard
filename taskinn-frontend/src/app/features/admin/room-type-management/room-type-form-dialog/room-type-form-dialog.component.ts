import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { ToastrService } from 'ngx-toastr';
import { RoomTypeService } from '../../../room/room-type.service';
import { RoomTypeDto } from '../../../room/room';
import { MandatoryMarkComponent } from '../../../../shared/components/mandatory-mark/mandatory-mark.component';
import { ErrorResponse } from '../../../../shared/interfaces/error';

export interface RoomTypeDialogData {
    mode: 'create' | 'edit';
    roomType?: RoomTypeDto;
}

@Component({
    selector: 'tinn-room-type-form-dialog',
    imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatIcon, MandatoryMarkComponent],
    templateUrl: './room-type-form-dialog.component.html',
})
export class RoomTypeFormDialogComponent implements OnInit {
    private readonly fb = inject(FormBuilder);
    private readonly dialogRef = inject(MatDialogRef<RoomTypeFormDialogComponent>);
    private readonly roomTypeService = inject(RoomTypeService);
    private readonly toastr = inject(ToastrService);

    public readonly data = inject<RoomTypeDialogData>(MAT_DIALOG_DATA);
    public loading = false;

    public roomTypeForm = this.fb.group({
        name: ['', [Validators.required, Validators.maxLength(100)]],
        description: ['', [Validators.maxLength(500)]],
        capacity: [1, [Validators.required, Validators.min(1), Validators.max(10)]],
        basePrice: [0, [Validators.required, Validators.min(0.01)]],
        singleBed: [0, [Validators.min(0), Validators.max(5)]],
        doubleBed: [0, [Validators.min(0), Validators.max(5)]],
    });

    public async ngOnInit(): Promise<void> {
        if (this.data.mode === 'edit' && this.data.roomType) {
            this.roomTypeForm.patchValue({
                name: this.data.roomType.name,
                description: this.data.roomType.description || '',
                capacity: this.data.roomType.capacity,
                basePrice: this.data.roomType.basePrice,
                singleBed: this.data.roomType.singleBed || 0,
                doubleBed: this.data.roomType.doubleBed || 0,
            });
        }
    }

    public async onSubmit(): Promise<void> {
        if (this.roomTypeForm.invalid) {
            this.roomTypeForm.markAllAsTouched();
            return;
        }

        const formValue = this.roomTypeForm.value;
        const singleBed = formValue.singleBed || 0;
        const doubleBed = formValue.doubleBed || 0;

        if (singleBed + doubleBed === 0) {
            this.toastr.error('At least one bed (single or double) must be specified', 'Validation Error');
            this.roomTypeForm.get('singleBed')?.markAsTouched();
            this.roomTypeForm.get('doubleBed')?.markAsTouched();
            return;
        }

        if (singleBed + 2 * doubleBed !== formValue.capacity!) {
            this.toastr.error('The total number of beds must equal the room capacity', 'Validation Error');
            this.roomTypeForm.get('singleBed')?.markAsTouched();
            this.roomTypeForm.get('doubleBed')?.markAsTouched();
            this.roomTypeForm.get('capacity')?.markAsTouched();
            return;
        }

        this.loading = true;
        const roomTypeData = {
            name: formValue.name!,
            description: formValue.description || undefined,
            capacity: formValue.capacity!,
            basePrice: formValue.basePrice!,
            singleBed: formValue.singleBed != null ? formValue.singleBed : undefined,
            doubleBed: formValue.doubleBed != null ? formValue.doubleBed : undefined,
        };

        try {
            if (this.data.mode === 'create') {
                await this.roomTypeService.createRoomType(roomTypeData);
                this.toastr.success('Room type created successfully', 'Success');
                this.dialogRef.close(true);
            } else {
                await this.roomTypeService.updateRoomType(this.data.roomType!.id, roomTypeData);
                this.toastr.success('Room type updated successfully', 'Success');
                this.dialogRef.close(true);
            }
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

    public get isCreateMode(): boolean {
        return this.data.mode === 'create';
    }

    public get dialogTitle(): string {
        return this.isCreateMode ? 'Create Room Type' : 'Edit Room Type';
    }
}
