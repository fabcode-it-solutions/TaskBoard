import { Component, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { DetailedBooking } from '../interfaces/room';
import { BookingService } from '../booking.service';
import { MatTabsModule } from '@angular/material/tabs';
import { InvoicePreview } from '../interfaces/invoice';

@Component({
    selector: 'tinn-booking-detail-dialog',
    standalone: true,
    imports: [CommonModule, MatDialogModule, MatIcon, MatButtonModule, DatePipe, MatTabsModule],
    templateUrl: './booking-detail-dialog.component.html',
    styleUrls: ['./booking-detail-dialog.component.css'],
})
export class BookingDetailDialogComponent {
    public data: DetailedBooking = inject(MAT_DIALOG_DATA);
    private dialogRef = inject(MatDialogRef<BookingDetailDialogComponent>);
    private readonly bookingService = inject(BookingService);
    public invoiceData: InvoicePreview | null = null;
    public isLoadingInvoice = false;

    public closeDialog(): void {
        this.dialogRef.close();
    }

    public async loadInvoicePreview(): Promise<void> {
        this.isLoadingInvoice = true;
        try {
            this.invoiceData = await this.bookingService.getInvoice(this.data.bookingId);
        } finally {
            this.isLoadingInvoice = false;
        }
    }

    public onTabChange(index: number): void {
        if (index === 1 && !this.invoiceData) {
            this.loadInvoicePreview();
        }
    }
}
