export interface ValidationError {
    field: string;
    message: string;
}

export interface ErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
    validationErrors?: ValidationError[];
}
