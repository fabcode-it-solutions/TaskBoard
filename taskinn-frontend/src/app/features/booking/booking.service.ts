import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BookingStore } from './booking.store';
import {
    BookingSummary,
    CreateOrUpdateBookingRequest,
    CreateBookingResponse,
    DetailedBooking,
    RoomOptionsRequest,
    RoomTypeOption,
    StripeSessionResponse,
    SpecialBookingConstraints,
} from './interfaces/room';
import { processError } from '../../shared/util/error.utils';
import { ActivatedRoute, Router } from '@angular/router';
import { formatDate } from '@angular/common';
import { InvoicePreview } from './interfaces/invoice';

@Injectable({ providedIn: 'root' })
export class BookingService {
    private readonly http = inject(HttpClient);
    private readonly bookingStore = inject(BookingStore);

    private readonly bookingPath = `${environment.apiUrl}/booking`;

    /**
     * Begins a booking process by creating a Stripe session.
     *
     * Invoked by Customers, creates a booking with the status PENDING
     */
    public async beginBooking(request: CreateOrUpdateBookingRequest): Promise<StripeSessionResponse | null> {
        try {
            this.bookingStore.setLoading(true);
            this.bookingStore.setError(null);

            const response = await firstValueFrom(
                this.http.post<StripeSessionResponse>(`${this.bookingPath}/begin-booking`, request),
            );
            return response;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to create Stripe session', `${this.bookingPath}`);
            this.bookingStore.setError(errorResponse);
            return null;
        } finally {
            this.bookingStore.setLoading(false);
        }
    }

    /**
     * Confirms a booking by changing the status to CONFIRMED
     *
     * Invoked by Customers, changes the status of the booking to CONFIRMED
     */
    public async confirmBooking(bookingId: number): Promise<void> {
        try {
            this.bookingStore.setLoading(true);
            this.bookingStore.setError(null);

            await firstValueFrom(this.http.post<void>(`${this.bookingPath}/confirm-booking`, bookingId));
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to confirm booking',
                `${this.bookingPath}/confirm-booking`,
            );
            this.bookingStore.setError(errorResponse);
        } finally {
            this.bookingStore.setLoading(false);
        }
    }

    /**
     * Cancels a booking by changing the status to CANCELLED
     *
     * Invoked by Customers, changes the status of the booking to CANCELLED
     */
    public async cancelBooking(bookingId: number): Promise<void> {
        try {
            this.bookingStore.setLoading(true);
            this.bookingStore.setError(null);

            await firstValueFrom(this.http.post<void>(`${this.bookingPath}/cancel-booking`, bookingId));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to cancel booking', `${this.bookingPath}/cancel-booking`);
            this.bookingStore.setError(errorResponse);
        } finally {
            this.bookingStore.setLoading(false);
        }
    }

    /**
     * Fetch all bookings for the overview page.
     */
    public async fetchAllBookings(): Promise<BookingSummary[]> {
        try {
            this.bookingStore.setLoading(true);
            this.bookingStore.setError(null);
            this.bookingStore.setAllBookings([]);

            const response = await firstValueFrom(this.http.get<BookingSummary[]>(`${this.bookingPath}`));

            this.bookingStore.setAllBookings(response);
            return response;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to fetch bookings', this.bookingPath);
            this.bookingStore.setError(errorResponse);
            return [];
        } finally {
            this.bookingStore.setLoading(false);
        }
    }

    /**
     * Fetch the detailed information for a single booking by its ID.
     */
    public async fetchBookingDetails(bookingId: number): Promise<DetailedBooking | null> {
        try {
            this.bookingStore.setLoading(true);
            this.bookingStore.setError(null);
            this.bookingStore.setSelectedBooking(null);

            const url = `${this.bookingPath}/${bookingId}`;
            const response = await firstValueFrom(this.http.get<DetailedBooking>(url));

            this.bookingStore.setSelectedBooking(response);
            return response;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to fetch details for booking #${bookingId}`,
                `${this.bookingPath}/${bookingId}`,
            );
            this.bookingStore.setError(errorResponse);
            return null;
        } finally {
            this.bookingStore.setLoading(false);
        }
    }

    /**
     * Fetch a booking response by Stripe session id (success page).
     */
    public async fetchBookingResponseBySessionId(sessionId: string): Promise<CreateBookingResponse | null> {
        try {
            this.bookingStore.setLoading(true);
            this.bookingStore.setError(null);

            const response = await firstValueFrom(
                this.http.get<CreateBookingResponse>(`${this.bookingPath}/checkout-session`, {
                    params: { session_id: sessionId },
                }),
            );
            return response;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to fetch booking details from Stripe session',
                `${this.bookingPath}/checkout-session`,
            );
            this.bookingStore.setError(errorResponse);
            return null;
        } finally {
            this.bookingStore.setLoading(false);
        }
    }

    /**
     * Fetch available room type options based on the booking criteria
     */
    public async fetchRoomOptions(request: RoomOptionsRequest): Promise<RoomTypeOption[]> {
        try {
            this.bookingStore.setLoading(true);
            this.bookingStore.setError(null);
            this.bookingStore.setRoomOptions([]);

            const response = await firstValueFrom(
                this.http.post<RoomTypeOption[]>(`${this.bookingPath}/room-options`, request),
            );
            this.bookingStore.setRoomOptions(response);
            return response;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to fetch room options',
                `${this.bookingPath}/room-options`,
            );
            this.bookingStore.setError(errorResponse);
            return [];
        } finally {
            this.bookingStore.setLoading(false);
        }
    }

    /**
     * Creates a new booking with the provided details
     *
     * Invoked by Staff, creates a new booking with the status CONFIRMED
     */
    public async createBooking(request: CreateOrUpdateBookingRequest): Promise<CreateBookingResponse | null> {
        try {
            this.bookingStore.setLoading(true);
            this.bookingStore.setError(null);

            const response = await firstValueFrom(
                this.http.post<CreateBookingResponse>(`${this.bookingPath}/create-booking`, request),
            );
            return response;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to create booking', `${this.bookingPath}`);
            this.bookingStore.setError(errorResponse);
            return null;
        } finally {
            this.bookingStore.setLoading(false);
        }
    }

    /**
     * Update an existing booking with the provided details
     */
    public async updateBooking(bookingId: number, request: CreateOrUpdateBookingRequest): Promise<void> {
        try {
            this.bookingStore.setLoading(true);
            this.bookingStore.setError(null);

            await firstValueFrom(this.http.put<void>(`${this.bookingPath}?bookingId=${bookingId}`, request));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to update booking', `${this.bookingPath}`);
            this.bookingStore.setError(errorResponse);
        } finally {
            this.bookingStore.setLoading(false);
        }
    }

    public async getInvoice(bookingId: number): Promise<InvoicePreview> {
        try {
            return await firstValueFrom(this.http.get<InvoicePreview>(`${environment.apiUrl}/invoice/${bookingId}`));
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to fetch invoice for booking #${bookingId}`,
                `${environment.apiUrl}/invoice/${bookingId}`,
            );
            this.bookingStore.setError(errorResponse);
            throw errorResponse;
        }
    }

    /**
     * Fetches the count of check-ins for today
     * @returns Number of check-ins today
     */
    public async getTodaysCheckInCount(): Promise<number | null> {
        try {
            this.bookingStore.setLoading(true);
            this.bookingStore.setError(null);

            const count = await firstValueFrom(this.http.get<number>(`${this.bookingPath}/checkins/today/count`));
            return count;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                "Failed to fetch today's check-in count",
                `${this.bookingPath}/checkins/today/count`,
            );
            this.bookingStore.setError(errorResponse);
            return null;
        } finally {
            this.bookingStore.setLoading(false);
        }
    }

    /**
     * Fetches the count of check-outs for today
     * @returns Number of check-outs today
     */
    public async getTodaysCheckOutCount(): Promise<number | null> {
        try {
            this.bookingStore.setLoading(true);
            this.bookingStore.setError(null);

            const count = await firstValueFrom(this.http.get<number>(`${this.bookingPath}/checkouts/today/count`));
            return count;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                "Failed to fetch today's check-out count",
                `${this.bookingPath}/checkouts/today/count`,
            );
            this.bookingStore.setError(errorResponse);
            return null;
        } finally {
            this.bookingStore.setLoading(false);
        }
    }

    public async getSpecialBookingConstraints(
        year: number,
        month: number,
        day: number,
    ): Promise<SpecialBookingConstraints | null> {
        const url = `${this.bookingPath}/constrained-rooms/${year}/${month}/${day}`;
        try {
            this.bookingStore.setLoading(true);
            this.bookingStore.setError(null);

            const result = await firstValueFrom(this.http.get<SpecialBookingConstraints>(url));
            return result;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to fetch special booking constraints', url);
            this.bookingStore.setError(errorResponse);
            return null;
        } finally {
            this.bookingStore.setLoading(false);
        }
    }
}

