import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ErrorResponse } from '../../shared/interfaces/error';
import { environment } from '../../../environments/environment';
import { processError } from '../../shared/util/error.utils';
import { firstValueFrom } from 'rxjs';
import { InvoicePreview } from '../booking/interfaces/invoice';

@Injectable({ providedIn: 'root' })
export class CheckOutService {
    private readonly http = inject(HttpClient);

    private readonly endpoint = `${environment.apiUrl}/booking`;

    public loading = signal(false);
    public error = signal<ErrorResponse | null>(null);

    public async getInvoicePreview(bookingId: number): Promise<InvoicePreview | null> {
        try {
            this.loading.set(true);
            this.error.set(null);
            return await firstValueFrom(this.http.get<InvoicePreview>(`${environment.apiUrl}/invoice/${bookingId}`));
        } catch (err) {
            const errorResponse = processError(
                err,
                'Failed to load invoice preview',
                `${this.endpoint}/${bookingId}/invoice/preview`,
            );
            this.error.set(errorResponse);
            return null;
        } finally {
            this.loading.set(false);
        }
    }

    public async createPayment(bookingId: number, amount: number): Promise<boolean> {
        try {
            this.loading.set(true);
            this.error.set(null);

            await firstValueFrom(this.http.post(`${environment.apiUrl}/invoice/payment/${bookingId}`, { amount }));
            return true;
        } catch (err) {
            const errorResponse = processError(
                err,
                'Failed to process payment',
                `${this.endpoint}/invoice/payment/${bookingId}`,
            );
            this.error.set(errorResponse);
            return false;
        } finally {
            this.loading.set(false);
        }
    }

    public async finalizeCheckOut(bookingId: number): Promise<boolean> {
        try {
            this.loading.set(true);
            this.error.set(null);
            await firstValueFrom(this.http.put(`${this.endpoint}/${bookingId}/checkout`, {}));
            return true;
        } catch (err) {
            const errorResponse = processError(
                err,
                'Failed to check-out booking',
                `${this.endpoint}/${bookingId}/checkout`,
            );
            this.error.set(errorResponse);
            return false;
        } finally {
            this.loading.set(false);
        }
    }

    public async canCheckOut(bookingId: number): Promise<boolean> {
        try {
            this.loading.set(true);
            this.error.set(null);
            const response = await firstValueFrom(this.http.get<boolean>(`${this.endpoint}/${bookingId}/can-checkout`));
            return response;
        } catch (err) {
            const errorResponse = processError(
                err,
                'Failed to verify check-out eligibility',
                `${this.endpoint}/${bookingId}/can-checkout`,
            );
            this.error.set(errorResponse);
            return false;
        } finally {
            this.loading.set(false);
        }
    }
}
