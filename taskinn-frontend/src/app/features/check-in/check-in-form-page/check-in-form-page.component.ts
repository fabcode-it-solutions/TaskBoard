import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CheckInStore } from '../check-in.store';
import { CheckInService } from '../check-in.service';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { CheckInGuest, CheckInGuestForm, CheckInPayload } from '../interfaces/check-in.request';
import { MandatoryMarkComponent } from '../../../shared/components/mandatory-mark/mandatory-mark.component';
import { ToastrService } from 'ngx-toastr';
import { NgClass } from '@angular/common';
import { Subscription } from 'rxjs';

@Component({
    selector: 'tinn-check-in-form-page',
    imports: [MatIcon, PageHeaderComponent, ReactiveFormsModule, MandatoryMarkComponent, NgClass],
    templateUrl: './check-in-form-page.component.html',
})
export class CheckInFormPageComponent implements OnInit, OnDestroy {
    private readonly fb = inject(FormBuilder);
    private readonly store = inject(CheckInStore);
    private readonly service = inject(CheckInService);
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly toastr = inject(ToastrService);

    public readonly loading = this.store.loading;
    public readonly error = this.store.error;

    public readonly rooms = this.store.rooms;
    public readonly bookingId = this.store.bookingId;

    public guestDropdownOptions: { label: string; value: number }[] = [];
    public selectedGuestIndex = 0;

    public guestData: CheckInGuestForm[] = [];
    public guestForms: FormGroup[] = [];
    private subscriptions: Subscription[] = [];

    public formSubmitted = false;
    public submissionError: string | null = null;
    public criticalError: string | null = null;
    public isInitialized = false;

    public fileErrors: Record<number, string> = {};

    async ngOnInit(): Promise<void> {
        const bookingIdParam = this.route.snapshot.paramMap.get('bookingId');

        if (bookingIdParam) {
            const bookingId = Number(bookingIdParam);
            const success = await this.service.loadCheckInData(bookingId);

            if (!success) {
                this.criticalError = this.store.error()?.message || 'Failed to load check-in data.';
                return;
            }

            this.initGuestDataAndForms();
        } else {
            // Fall back to store if already loaded
            if (!this.store.bookingId() || !this.store.rooms() || this.store.rooms()!.length === 0) {
                this.criticalError = 'Booking information is missing, please restart the check-in process.';
                return;
            }

            this.initGuestDataAndForms();
        }

        this.setGuestDropdownOptions();
        this.isInitialized = true;
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach((sub) => sub.unsubscribe());
    }

    public onGuestChange(guestIndex: string): void {
        if (guestIndex == null) {
            return;
        }
        this.selectedGuestIndex = Number(guestIndex);
        this.fileErrors[this.selectedGuestIndex] = '';
    }

    public onFileChange(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (!input.files || input.files.length === 0) {
            this.fileErrors[this.selectedGuestIndex] = 'ID Image is required.';
            this.guestData[this.selectedGuestIndex].idImageBase64 = '';
            this.guestData[this.selectedGuestIndex].idImageFile = null;
            return;
        }

        const file = input.files[0];
        if (file.size > 10 * 1024 * 1024) {
            this.fileErrors[this.selectedGuestIndex] = 'File size must be less than 10 MB.';
            return;
        }
        if (!file.type.startsWith('image/')) {
            this.fileErrors[this.selectedGuestIndex] = 'Only image files are allowed.';
            return;
        }

        this.fileErrors[this.selectedGuestIndex] = '';
        this.guestData[this.selectedGuestIndex].idImageFile = file;

        this.convertFileToBase64(file, (base64: string | null) => {
            if (base64) {
                this.guestData[this.selectedGuestIndex].idImageBase64 = base64;
            } else {
                this.fileErrors[this.selectedGuestIndex] = 'Failed to read file';
            }
        });
    }

