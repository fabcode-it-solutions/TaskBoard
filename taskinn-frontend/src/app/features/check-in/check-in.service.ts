import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { CheckInStore } from './check-in.store';
import { RoomForCheckInResponse } from './interfaces/room-for-check-in.response';
import { BookerInfo, CheckInPayload } from './interfaces/check-in.request';
import { firstValueFrom } from 'rxjs';
import { processError } from '../../shared/util/error.utils';
import { DetailedBooking } from '../booking/interfaces/room';

@Injectable({ providedIn: 'root' })
export class CheckInService {
    private readonly http = inject(HttpClient);
    private readonly bookingPath = `${environment.apiUrl}/booking`;
    private readonly store = inject(CheckInStore);

    public async loadCheckInData(bookingId: number): Promise<boolean> {
        try {
            this.store.setLoading(true);
            this.store.setError(null);

            // Fetch rooms and booker info in parallel
            const [rooms, bookerInfo] = await Promise.all([
                this.fetchRoomsForCheckinInternal(bookingId),
                this.fetchBookerInfoInternal(bookingId),
            ]);

            if (!rooms || rooms.length === 0) {
                this.store.setError({
                    timestamp: new Date().toISOString(),
                    status: 404,
                    error: 'Not Found',
                    message: 'Booking not found or has no rooms assigned.',
                    path: `${this.bookingPath}/${bookingId}`,
                });
                return false;
            }

            // Update store with fetched data
            this.store.setBookingId(bookingId);
            this.store.setRooms(rooms);
            this.store.setBookerInfo(bookerInfo);

            return true;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to load check-in data',
                `${this.bookingPath}/${bookingId}`,
            );
            this.store.setError(errorResponse);
            return false;
        } finally {
            this.store.setLoading(false);
        }
    }

    public async postCheckIn(bookingId: number, payload: CheckInPayload): Promise<boolean> {
        try {
            this.store.setLoading(true);
            this.store.setError(null);

            const formData = new FormData();
            const guestsBlob = new Blob([JSON.stringify(payload.guests)], { type: 'application/json' });

            formData.append('guests', guestsBlob);

            payload.files.forEach((file, index) => {
                formData.append(`guestFiles_${index}`, file, file.name);
            });

            await firstValueFrom(this.http.post<void>(`${this.bookingPath}/${bookingId}/checkin`, formData));
            return true;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to submit check-in information',
                `${this.bookingPath}/${bookingId}/checkin`,
            );
            this.store.setError(errorResponse);
            return false;
        } finally {
            this.store.setLoading(false);
        }
    }

    private async fetchRoomsForCheckinInternal(bookingId: number): Promise<RoomForCheckInResponse[]> {
        return await firstValueFrom(
            this.http.get<RoomForCheckInResponse[]>(`${this.bookingPath}/${bookingId}/checkin/rooms`),
        );
    }

    private async fetchBookerInfoInternal(bookingId: number): Promise<BookerInfo | null> {
        const response = await firstValueFrom(this.http.get<DetailedBooking>(`${this.bookingPath}/${bookingId}`));

        return {
            firstName: response.primaryGuest.firstName,
            lastName: response.primaryGuest.lastName,
            dateOfBirth: response.primaryGuest.dateOfBirth,
            email: response.primaryGuest.email,
            phone: response.primaryGuest.phone,
            numAdults: response.numAdults,
            numChildren: response.numChildren ?? 0,
        };
    }
}
