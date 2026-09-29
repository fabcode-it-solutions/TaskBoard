import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ToastrService } from 'ngx-toastr';
import { MandatoryMarkComponent } from '../../../../shared/components/mandatory-mark/mandatory-mark.component';
import { InventoryItemDto, InventoryItemRequestDto } from '../../../mobile/minibar';
import { MinibarManagementService } from '../minibar-management.service';

export interface MinibarFormDialogData {
    mode: 'create' | 'edit';
    item?: InventoryItemDto;
}

@Component({
    selector: 'tinn-minibar-form-dialog',
    imports: [ReactiveFormsModule, MatIconModule, MandatoryMarkComponent],
    templateUrl: './minibar-form-dialog.component.html',
})
export class MinibarFormDialogComponent {
    private readonly dialogRef = inject(MatDialogRef<MinibarFormDialogComponent>);
    private readonly fb = inject(FormBuilder);
    private readonly minibarService = inject(MinibarManagementService);
    private readonly toastr = inject(ToastrService);
    protected readonly data: MinibarFormDialogData = inject(MAT_DIALOG_DATA);

    protected form: FormGroup;
    protected loading = false;

    constructor() {
        this.form = this.fb.group({
            name: [this.data.item?.name ?? '', [Validators.required, Validators.maxLength(100)]],
            category: [this.data.item?.category ?? '', [Validators.maxLength(100)]],
            unitOfMeasurement: [this.data.item?.unitOfMeasurement ?? '', [Validators.maxLength(50)]],
            unitPrice: [this.data.item?.unitPrice ?? null, [Validators.required, Validators.min(0.01)]],
            currentStock: [this.data.item?.currentStock ?? null, [Validators.required, Validators.min(0)]],
            reorderStock: [this.data.item?.reorderStock ?? null, [Validators.min(0)]],
        });
    }

    protected get isEditMode(): boolean {
        return this.data.mode === 'edit';
    }

    protected get dialogTitle(): string {
        return this.isEditMode ? 'Edit Minibar Item' : 'Create Minibar Item';
    }

    protected async onSubmit(): Promise<void> {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        this.loading = true;
        const formValue: InventoryItemRequestDto = this.form.value;

        let result: InventoryItemDto | null = null;

        if (this.isEditMode && this.data.item) {
            result = await this.minibarService.updateItem(this.data.item.id, formValue);
        } else {
            result = await this.minibarService.createItem(formValue);
        }

        this.loading = false;

        const error = this.minibarService.error();
        if (error) {
            let errorMessage = error.message || 'An error occurred';
            if (error.validationErrors && error.validationErrors.length > 0) {
                errorMessage = error.validationErrors.map((e) => e.message).join(', ');
            }
            this.toastr.error(errorMessage);
        } else if (result) {
            this.toastr.success(
                this.isEditMode ? 'Minibar item updated successfully' : 'Minibar item created successfully',
            );
            this.dialogRef.close(true);
        }
    }

    protected onCancel(): void {
        this.dialogRef.close(false);
    }
}
