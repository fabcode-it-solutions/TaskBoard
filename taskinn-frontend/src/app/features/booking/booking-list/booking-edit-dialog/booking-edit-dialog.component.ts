import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { BookingService } from '../../booking.service';
import { BookingStore } from '../../booking.store';
import { RoomService } from '../../../room/room.service';
import { ToastrService } from 'ngx-toastr';
import { AssignedRoom, CreateOrUpdateBookingRequest, DetailedBooking, GuestInfo } from '../../interfaces/room';

export interface BookingEditDialogData {
    bookingId: number;
    booking: DetailedBooking;
}

@Component({
    selector: 'tinn-booking-edit-dialog',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatDialogModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        MatDatepickerModule,
        MatNativeDateModule,
    ],
    templateUrl: './booking-edit-dialog.component.html',
})
export class BookingEditDialogComponent implements OnInit {
    private readonly fb = inject(FormBuilder);
    private readonly dialogRef = inject(MatDialogRef<BookingEditDialogComponent>);
    private readonly data = inject<BookingEditDialogData>(MAT_DIALOG_DATA);
    private readonly bookingService = inject(BookingService);
    private readonly bookingStore = inject(BookingStore);
    private readonly roomService = inject(RoomService);
    private readonly toastr = inject(ToastrService);

    public readonly loading = signal<boolean>(false);
    public readonly availableRooms = signal<AssignedRoom[]>([]);
    public readonly selectedRoomIds = signal<number[]>([]);

    public readonly bookingForm = this.fb.group({
        checkInDate: ['', [Validators.required]],
        checkOutDate: ['', [Validators.required]],
        numAdults: [1, [Validators.required, Validators.min(1)]],
        numChildren: [0, [Validators.required, Validators.min(0)]],
        firstName: ['', [Validators.required]],
        lastName: ['', [Validators.required]],
        dateOfBirth: ['', [Validators.required]],
        email: ['', [Validators.required, Validators.email]],
        phone: [''],
        earlyCheckIn: [false],
        lateCheckOut: [false],
    });

    public get minCheckInDate(): Date {
        return new Date();
    }

    public get minCheckOutDate(): Date {
        const checkInDate = this.bookingForm.get('checkInDate')?.value;
        if (checkInDate) {
            const minDate = new Date(checkInDate);
            minDate.setDate(minDate.getDate() + 1);
            return minDate;
        }
        return new Date();
    }

    public async ngOnInit(): Promise<void> {
        this.patchFormWithBookingData();
        await this.loadAvailableRooms();
    }

    public async onDateChange(): Promise<void> {
        await this.loadAvailableRooms();
    }

    public toggleRoomSelection(roomId: number): void {
        const currentSelection = this.selectedRoomIds();
        if (currentSelection.includes(roomId)) {
            this.selectedRoomIds.set(currentSelection.filter((id) => id !== roomId));
        } else {
            this.selectedRoomIds.set([...currentSelection, roomId]);
        }
    }

    public isRoomSelected(roomId: number): boolean {
        return this.selectedRoomIds().includes(roomId);
    }

    public get totalGuests(): number {
        const numAdults = this.bookingForm.get('numAdults')?.value || 0;
        const numChildren = this.bookingForm.get('numChildren')?.value || 0;
        return numAdults + numChildren;
    }

    public get totalCapacity(): number {
        const selectedIds = this.selectedRoomIds();
        const selectedRooms = this.availableRooms().filter((room) => selectedIds.includes(room.id));
        return selectedRooms.reduce((sum, room) => sum + room.capacity, 0);
    }

    public get hasInsufficientCapacity(): boolean {
        return this.totalCapacity < this.totalGuests && this.selectedRoomIds().length > 0;
    }

