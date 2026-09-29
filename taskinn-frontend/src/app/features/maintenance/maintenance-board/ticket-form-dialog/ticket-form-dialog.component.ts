import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { TextFieldModule } from '@angular/cdk/text-field';
import { ToastrService } from 'ngx-toastr';
import { MaintenanceService } from '../../maintenance.service';
import { RoomService } from '../../../room/room.service';
import { MaintenanceTicketDto, Priority, TicketStatus } from '../../maintenance';
import { MandatoryMarkComponent } from '../../../../shared/components/mandatory-mark/mandatory-mark.component';
import {
    ConfirmationDialogComponent,
    ConfirmationDialogData,
} from '../../../../shared/components/confirmation-dialog/confirmation-dialog.component';

export interface TicketDialogData {
    mode: 'create' | 'edit' | 'view';
    ticket?: MaintenanceTicketDto;
}

@Component({
    selector: 'tinn-ticket-form-dialog',
    imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatIcon, MandatoryMarkComponent, TextFieldModule],
    templateUrl: './ticket-form-dialog.component.html',
})
export class TicketFormDialogComponent implements OnInit {
    private readonly fb = inject(FormBuilder);
    private readonly dialogRef = inject(MatDialogRef<TicketFormDialogComponent>);
    private readonly dialog = inject(MatDialog);
    private readonly maintenanceService = inject(MaintenanceService);
    private readonly roomService = inject(RoomService);
    private readonly toastr = inject(ToastrService);

    public readonly data = inject<TicketDialogData>(MAT_DIALOG_DATA);
    public readonly loading = this.maintenanceService.loading;
    public readonly rooms = this.roomService.roomList;
    public currentMode: 'create' | 'edit' | 'view' = this.data.mode;

    public ticketForm = this.fb.group({
        title: ['', [Validators.required]],
        description: ['', [Validators.required]],
        priority: [Priority.MEDIUM, [Validators.required]],
        status: [TicketStatus.OPEN, [Validators.required]],
        roomId: [null as number | null, [Validators.required]],
    });

    public readonly priorities = Object.values(Priority);
    public readonly statuses = Object.values(TicketStatus);

    public async ngOnInit(): Promise<void> {
        await this.roomService.getActiveRooms();

        if ((this.data.mode === 'edit' || this.data.mode === 'view') && this.data.ticket) {
            this.ticketForm.patchValue({
                title: this.data.ticket.title,
                description: this.data.ticket.description,
                priority: this.data.ticket.priority,
                status: this.data.ticket.status,
                roomId: this.data.ticket.roomId,
            });

            if (this.data.mode === 'view') {
                this.ticketForm.disable();
            }
        }
    }

    public async onSubmit(): Promise<void> {
        if (this.ticketForm.invalid) {
            this.ticketForm.markAllAsTouched();
            return;
        }

        const formValue = this.ticketForm.value;
        const ticketData = {
            id: this.data.ticket?.id || 0,
            title: formValue.title!,
            description: formValue.description!,
            priority: formValue.priority!,
            status: formValue.status!,
            roomId: formValue.roomId!,
        };

        if (this.data.mode === 'create') {
            await this.maintenanceService.createMaintenanceTicket(ticketData);
            if (!this.maintenanceService.error()) {
                this.toastr.success('Maintenance ticket created successfully', 'Success');
                this.dialogRef.close(true);
            } else {
                this.toastr.error(this.maintenanceService.error()?.message || 'Failed to create ticket', 'Error');
            }
        } else {
            await this.maintenanceService.updateMaintenanceTicket(this.data.ticket!.id, ticketData);
            if (!this.maintenanceService.error()) {
                this.toastr.success('Maintenance ticket updated successfully', 'Success');
                this.dialogRef.close(true);
            } else {
                this.toastr.error(this.maintenanceService.error()?.message || 'Failed to update ticket', 'Error');
            }
        }
    }

    public onCancel(): void {
        this.dialogRef.close(false);
    }

    public switchToEditMode(): void {
        this.currentMode = 'edit';
        this.ticketForm.enable();
    }

    public async onDelete(): Promise<void> {
        if (!this.data.ticket) {
            return;
        }

        const confirmData: ConfirmationDialogData = {
            title: 'Delete Ticket',
            message: `Are you sure you want to delete "${this.data.ticket.title}"? This action cannot be undone.`,
            confirmText: 'Delete',
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

        await this.maintenanceService.deleteMaintenanceTicket(this.data.ticket.id);
        if (!this.maintenanceService.error()) {
            this.toastr.success('Maintenance ticket deleted successfully', 'Success');
            this.dialogRef.close(true);
        } else {
            this.toastr.error(this.maintenanceService.error()?.message || 'Failed to delete ticket', 'Error');
        }
    }

    public get isViewMode(): boolean {
        return this.currentMode === 'view';
    }

    public get isCreateMode(): boolean {
        return this.currentMode === 'create';
    }

    public get dialogTitle(): string {
        if (this.isCreateMode) {
            return 'Create Maintenance Ticket';
        }
        if (this.isViewMode) {
            return 'Maintenance Ticket Details';
        }
        return 'Edit Maintenance Ticket';
    }

    public getRoomNumber(): string {
        if (!this.data.ticket) {
            return '';
        }
        return this.data.ticket.roomNumber;
    }

    public getReporterName(): string {
        if (!this.data.ticket) {
            return '';
        }
        return this.data.ticket.reporterName;
    }
}
