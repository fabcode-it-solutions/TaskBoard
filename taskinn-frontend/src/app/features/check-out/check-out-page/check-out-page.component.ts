import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIcon } from '@angular/material/icon';
import { ToastrService } from 'ngx-toastr';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { MandatoryMarkComponent } from '../../../shared/components/mandatory-mark/mandatory-mark.component';
import { CheckOutService } from '../check-out.service';
import { InvoicePreview } from '../../booking/interfaces/invoice';
import { CommonModule, CurrencyPipe } from '@angular/common';

@Component({
    selector: 'tinn-check-out-page',
    standalone: true,
    imports: [MatIcon, PageHeaderComponent, ReactiveFormsModule, MandatoryMarkComponent, CommonModule, CurrencyPipe],
    templateUrl: './check-out-page.component.html',
})
export class CheckOutPageComponent {
    private readonly fb = inject(FormBuilder);
    private readonly checkOutService = inject(CheckOutService);
    private readonly toastr = inject(ToastrService);
    protected readonly router = inject(Router);

    public readonly loading = this.checkOutService.loading;
    public readonly error = this.checkOutService.error;

    public invoicePreview = signal<InvoicePreview | null>(null);
    public showInvoice = signal(false);

    public bookingForm: FormGroup = this.fb.group({
        bookingId: ['', Validators.required],
    });

    public async onSearch(): Promise<void> {
        if (this.bookingForm.invalid) {
            this.bookingForm.markAllAsTouched();
            return;
        }

        const rawBookingId = this.bookingForm.value.bookingId;
        const bookingId = Number(rawBookingId);

        if (Number.isNaN(bookingId)) {
            this.toastr.error('Booking ID must be a number.', 'Error');
            return;
        }

        const canCheckOut = await this.checkOutService.canCheckOut(bookingId);
        if (!canCheckOut) {
            this.toastr.error('Booking cannot be checked out. Please verify the Booking ID.', 'Error');
            return;
        }

        const preview = await this.checkOutService.getInvoicePreview(bookingId);

        if (preview) {
            this.invoicePreview.set(preview);
            this.showInvoice.set(true);
            this.bookingForm.get('bookingId')?.disable(); // Lock input
        } else if (this.error()) {
            this.toastr.error(this.error()?.message, 'Error fetching invoice');
        }
    }

    public async onFinalize(): Promise<void> {
        const preview = this.invoicePreview();
        if (!preview) {
            return;
        }

        const bookingId = preview.bookingId;
        const remaining = preview.remainingBalance;

        if (remaining !== 0) {
            const paymentSuccess = await this.checkOutService.createPayment(bookingId, remaining);
            if (!paymentSuccess) {
                this.toastr.error('Payment failed. Cannot proceed with checkout.', 'Error');
                return;
            }
            this.toastr.info(`Payment of ${remaining.toFixed(2)} recorded.`, 'Payment Successful');
        }

        const checkoutSuccess = await this.checkOutService.finalizeCheckOut(bookingId);

        if (checkoutSuccess) {
            this.toastr.success('Booking successfully checked out. Invoice sent.', 'Check-out Complete');
            this.router.navigate(['/staff/dashboard']);
        } else {
            this.toastr.error(this.error()?.message || 'Finalize failed', 'Check-out Error');
        }
    }

    public onCancel(): void {
        this.showInvoice.set(false);
        this.invoicePreview.set(null);
        this.bookingForm.get('bookingId')?.enable();
        this.bookingForm.reset();
    }
}
