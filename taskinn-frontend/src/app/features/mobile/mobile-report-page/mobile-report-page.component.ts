import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
    AbstractControl,
    FormBuilder,
    FormGroup,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatOptionModule } from '@angular/material/core';
import { ToastrService } from 'ngx-toastr';

import { MaintenanceService } from '../../maintenance/maintenance.service';
import { RoomService } from '../../room/room.service';
import { MaintenanceTicketCreationUpdateDto, Priority, TicketStatus } from '../../maintenance/maintenance';
import { MobileHeaderComponent } from '../components/mobile-header/mobile-header';
import { MandatoryMarkComponent } from '../../../shared/components/mandatory-mark/mandatory-mark.component';
import { LightRoomDto } from '../../room/room';
import { MobileNavigationService } from '../mobile-navigation.service';

function spaceAwareStringMinLength(minLength: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
        if (typeof control.value === 'string') {
            const length = control.value.trim().length;
            if (length < minLength) {
                return { minlength: { requiredLength: minLength, actualLength: length } };
            }
        }
        return null;
    };
}

@Component({
    selector: 'tinn-mobile-report-page',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatIcon,
        MatAutocompleteModule,
        MatOptionModule,
        MobileHeaderComponent,
        MandatoryMarkComponent,
    ],
    templateUrl: './mobile-report-page.component.html',
    styleUrl: './mobile-report-page.component.scss',
})
export class MobileReportPageComponent implements OnInit {
    private readonly fb = inject(FormBuilder);
    private readonly maintenanceService = inject(MaintenanceService);
    private readonly roomService = inject(RoomService);
    private readonly router = inject(Router);
    private readonly toastr = inject(ToastrService);
    private readonly navigationService = inject(MobileNavigationService);

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
            title: ['', [Validators.required, spaceAwareStringMinLength(3), Validators.maxLength(100)]],
            priority: [Priority.MEDIUM, [Validators.required]],
            description: ['', [Validators.maxLength(1000)]],
        });
    }

    async ngOnInit(): Promise<void> {
        await this.roomService.getActiveRooms();

        this.reportForm.get('roomSearch')?.valueChanges.subscribe((value) => {
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

        // Validate that a room was actually selected (not just typed)
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
            this.reportForm.reset();
            this.submitted = false;
            this.roomSearchText.set('');
            this.navigationService.navigateTo('/mobile');
        } else {
            this.toastr.error(this.maintenanceService.error()?.message || 'Failed to submit report');
        }
    }

    public onCancel(): void {
        this.navigationService.back('/mobile');
    }

    public onBackClick(): void {
        this.navigationService.back('/mobile');
    }
}
