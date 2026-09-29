import { Component, EventEmitter, inject, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIcon } from '@angular/material/icon';
import { RoomTypeService } from '../../room/room-type.service';
import { RoomDto, RoomRequestDto, RoomStatus } from '../../room/room';

@Component({
    selector: 'tinn-room-properties-panel',
    imports: [CommonModule, ReactiveFormsModule, MatIcon],
    templateUrl: './room-properties-panel.component.html',
})
export class RoomPropertiesPanelComponent implements OnInit {
    @Input({ required: true }) public room!: RoomDto;
    @Output() public readonly roomSaved = new EventEmitter<RoomRequestDto>();
    @Output() public readonly cancelled = new EventEmitter<void>();

    private readonly fb = inject(FormBuilder);
    public readonly roomTypeService = inject(RoomTypeService);

    public form: FormGroup = this.fb.group({
        roomNumber: ['', [Validators.required, Validators.pattern(/^\d+$/)]],
        roomTypeId: [null, Validators.required],
    });

    public async ngOnInit(): Promise<void> {
        if (this.roomTypeService.roomTypeList() === null && !this.roomTypeService.loading()) {
            await this.roomTypeService.getAllRoomTypes();
        }

        if (this.room) {
            this.form.patchValue({
                roomNumber: this.room.number,
                roomTypeId: this.room.roomTypeId ?? null,
            });
        }
    }

    public onSubmit(): void {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        const roomRequest: RoomRequestDto = {
            id: this.room.id,
            roomNumber: this.form.value.roomNumber,
            floor: this.room.floor,
            status: RoomStatus.AVAILABLE,
            svgPoints: this.room.svgPoints,
            active: true,
            roomTypeId: this.form.value.roomTypeId,
        };

        this.roomSaved.emit(roomRequest);
    }

    public onCancel(): void {
        this.cancelled.emit();
    }

    /**
     * Prevents non-numeric characters from being entered
     */
    public onRoomNumberKeyPress(event: KeyboardEvent): boolean {
        const charCode = event.which ? event.which : event.keyCode;
        // Allow only digits (0-9)
        if (charCode < 48 || charCode > 57) {
            event.preventDefault();
            return false;
        }
        return true;
    }
}
