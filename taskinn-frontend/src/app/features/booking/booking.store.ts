import { Injectable, signal, WritableSignal } from '@angular/core';
import { ErrorResponse } from '../../shared/interfaces/error';
import {
    BookingSummary,
    CreateBookingResponse,
    DetailedBooking,
    GuestInfo,
    RoomOptionsRequest,
    RoomTypeOption,
} from './interfaces/room';

export interface SelectedRoomType {
    roomTypeId: number;
    pricePerNight: number;
    quantity: number;
}

@Injectable({ providedIn: 'root' })
export class BookingStore {
    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);
    public request: WritableSignal<RoomOptionsRequest> = signal({
        checkInDate: '',
        checkOutDate: null,
        numAdults: 1,
        numChildren: 0,
        numRooms: 1,
    });
    public roomOptions: WritableSignal<RoomTypeOption[]> = signal([]);
    public selectedRoomTypes: WritableSignal<SelectedRoomType[]> = signal([]);
    public selectedRoomIds: WritableSignal<number[]> = signal([]);
    public guestInfo: WritableSignal<GuestInfo | null> = signal(null);
    public bookingResponse: WritableSignal<CreateBookingResponse | null> = signal(null);
    public earlyCheckIn: WritableSignal<boolean> = signal(false);
    public lateCheckOut: WritableSignal<boolean> = signal(false);

    public allBookings: WritableSignal<BookingSummary[]> = signal([]);
    public selectedBooking: WritableSignal<DetailedBooking | null> = signal(null);

    public setLoading(loading: boolean): void {
        this.loading.set(loading);
    }

    public setError(error: ErrorResponse | null): void {
        this.error.set(error);
    }

    public setRequest(request: RoomOptionsRequest): void {
        this.request.set(request);
    }

    public setRoomOptions(data: RoomTypeOption[]): void {
        this.roomOptions.set(data);
    }

    public setSelectedRoomTypes(data: SelectedRoomType[]): void {
        this.selectedRoomTypes.set(data);
    }

    public setSelectedRoomIds(data: number[]): void {
        this.selectedRoomIds.set(data);
    }

    public setGuestInfo(data: GuestInfo | null): void {
        this.guestInfo.set(data);
    }

    public setBookingResponse(response: CreateBookingResponse | null): void {
        this.bookingResponse.set(response);
    }

    public setAllBookings(bookings: BookingSummary[]): void {
        this.allBookings.set(bookings);
    }

    public setSelectedBooking(booking: DetailedBooking | null): void {
        this.selectedBooking.set(booking);
    }

    public setEarlyCheckIn(value: boolean): void {
        this.earlyCheckIn.set(value);
    }

    public setLateCheckOut(value: boolean): void {
        this.lateCheckOut.set(value);
    }

    public resetRequest(): void {
        this.request.set({
            checkInDate: '',
            checkOutDate: null,
            numAdults: 1,
            numChildren: 0,
            numRooms: 1,
        });
    }

    public resetAll(): void {
        this.resetRequest();
        this.selectedRoomTypes.set([]);
        this.selectedRoomIds.set([]);
        this.roomOptions.set([]);
        this.guestInfo.set(null);
        this.bookingResponse.set(null);
        this.earlyCheckIn.set(false);
        this.lateCheckOut.set(false);
        this.error.set(null);
    }

    public isRequestValid(): boolean {
        return (
            this.request().checkInDate !== '' &&
            this.request().checkOutDate !== null &&
            this.request().checkInDate !== this.request().checkOutDate &&
            this.request().numRooms >= 1 &&
            this.request().numAdults >= 1 &&
            this.request().numChildren >= 0
        );
    }
}