    public async onSubmit(): Promise<void> {
        if (this.bookingForm.invalid) {
            this.toastr.error('Please fill all required fields correctly');
            return;
        }

        if (this.selectedRoomIds().length === 0) {
            this.toastr.error('Please select at least one room');
            return;
        }

        this.loading.set(true);

        const formValue = this.bookingForm.value;

        const guestInfo: GuestInfo = {
            firstName: formValue.firstName!,
            lastName: formValue.lastName!,
            dateOfBirth: formValue.dateOfBirth!,
            email: formValue.email!,
            phone: formValue.phone || '',
        };

        const request: CreateOrUpdateBookingRequest = {
            checkInDate: this.formatDate(formValue.checkInDate!),
            checkOutDate: this.formatDate(formValue.checkOutDate!),
            numAdults: formValue.numAdults!,
            numChildren: formValue.numChildren!,
            guestInfo: guestInfo,
            roomIds: this.selectedRoomIds(),
            earlyCheckIn: formValue.earlyCheckIn || false,
            lateCheckOut: formValue.lateCheckOut || false,
        };

        await this.bookingService.updateBooking(this.data.bookingId, request);

        this.loading.set(false);

        if (this.bookingStore.error()) {
            const error = this.bookingStore.error();
            const errorMessage = error?.validationErrors?.[0]?.message || error?.message || 'Failed to update booking';
            this.toastr.error(errorMessage);
        } else {
            this.dialogRef.close(true);
        }
    }

    public onCancel(): void {
        this.dialogRef.close(false);
    }

    private patchFormWithBookingData(): void {
        const booking = this.data.booking;
        const guest = booking.primaryGuest;

        this.bookingForm.patchValue({
            checkInDate: this.parseBackendDate(booking.checkInDate),
            checkOutDate: this.parseBackendDate(booking.checkOutDate),
            numAdults: booking.numAdults,
            numChildren: booking.numChildren,
            firstName: guest.firstName,
            lastName: guest.lastName,
            dateOfBirth: this.parseBackendDate(guest.dateOfBirth),
            email: guest.email,
            phone: guest.phone || '',
            earlyCheckIn: booking.earlyCheckIn || false,
            lateCheckOut: booking.lateCheckOut || false,
        });

        this.selectedRoomIds.set(booking.rooms.map((room) => room.id));
    }

    private async loadAvailableRooms(): Promise<void> {
        const checkInDate = this.bookingForm.get('checkInDate')?.value;
        const checkOutDate = this.bookingForm.get('checkOutDate')?.value;

        if (!checkInDate || !checkOutDate) {
            return;
        }

        const rooms = await this.roomService.getAvailableRooms(
            this.formatDate(checkInDate),
            this.formatDate(checkOutDate),
        );

        if (this.roomService.error()) {
            const error = this.roomService.error();
            const errorMessage =
                error?.validationErrors?.[0]?.message || error?.message || 'Failed to load available rooms';
            this.toastr.error(errorMessage);
            return;
        }

        const currentRoomIds = this.data.booking.rooms.map((r) => r.id);
        const currentRooms: AssignedRoom[] = this.data.booking.rooms.map((r) => ({
            id: r.id,
            roomNumber: r.roomNumber,
            roomTypeName: r.roomTypeName,
            capacity: r.capacity,
        }));

        const availableRooms: AssignedRoom[] = rooms
            .filter((r) => !currentRoomIds.includes(r.id))
            .map((r) => ({
                id: r.id,
                roomNumber: r.roomNumber,
                roomTypeName: r.roomType.name,
                capacity: r.effectiveCapacity,
            }));

        const allRooms: AssignedRoom[] = [...currentRooms, ...availableRooms];

        this.availableRooms.set(allRooms);
    }

    private parseBackendDate(dateStr: string): string {
        if (dateStr.match(/^\d{2}-\d{2}-\d{4}$/)) {
            const [day, month, year] = dateStr.split('-');
            return `${year}-${month}-${day}`;
        } else if (dateStr.match(/^\d{4}-\d{2}-\d{2}$/)) {
            return dateStr;
        } else {
            const date = new Date(dateStr);
            return this.toDateInputValue(date);
        }
    }

    /**
     * Converts a Date object to yyyy-MM-dd string format for date input fields
     */
    private toDateInputValue(date: Date): string {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    /**
     * Formats a date string (from yyyy-MM-dd) to dd-MM-yyyy for the backend API
     * Parses the string directly to avoid timezone issues
     */
    private formatDate(dateStr: string): string {
        const [year, month, day] = dateStr.split('-');
        return `${day}-${month}-${year}`;
    }
}
