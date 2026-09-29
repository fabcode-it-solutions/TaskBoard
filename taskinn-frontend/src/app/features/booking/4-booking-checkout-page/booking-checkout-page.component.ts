import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import {
    FormBuilder,
    FormGroup,
    ReactiveFormsModule,
    Validators,
    AbstractControl,
    ValidationErrors,
} from '@angular/forms';
import { BookingStore } from '../booking.store';
import { BookingService, extractQueryParamsAndUpdateStore, navigateWithQueryParams } from '../booking.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { CreateOrUpdateBookingRequest, CreateBookingResponse, GuestInfo } from '../interfaces/room';
import { AuthStore } from '../../auth/auth.store';
import { RoomService } from '../../room/room.service';
import { LightRoomDto } from '../../room/room';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'tinn-booking-checkout-page',
    imports: [MatIcon, PageHeaderComponent, CommonModule, ReactiveFormsModule],
    templateUrl: './booking-checkout-page.component.html',
})
export class BookingCheckoutPageComponent implements OnInit {
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly fb = inject(FormBuilder);
    private readonly bookingService = inject(BookingService);
    private readonly roomService = inject(RoomService);
    private readonly bookingStore = inject(BookingStore);
    private readonly authStore = inject(AuthStore);
    private readonly toastr = inject(ToastrService);

    public readonly loading = this.bookingStore.loading;
    public readonly error = this.bookingStore.error;
    public readonly request = this.bookingStore.request;
    public readonly roomOptions = this.bookingStore.roomOptions;
    public readonly selectedRoomTypes = this.bookingStore.selectedRoomTypes;
    public readonly selectedRoomIds = this.bookingStore.selectedRoomIds;
    public readonly earlyCheckIn = this.bookingStore.earlyCheckIn;
    public readonly lateCheckOut = this.bookingStore.lateCheckOut;
    public readonly bookingConfirmed = signal(false);
    public readonly bookingResponse = signal<CreateBookingResponse | null>(null);
    public readonly earlyCheckInFeePerRoom = 20;
    public readonly lateCheckOutFeePerRoom = 20;

    // For manual booking - store available rooms
    public readonly availableRooms = signal<LightRoomDto[]>([]);

    public readonly isStaff = computed(() => {
        const userProfile = this.authStore.userProfile();
        return userProfile?.roles.includes('EMPLOYEE') || userProfile?.roles.includes('ADMIN') || false;
    });

    public guestInfoForm: FormGroup;

