export interface CheckInGuest {
    roomId: number;
    firstName: string;
    lastName: string;
    dateOfBirth: string; // format: yyyy-MM-dd
    email: string;
    phoneNumber: string;
}

export interface CheckInRequest {
    bookingId: number;
    guests: CheckInGuest[];
}

export interface CheckInGuestForm extends CheckInGuest {
    idImageBase64: string;
    idImageFile: File | null;
}

export interface CheckInForm {
    bookingId: number;
    guests: CheckInGuestForm[];
}

export interface CheckInPayload {
    guests: CheckInGuest[];
    files: File[];
}

export interface PostCheckInResponse {
    status?: number;
    error?: string;
    message?: string;
}

export interface BookerInfo {
    firstName: string;
    lastName: string;
    dateOfBirth: string;
    email: string;
    phone: string;
    numAdults: number;
    numChildren: number;
}
