import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';

export interface ConfirmationDialogData {
    title: string;
    message: string;
    confirmText?: string;
    cancelText?: string;
    danger?: boolean;
}

@Component({
    selector: 'tinn-confirmation-dialog',
    imports: [CommonModule, MatDialogModule, MatIcon],
    templateUrl: './confirmation-dialog.component.html',
})
export class ConfirmationDialogComponent {
    public readonly dialogRef = inject(MatDialogRef<ConfirmationDialogComponent>);
    public readonly data = inject<ConfirmationDialogData>(MAT_DIALOG_DATA);

    public get confirmText(): string {
        return this.data.confirmText || 'Confirm';
    }

    public get cancelText(): string {
        return this.data.cancelText || 'Cancel';
    }

    public onCancel(): void {
        this.dialogRef.close(false);
    }

    public onConfirm(): void {
        this.dialogRef.close(true);
    }
}
