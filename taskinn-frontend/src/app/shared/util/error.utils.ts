import { HttpErrorResponse } from '@angular/common/http';
import { ErrorResponse } from '../interfaces/error';

/**
 * Processes an error and returns a standardized ErrorResponse object.
 * Handles both HttpErrorResponse and generic errors.
 *
 * @param error - The caught error (unknown type)
 * @param message - Fallback error message for non-HTTP errors
 * @param path - The API path where the error occurred
 * @returns ErrorResponse object that can be used by any service
 */
export function processError(error: unknown, message: string, path: string): ErrorResponse {
    if (error instanceof HttpErrorResponse) {
        return error.error;
    } else {
        return {
            timestamp: new Date().toISOString(),
            status: 500,
            error: 'Internal Error',
            message: message,
            path: path,
        };
    }
}
