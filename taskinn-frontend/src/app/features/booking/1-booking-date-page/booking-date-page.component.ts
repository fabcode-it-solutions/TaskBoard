import { Component, computed, inject, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { BookingStore } from '../booking.store';
import { formatDateForBackend } from '../booking.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { DateRangeCalendarComponent } from '../../../shared/components/date-range-calendar/date-range-calendar.component';
import { extractQueryParamsAndUpdateStore } from '../booking.service';
import { navigateWithQueryParams } from '../booking.service';
import { AuthStore } from '../../auth/auth.store';

@Component({
    selector: 'tinn-booking-date-page',
    imports: [MatIcon, PageHeaderComponent, DateRangeCalendarComponent],
    templateUrl: './booking-date-page.component.html',
})
export class BookingDatePageComponent implements OnInit {
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly bookingStore = inject(BookingStore);
    private readonly authStore = inject(AuthStore);

    public readonly request = this.bookingStore.request;
    public readonly isRequestValid = this.bookingStore.isRequestValid;

    public readonly isStaff = computed(() => {
        const userProfile = this.authStore.userProfile();
        return userProfile?.roles.includes('EMPLOYEE') || userProfile?.roles.includes('ADMIN') || false;
    });

    public readonly hasDateError = computed(() => {
        const req = this.request();
        return req.checkInDate !== '' && req.checkOutDate !== null && req.checkInDate === req.checkOutDate;
    });

    public minDate: Date = new Date();

    public ngOnInit(): void {
        const hasQueryParams = Object.keys(this.route.snapshot.queryParams).length > 0;

        if (!hasQueryParams) {
            this.bookingStore.resetAll();
        } else {
            extractQueryParamsAndUpdateStore(this.route, this.bookingStore);
            if (!this.isRequestValid()) {
                this.bookingStore.resetRequest();
            }
        }

        this.minDate.setHours(0, 0, 0, 0);
    }

    public onCheckInChange(date: Date | null): void {
        this.bookingStore.setRequest({
            ...this.request(),
            checkInDate: date ? formatDateForBackend(date) : '',
        });
    }

    public onCheckOutChange(date: Date | null): void {
        this.bookingStore.setRequest({
            ...this.request(),
            checkOutDate: date ? formatDateForBackend(date) : null,
        });
    }

    public goForwards(): void {
        if (this.isRequestValid()) {
            navigateWithQueryParams('/booking/guests', this.router, this.bookingStore);
        }
    }

    public cancelBooking(): void {
        this.bookingStore.resetAll();
        this.router.navigate(['/staff/dashboard']);
    }
}