    public async onSubmit(): Promise<void> {
        if (!this.bookingId() || !this.guestData || !this.rooms() || this.rooms()!.length === 0) {
            this.submissionError = 'Form submission is disabled due to missing booking data.';
            return;
        }

        if (!this.formIsValid()) {
            this.toastr.error('Please ensure all guests are filled out with an ID image.');
            return;
        }

        const filteredGuests = this.guestData.filter(
            (guest) =>
                guest.firstName.trim().length > 0 &&
                guest.lastName.trim().length > 0 &&
                guest.dateOfBirth.trim().length > 0 &&
                guest.idImageBase64.trim().length > 0,
        );

        const guests: CheckInGuest[] = filteredGuests.map((guest) => ({
            roomId: guest.roomId,
            firstName: guest.firstName,
            lastName: guest.lastName,
            dateOfBirth: guest.dateOfBirth,
            email: guest.email,
            phoneNumber: guest.phoneNumber,
        }));

        const files: File[] = filteredGuests.map((guest) => guest.idImageFile!);

        const payload: CheckInPayload = {
            guests,
            files,
        };

        const success = await this.service.postCheckIn(this.bookingId()!, payload);

        if (success) {
            this.formSubmitted = true;
            this.submissionError = null;
            this.toastr.success('Check-in submitted successfully.');
            this.router.navigate(['/staff/dashboard']);
        } else {
            const error = this.store.error();
            let errorMessage = error?.message || 'An error occurred';

            if (error?.validationErrors && error.validationErrors.length > 0) {
                const validationMessages = error.validationErrors.map((ve) => ve.message).join('; ');
                errorMessage = validationMessages;
            }

            this.submissionError = errorMessage;
            this.toastr.error(errorMessage);
        }
    }

    public get currentGuestForm(): FormGroup {
        return this.guestForms[this.selectedGuestIndex];
    }

    public onClickBack(): void {
        this.router.navigate(['checkin/initial']);
    }

    protected isFirstGuest(): boolean {
        return this.selectedGuestIndex === 0;
    }

    protected truncateFilename(filename: string, maxLength: number = 30): string {
        if (!filename || filename.length <= maxLength) {
            return filename;
        }

        const extension = this.getFileExtension(filename);
        const nameWithoutExt = filename.substring(0, filename.length - extension.length);
        const truncatedLength = maxLength - extension.length - 3;

        if (truncatedLength > 0) {
            return nameWithoutExt.substring(0, truncatedLength) + '...' + extension;
        }

        return filename.substring(0, maxLength) + '...';
    }

    protected formIsValid(): boolean {
        const rooms = this.rooms();
        if (!rooms || !this.guestData || this.guestData.length === 0 || this.guestForms.length === 0) {
            return false;
        }

        const allFormsValid = this.guestForms.every((form) => form.valid);
        if (!allFormsValid) {
            return false;
        }

        const allGuestsHaveIdImage = this.guestData.every(
            (guest) => guest.idImageBase64 && guest.idImageBase64.trim().length > 0,
        );

        return allGuestsHaveIdImage;
    }

    private initGuestDataAndForms(): void {
        const rooms = this.store.rooms();
        const bookerInfo = this.store.bookerInfo();

        if (!rooms) {
            return;
        }

        const totalGuests = bookerInfo ? bookerInfo.numAdults + bookerInfo.numChildren : rooms.length;
        const roomAssignments = this.distributeGuestsToRooms(totalGuests, rooms);

        let guestIndex = 0;
        for (const assignment of roomAssignments) {
            const room = rooms.find((r) => r.roomId === assignment.roomId);
            if (!room) {
                continue;
            }

            for (let i = 0; i < assignment.guestCount; i++) {
                const isFirstGuest = guestIndex === 0;
                const currentIndex = guestIndex;

                const guestData: CheckInGuestForm = {
                    roomId: room.roomId,
                    firstName: isFirstGuest && bookerInfo?.firstName ? bookerInfo.firstName : '',
                    lastName: isFirstGuest && bookerInfo?.lastName ? bookerInfo.lastName : '',
                    dateOfBirth: isFirstGuest && bookerInfo?.dateOfBirth ? bookerInfo.dateOfBirth : '',
                    email: isFirstGuest && bookerInfo?.email ? bookerInfo.email : '',
                    phoneNumber: isFirstGuest && bookerInfo?.phone ? bookerInfo.phone : '',
                    idImageBase64: '',
                    idImageFile: null,
                };
                this.guestData.push(guestData);

                const form = this.createGuestFormGroup(isFirstGuest, guestData);
                this.guestForms.push(form);

                const sub = form.valueChanges.subscribe((value) => {
                    this.syncFormToGuestData(currentIndex, value, isFirstGuest);
                    this.setGuestDropdownOptions();
                });
                this.subscriptions.push(sub);

                guestIndex++;
            }
        }
    }