    public readonly bookingSummary = computed(() => {
        const req = this.request();

        // Calculate number of nights
        const checkIn = req.checkInDate ? new Date(req.checkInDate.split('-').reverse().join('-')) : null;
        const checkOut = req.checkOutDate ? new Date(req.checkOutDate.split('-').reverse().join('-')) : null;
        const nights =
            checkIn && checkOut ? Math.ceil((checkOut.getTime() - checkIn.getTime()) / (1000 * 60 * 60 * 24)) : 0;

        if (this.isStaff()) {
            // Manual booking - use selected room IDs
            const selectedIds = this.selectedRoomIds();
            const availableRoomsList = this.availableRooms();
            const selectedRoomsList = availableRoomsList.filter((room) => selectedIds.includes(room.id));

            const rooms = selectedRoomsList.map((room: LightRoomDto) => ({
                roomNumber: room.roomNumber,
                roomType: {
                    id: room.roomType.id,
                    name: room.roomType.name,
                    capacity: room.effectiveCapacity,
                    basePrice: room.effectiveBasePrice,
                },
                quantity: 1,
                pricePerNight: room.effectiveBasePrice,
                totalPrice: room.effectiveBasePrice,
            }));

            const totalPricePerNight = rooms.reduce((sum, r) => sum + r.totalPrice, 0);
            const numRooms = selectedRoomsList.length;
            const additionalFees =
                (this.earlyCheckIn() ? this.earlyCheckInFeePerRoom * numRooms : 0) +
                (this.lateCheckOut() ? this.lateCheckOutFeePerRoom * numRooms : 0);
            const totalPrice = totalPricePerNight * nights + additionalFees;

            return {
                rooms,
                totalRoomsSelected: selectedRoomsList.length,
                totalPricePerNight,
                nights,
                additionalFees,
                totalPrice,
            };
        } else {
            // Customer booking - use room type selections
            const selected = this.selectedRoomTypes();
            const options = this.roomOptions();

            const rooms = selected
                .map((selection) => {
                    const roomOption = options.find(
                        (opt) =>
                            opt.roomType.id === selection.roomTypeId && opt.pricePerNight === selection.pricePerNight,
                    );
                    if (roomOption) {
                        return {
                            roomNumber: undefined as string | undefined, // For type compatibility
                            roomType: roomOption.roomType,
                            quantity: selection.quantity,
                            pricePerNight: roomOption.pricePerNight,
                            totalPrice: roomOption.pricePerNight * selection.quantity,
                        };
                    }
                    return null;
                })
                .filter((item) => item !== null);

            const totalRoomsSelected = selected.reduce((sum, s) => sum + s.quantity, 0);
            const totalPricePerNight = rooms.reduce((sum, r) => sum + (r?.totalPrice || 0), 0);
            const additionalFees =
                (this.earlyCheckIn() ? this.earlyCheckInFeePerRoom * totalRoomsSelected : 0) +
                (this.lateCheckOut() ? this.lateCheckOutFeePerRoom * totalRoomsSelected : 0);
            const totalPrice = totalPricePerNight * nights + additionalFees;

            return {
                rooms,
                totalRoomsSelected,
                totalPricePerNight,
                nights,
                additionalFees,
                totalPrice,
            };
        }
    });

    public readonly earlyCheckInFee = computed(() => {
        const summary = this.bookingSummary();
        return this.earlyCheckIn() ? this.earlyCheckInFeePerRoom * summary.totalRoomsSelected : 0;
    });

    public readonly lateCheckOutFee = computed(() => {
        const summary = this.bookingSummary();
        return this.lateCheckOut() ? this.lateCheckOutFeePerRoom * summary.totalRoomsSelected : 0;
    });

    constructor() {
        this.guestInfoForm = this.fb.group({
            firstName: ['', [Validators.required, Validators.minLength(2)]],
            lastName: ['', [Validators.required, Validators.minLength(2)]],
            dateOfBirth: ['', [Validators.required, this.dateValidator.bind(this), this.ageValidator.bind(this)]],
            email: ['', [Validators.required, Validators.email]],
            phone: [''],
        });
    }

    public async ngOnInit(): Promise<void> {
        extractQueryParamsAndUpdateStore(this.route, this.bookingStore);

        // Validate that we have all necessary data
        const hasRoomSelection = this.isStaff()
            ? this.selectedRoomIds().length > 0
            : this.selectedRoomTypes().length > 0;

        if (!this.bookingStore.isRequestValid() || !hasRoomSelection) {
            this.router.navigate(['/booking/date']);
            return;
        }

        // For manual booking, fetch available rooms to get room details
        if (this.isStaff()) {
            const req = this.request();
            const rooms = await this.roomService.getAvailableRooms(req.checkInDate!, req.checkOutDate!);
            this.availableRooms.set(rooms);
        }

        // Pre-fill form if guest info exists
        const existingGuestInfo = this.bookingStore.guestInfo();
        if (existingGuestInfo) {
            this.guestInfoForm.patchValue(existingGuestInfo);
        }
    }

    public goBack(): void {
        // Save guest info before going back
        if (this.guestInfoForm.valid) {
            this.bookingStore.setGuestInfo(this.guestInfoForm.value as GuestInfo);
        }
        navigateWithQueryParams('/booking/room-options', this.router, this.bookingStore);
    }

