import { Component, inject, OnInit, signal } from '@angular/core';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { MatIcon } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { CreateBookingResponse } from '../interfaces/room';
import { BookingStore } from '../booking.store';
import { BookingService } from '../booking.service';

@Component({
    selector: 'tinn-booking-success-page',
    templateUrl: './booking-success-page.component.html',
    standalone: true,
    imports: [PageHeaderComponent, MatIcon],
})
export class BookingSuccessPageComponent implements OnInit {
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly bookingStore = inject(BookingStore);
    private readonly bookingService = inject(BookingService);

    public readonly bookingResponse = signal<CreateBookingResponse | null>(null);

    public async ngOnInit(): Promise<void> {
        const storedResponse = this.bookingStore.bookingResponse();
        if (storedResponse) {
            this.bookingResponse.set(storedResponse);
            return;
        }

        const sessionId = this.route.snapshot.queryParams['session_id'];
        if (!sessionId) {
            return;
        }

        const response = await this.bookingService.fetchBookingResponseBySessionId(sessionId);
        if (response) {
            this.bookingResponse.set(response);
            this.bookingStore.setBookingResponse(response);
        }
    }

    public startNewBooking(): void {
        this.bookingStore.resetAll();
        this.router.navigate(['/booking/date']);
    }
}
