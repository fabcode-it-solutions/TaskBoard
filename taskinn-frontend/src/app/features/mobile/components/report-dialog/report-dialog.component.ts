import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatOptionModule } from '@angular/material/core';
import { ToastrService } from 'ngx-toastr';

import { MaintenanceService } from '../../../maintenance/maintenance.service';
import { RoomService } from '../../../room/room.service';
import { MaintenanceTicketCreationUpdateDto, Priority, TicketStatus } from '../../../maintenance/maintenance';
import { MandatoryMarkComponent } from '../../../../shared/components/mandatory-mark/mandatory-mark.component';
import { LightRoomDto } from '../../../room/room';

export interface ReportDialogData {
    roomId?: number;
    roomNumber?: string;
}

@Component({
    selector: 'tinn-report-dialog',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatIcon,
        MatAutocompleteModule,
        MatOptionModule,
        MandatoryMarkComponent,
    ],
    templateUrl: './report-dialog.component.html',
    styleUrl: './report-dialog.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportDialogComponent implements OnInit {
    private readonly fb = inject(FormBuilder);
    private readonly maintenanceService = inject(MaintenanceService);
    private readonly roomService = inject(RoomService);
    private readonly toastr = inject(ToastrService);
    private readonly dialogRef = inject(MatDialogRef<ReportDialogComponent>);
    private readonly data = inject<ReportDialogData>(MAT_DIALOG_DATA);
    private readonly destroyRef = inject(DestroyRef);

    public readonly loading = this.maintenanceService.loading;
    public readonly rooms = this.roomService.roomList;
    public readonly Priority = Priority;
    public readonly priorityOptions = [
        { value: Priority.LOW, label: 'Low', icon: 'low_priority' },
        { value: Priority.MEDIUM, label: 'Medium', icon: 'drag_handle' },
        { value: Priority.HIGH, label: 'High', icon: 'priority_high' },
        { value: Priority.URGENT, label: 'Urgent', icon: 'warning' },
    ];

    public reportForm: FormGroup;
    public submitted = false;
    public roomSearchText = signal<string>('');
    public filteredRooms = computed(() => {
        const searchText = this.roomSearchText().toLowerCase().trim();
        const allRooms = this.rooms() || [];

        if (!searchText) {
            return allRooms;
        }

        const normalizedSearch = searchText.startsWith('room ') ? searchText.substring(5).trim() : searchText;

        return allRooms.filter((room) => room.roomNumber.toLowerCase().includes(normalizedSearch));
    });

    constructor() {
        this.reportForm = this.fb.group({
            roomId: [null as number | null, [Validators.required]],
            roomSearch: [''],
            title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
            priority: [Priority.MEDIUM, [Validators.required]],
            description: [''],
        });
    }

    public async ngOnInit(): Promise<void> {
        await this.roomService.getActiveRooms();

        // Pre-fill room if provided
        if (this.data?.roomId) {
            const room = this.rooms()?.find((r) => r.id === this.data.roomId);
            if (room) {
                this.reportForm.patchValue(
                    {
                        roomId: room.id,
                        roomSearch: `Room ${room.roomNumber}`,
                    },
                    { emitEvent: false },
                );
                // Disable room search when pre-filled from map
                this.reportForm.get('roomSearch')?.disable();
            }
        }

        this.reportForm
            .get('roomSearch')
            ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((value) => {
                if (typeof value === 'string') {
                    this.roomSearchText.set(value);
                } else {
                    this.roomSearchText.set('');
                }

                const currentRoomId = this.reportForm.get('roomId')?.value;
                if (currentRoomId && typeof value === 'string') {
                    const room = this.rooms()?.find((r) => r.id === currentRoomId);
                    if (room && value !== `Room ${room.roomNumber}`) {
                        this.reportForm.patchValue({ roomId: null }, { emitEvent: false });
                    }
                }
            });
    }

    public onRoomSelected(event: MatAutocompleteSelectedEvent): void {
        const room: LightRoomDto = event.option.value;

        this.reportForm.patchValue(
            {
                roomId: room.id,
                roomSearch: `Room ${room.roomNumber}`,
            },
            { emitEvent: false },
        );
    }

    public displayRoomFn(room: LightRoomDto | string | null): string {
        if (typeof room === 'string') {
            return room;
        }
        if (!room) {
            return '';
        }
        return `Room ${room.roomNumber}`;
    }

    public async onSubmit(): Promise<void> {
        this.submitted = true;

        if (!this.reportForm.get('roomId')?.value) {
            this.reportForm.get('roomId')?.setErrors({ required: true });
            this.reportForm.get('roomId')?.markAsTouched();
            this.toastr.error('Please select a room from the list');
            return;
        }

        if (this.reportForm.invalid) {
            this.reportForm.markAllAsTouched();
            this.toastr.error('Please fill in all required fields');
            return;
        }

        const formValue = this.reportForm.value;
        const ticketData: MaintenanceTicketCreationUpdateDto = {
            id: 0,
            title: formValue.title!,
            description: formValue.description || '',
            priority: formValue.priority!,
            status: TicketStatus.OPEN,
            roomId: formValue.roomId!,
        };

        await this.maintenanceService.createMaintenanceTicket(ticketData);

        if (!this.maintenanceService.error()) {
            this.toastr.success('Maintenance issue reported successfully');
            this.dialogRef.close(true);
        } else {
            this.toastr.error(this.maintenanceService.error()?.message || 'Failed to submit report');
        }
    }

    public onCancel(): void {
        this.dialogRef.close(false);
    }
}