    public async goForwards(): Promise<void> {
        const guestInfo: GuestInfo = this.guestInfoForm.value;
        this.bookingStore.setGuestInfo(guestInfo);

        const req = this.request();
        const bookingRequest: CreateOrUpdateBookingRequest = {
            checkInDate: req.checkInDate,
            checkOutDate: req.checkOutDate!,
            numAdults: req.numAdults,
            numChildren: req.numChildren,
            guestInfo,
            earlyCheckIn: this.earlyCheckIn(),
            lateCheckOut: this.lateCheckOut(),
        };

        if (this.isStaff()) {
            bookingRequest.roomIds = this.selectedRoomIds();
            bookingRequest.createdManually = true;

            const response = await this.bookingService.createBooking(bookingRequest);

            if (response) {
                this.bookingResponse.set(response);
                this.bookingStore.setBookingResponse(response);
                this.bookingConfirmed.set(true);
                this.router.navigate(['/booking/success']);
            } else {
                this.toastr.error('Failed to create booking', 'Error');
            }
        } else {
            bookingRequest.roomTypeSelections = this.selectedRoomTypes().map((s) => ({
                roomTypeId: s.roomTypeId,
                pricePerNight: s.pricePerNight,
                quantity: s.quantity,
            }));

            const response = await this.bookingService.beginBooking(bookingRequest);

            if (response?.url) {
                window.location.assign(response.url);
            }
        }
    }

    public getFieldError(fieldName: string): string {
        const field = this.guestInfoForm.get(fieldName);
        if (!field) {
            return '';
        }
        const fieldLabel = this.getFieldLabel(fieldName);
        if (field.hasError('invalidDate')) {
            return 'Date of birth is invalid';
        }
        if (field.hasError('required')) {
            return `${fieldLabel} is required`;
        }
        if (field?.hasError('email')) {
            return 'Please enter a valid email address';
        }
        if (field?.hasError('minlength')) {
            return `${fieldLabel} must be at least 2 characters`;
        }
        if (field?.hasError('underAge')) {
            return 'You must be at least 18 years old';
        }
        return '';
    }

    public onDateOfBirthInput(event: Event): void {
        const input = event.target as HTMLInputElement | null;
        const control = this.guestInfoForm.get('dateOfBirth');
        if (!input || !control) {
            return;
        }

        const hasBadInput = input.validity?.badInput || input.validity?.typeMismatch;

        if (hasBadInput || (input.value && !this.isValidDateString(input.value))) {
            this.setControlError(control, 'invalidDate');
            return;
        }

        this.clearControlError(control, 'invalidDate');
    }

    private getFieldLabel(fieldName: string): string {
        switch (fieldName) {
            case 'firstName':
                return 'First name';
            case 'lastName':
                return 'Last name';
            case 'dateOfBirth':
                return 'Date of birth';
            case 'email':
                return 'Email';
            case 'phone':
                return 'Phone number';
            default:
                return fieldName.charAt(0).toUpperCase() + fieldName.slice(1);
        }
    }

    private setControlError(control: AbstractControl, errorKey: string): void {
        const errors = control.errors ?? {};
        if (!errors[errorKey]) {
            control.setErrors({ ...errors, [errorKey]: true });
        }
    }

    private clearControlError(control: AbstractControl, errorKey: string): void {
        const errors = control.errors;
        if (!errors?.[errorKey]) {
            return;
        }
        const { [errorKey]: _, ...remaining } = errors;
        control.setErrors(Object.keys(remaining).length > 0 ? remaining : null);
    }

    private dateValidator(control: AbstractControl): ValidationErrors | null {
        if (!control.value) {
            return null;
        }
        return this.isValidDateString(control.value) ? null : { invalidDate: true };
    }

    private ageValidator(control: AbstractControl): ValidationErrors | null {
        if (!control.value) {
            return null;
        }

        if (!this.isValidDateString(control.value)) {
            return null;
        }

        const birthDate = new Date(control.value + 'T00:00:00');
        const today = new Date();
        let age = today.getFullYear() - birthDate.getFullYear();
        const monthDiff = today.getMonth() - birthDate.getMonth();

        if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
            age--;
        }

        return age < 18 ? { underAge: true } : null;
    }

    private isValidDateString(value: string): boolean {
        if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
            return false;
        }
        const [year, month, day] = value.split('-').map((part) => Number(part));
        const date = new Date(Date.UTC(year, month - 1, day));
        return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day;
    }
}