    private distributeGuestsToRooms(
        totalGuests: number,
        rooms: { roomId: number; roomNumber: string; capacity: number }[],
    ): { roomId: number; guestCount: number }[] {
        if (!rooms || rooms.length === 0) {
            return [];
        }

        const assignments: { roomId: number; guestCount: number; capacity: number }[] = rooms.map((room) => ({
            roomId: room.roomId,
            guestCount: 0,
            capacity: room.capacity,
        }));

        let remainingGuests = totalGuests;

        for (const assignment of assignments) {
            if (remainingGuests > 0) {
                assignment.guestCount = 1;
                remainingGuests--;
            }
        }

        for (const assignment of assignments) {
            const additionalCapacity = assignment.capacity - assignment.guestCount;
            const guestsToAdd = Math.min(additionalCapacity, remainingGuests);
            assignment.guestCount += guestsToAdd;
            remainingGuests -= guestsToAdd;

            if (remainingGuests === 0) {
                break;
            }
        }

        return assignments;
    }

    private createGuestFormGroup(isFirstGuest: boolean, initialData: CheckInGuestForm): FormGroup {
        if (isFirstGuest) {
            return this.fb.group({
                firstName: [initialData.firstName, Validators.required],
                lastName: [initialData.lastName, Validators.required],
                dateOfBirth: [initialData.dateOfBirth, Validators.required],
                email: [initialData.email, [Validators.required, Validators.email]],
                phoneNumber: [initialData.phoneNumber, Validators.required],
            });
        } else {
            return this.fb.group({
                firstName: [initialData.firstName, Validators.required],
                lastName: [initialData.lastName, Validators.required],
                dateOfBirth: [initialData.dateOfBirth, Validators.required],
            });
        }
    }

    private syncFormToGuestData(index: number, value: Record<string, string>, isFirstGuest: boolean): void {
        if (isFirstGuest) {
            this.guestData[index].firstName = value['firstName'] || '';
            this.guestData[index].lastName = value['lastName'] || '';
            this.guestData[index].dateOfBirth = value['dateOfBirth'] || '';
            this.guestData[index].email = value['email'] || '';
            this.guestData[index].phoneNumber = value['phoneNumber'] || '';
        } else {
            this.guestData[index].firstName = value['firstName'] || '';
            this.guestData[index].lastName = value['lastName'] || '';
            this.guestData[index].dateOfBirth = value['dateOfBirth'] || '';
        }
    }

    private setGuestDropdownOptions(): void {
        const rooms = this.rooms();
        this.guestDropdownOptions = this.guestData.map((guest, index) => {
            const room = rooms?.find((r) => r.roomId === guest.roomId);
            const roomNumber = room ? (room.roomNumber ?? guest.roomId) : guest.roomId;

            const guestName =
                guest.firstName && guest.firstName.trim().length > 0 ? guest.firstName : `Guest ${index + 1}`;

            return {
                label: `${guestName} (Room ${roomNumber})`,
                value: index,
            };
        });
    }

    private convertFileToBase64(file: File, callback: (base64: string | null) => void): void {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = (): void => {
            if (typeof reader.result === 'string') {
                const base64Str = reader.result.split(',')[1];
                callback(base64Str);
            } else {
                callback(null);
            }
        };
        reader.onerror = (): void => {
            callback(null);
        };
    }

    private getFileExtension(filename: string): string {
        const idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : '';
    }
}