export function extractQueryParamsAndUpdateStore(route: ActivatedRoute, bookingStore: BookingStore): void {
    const checkIn = route.snapshot.queryParams['checkIn'];
    const checkOut = route.snapshot.queryParams['checkOut'];
    const numRooms = route.snapshot.queryParams['numRooms'];
    const numAdults = route.snapshot.queryParams['numAdults'];
    const numChildren = route.snapshot.queryParams['numChildren'];

    if (checkIn) {
        bookingStore.setRequest({
            ...bookingStore.request(),
            checkInDate: checkIn,
        });
    }
    if (checkOut) {
        bookingStore.setRequest({
            ...bookingStore.request(),
            checkOutDate: checkOut,
        });
    }
    if (numRooms) {
        bookingStore.setRequest({
            ...bookingStore.request(),
            numRooms: numRooms,
        });
    }
    if (numAdults) {
        bookingStore.setRequest({
            ...bookingStore.request(),
            numAdults: numAdults,
        });
    }
    if (numChildren) {
        bookingStore.setRequest({
            ...bookingStore.request(),
            numChildren: numChildren,
        });
    }
}

export function navigateWithQueryParams(path: string, router: Router, bookingStore: BookingStore): void {
    router.navigate([path], {
        queryParams: {
            checkIn: bookingStore.request().checkInDate,
            checkOut: bookingStore.request().checkOutDate,
            numRooms: bookingStore.request().numRooms,
            numAdults: bookingStore.request().numAdults,
            numChildren: bookingStore.request().numChildren,
        },
    });
}

/**
 * Converts a Date object or date string to dd-MM-yyyy format
 */
export function formatDateForBackend(date: Date | string): string {
    const d = typeof date === 'string' ? new Date(date) : date;
    return formatDate(d, 'dd-MM-yyyy', 'en-US');
}
