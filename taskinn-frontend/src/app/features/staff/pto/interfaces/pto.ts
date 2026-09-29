export interface Pto {
    id: number;
    startDate: string;
    endDate: string;
}

export interface CreatePtoRequest {
    startDate: string;
    endDate: string;
}
