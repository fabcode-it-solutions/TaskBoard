import { Component, computed, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { BookingStore } from '../booking.store';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { MandatoryMarkComponent } from '../../../shared/components/mandatory-mark/mandatory-mark.component';
import { extractQueryParamsAndUpdateStore } from '../booking.service';
import { navigateWithQueryParams } from '../booking.service';
import { AuthStore } from '../../auth/auth.store';

@Component({
    selector: 'tinn-booking-guests-page',
    imports: [ReactiveFormsModule, MatIcon, PageHeaderComponent, MandatoryMarkComponent],
    templateUrl: './booking-guests-page.component.html',
})
export class BookingGuestsPageComponent implements OnInit {
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly fb = inject(FormBuilder);
    private readonly bookingStore = inject(BookingStore);
    private readonly authStore = inject(AuthStore);

    public readonly isStaff = computed(() => {
        const userProfile = this.authStore.userProfile();
        return userProfile?.roles.includes('EMPLOYEE') || userProfile?.roles.includes('ADMIN') || false;
    });

    public guestsForm = this.fb.group({
        numRooms: [
            this.bookingStore.request().numRooms,
            [Validators.required, Validators.min(1), this.maxRoomsValidator.bind(this)],
        ],
        numAdults: [this.bookingStore.request().numAdults, [Validators.required, Validators.min(1)]],
        numChildren: [this.bookingStore.request().numChildren, [Validators.required, Validators.min(0)]],
    });

    public ngOnInit(): void {
        extractQueryParamsAndUpdateStore(this.route, this.bookingStore);
        if (!this.bookingStore.isRequestValid()) {
            this.bookingStore.resetRequest();
            navigateWithQueryParams('/booking/date', this.router, this.bookingStore);
        } else {
            this.guestsForm.patchValue({
                numRooms: this.bookingStore.request().numRooms,
                numAdults: this.bookingStore.request().numAdults,
                numChildren: this.bookingStore.request().numChildren,
            });

            this.guestsForm.get('numAdults')?.valueChanges.subscribe(() => {
                this.guestsForm.get('numRooms')?.updateValueAndValidity({ emitEvent: false });
            });
            this.guestsForm.get('numChildren')?.valueChanges.subscribe(() => {
                this.guestsForm.get('numRooms')?.updateValueAndValidity({ emitEvent: false });
            });
        }
    }

    public goForwards(): void {
        if (this.guestsForm.valid) {
            this.bookingStore.setRequest({
                ...this.bookingStore.request(),
                numRooms: this.guestsForm.get('numRooms')?.value ?? 1,
                numAdults: this.guestsForm.get('numAdults')?.value ?? 1,
                numChildren: this.guestsForm.get('numChildren')?.value ?? 0,
            });
            navigateWithQueryParams('/booking/room-options', this.router, this.bookingStore);
        }
    }

    public goBack(): void {
        navigateWithQueryParams('/booking/date', this.router, this.bookingStore);
    }

    public cancelBooking(): void {
        this.bookingStore.resetRequest();
        this.router.navigate(['/staff/dashboard']);
    }

    public getRoomsError(): string {
        const numRoomsControl = this.guestsForm.get('numRooms');
        const error = numRoomsControl?.errors?.['tooManyRooms'] as { actual: number; maxAllowed: number } | null;
        if (error && error.maxAllowed > 0) {
            // Safety check
            return `Need at most ${error.maxAllowed} room${error.maxAllowed !== 1 ? 's' : ''} for ${error.maxAllowed} guest${error.maxAllowed !== 1 ? 's' : ''} (have ${error.actual})`;
        }
        return '';
    }

    private maxRoomsValidator(control: AbstractControl): ValidationErrors | null {
        if (!control.parent) {
            return null;
        }

        const numRooms = Number(control.value) || 0;
        const numAdults = Number(control.parent.get('numAdults')?.value) || 0;
        const numChildren = Number(control.parent.get('numChildren')?.value) || 0;
        const totalGuests = numAdults + numChildren;

        if (numRooms > totalGuests) {
            return { tooManyRooms: { actual: numRooms, maxAllowed: totalGuests } };
        }
        return null;
    }
}
