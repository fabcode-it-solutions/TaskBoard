/**
 * Request DTO for room options. Dates should be in dd-MM-yyyy format.
 */
export interface RoomOptionsRequest {
    checkInDate: string; // Format: dd-MM-yyyy
    checkOutDate: string | null; // Format: dd-MM-yyyy
    numRooms: number;
    numAdults: number;
    numChildren: number;
}

export interface RoomOption {
    roomId: number;
    roomType: RoomType;
}

export interface RoomTypeOption {
    roomType: RoomType;
    pricePerNight: number;
    availableCount: number;
    recommendedCount: number;
    totalPricePerNight: number;
}

export interface RoomType {
    id: number;
    name: string;
    description: string;
    capacity: number;
    basePrice: number;
    singleBed: number;
    doubleBed: number;
}

/**
 * Guest information for booking
 */
export interface GuestInfo {
    firstName: string;
    lastName: string;
    dateOfBirth: string;
    email: string;
    phone?: string;
}

/**
 * Room type selection for booking (roomTypeId, pricePerNight and quantity)
 */
export interface RoomTypeSelection {
    roomTypeId: number;
    pricePerNight: number;
    quantity: number;
}

/**
 * Request DTO for creating a booking. Dates should be in dd-MM-yyyy format.
 */
export interface CreateOrUpdateBookingRequest {
    checkInDate: string; // Format: dd-MM-yyyy
    checkOutDate: string; // Format: dd-MM-yyyy
    numAdults: number;
    numChildren: number;
    guestInfo: GuestInfo;
    roomTypeSelections?: RoomTypeSelection[];
    roomIds?: number[];
    createdManually?: boolean;
    earlyCheckIn?: boolean;
    lateCheckOut?: boolean;
}

/**
 * Response DTO after creating a booking
 */
export interface CreateBookingResponse {
    bookingId: number;
    checkInDate: string; // Format: dd-MM-yyyy
    checkOutDate: string; // Format: dd-MM-yyyy
    assignedRoomNumbers: string[];
    status: string;
    guestEmail: string;
    earlyCheckIn?: boolean;
    lateCheckOut?: boolean;
}

export interface StripeSessionResponse {
    url: string;
}

export interface BookingSummary {
    id: number;
    guestName: string;
    checkInDate: string; // "yyyy-MM-dd"
    checkOutDate: string; // "yyyy-MM-dd"
    roomCount: number;
    status: BookingStatus;
}

export interface DetailedBooking {
    bookingId: number;
    status: BookingStatus;
    checkInDate: string;
    checkOutDate: string;
    numAdults: number;
    numChildren: number;
    totalPrice: number;
    primaryGuest: PrimaryGuest;
    rooms: AssignedRoom[];
    checkedInGuests?: CheckedInGuest[];
    createdAt: string;
    earlyCheckIn?: boolean;
    lateCheckOut?: boolean;
}

export enum BookingStatus {
    PENDING = 'PENDING',
    CONFIRMED = 'CONFIRMED',
    CHECKED_IN = 'CHECKED_IN',
    CHECKED_OUT = 'CHECKED_OUT',
    CANCELLED = 'CANCELLED',
}

export interface PrimaryGuest {
    id: number;
    firstName: string;
    lastName: string;
    dateOfBirth: string;
    email: string;
    phone: string;
}

export interface AssignedRoom {
    id: number;
    roomNumber: string;
    roomTypeName: string;
    capacity: number;
}

export interface CheckedInGuest {
    id: number;
    firstName: string;
    lastName: string;
    roomNumber?: string;
    roomId?: number;
}

export interface SpecialBookingConstraints {
    earlyCheckInRoomIds: number[];
    lateCheckOutRoomIds: number[];
}
