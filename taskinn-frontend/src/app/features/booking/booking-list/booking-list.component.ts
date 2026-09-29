import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { MatIcon } from '@angular/material/icon';
import { PageChangeEvent, PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { ToastrService } from 'ngx-toastr';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { BookingService } from '../booking.service';
import { BookingStore } from '../booking.store';
import { BookingDetailDialogComponent } from './booking-detail-dialog.component';
import { BookingEditDialogComponent } from './booking-edit-dialog/booking-edit-dialog.component';

enum StatusFilter {
    ALL = 'ALL',
    CONFIRMED = 'CONFIRMED',
    CHECKED_IN = 'CHECKED_IN',
    CHECKED_OUT = 'CHECKED_OUT',
}

@Component({
    selector: 'tinn-booking-list',
    standalone: true,
    imports: [CommonModule, PageHeaderComponent, MatIcon, FormsModule, PaginatorComponent, DatePipe],
    templateUrl: './booking-list.component.html',
    styleUrls: [],
})
export class BookingListComponent implements OnInit {
    private readonly bookingService = inject(BookingService);
    private readonly bookingStore = inject(BookingStore);
    private readonly toastr = inject(ToastrService);
    private readonly dialog = inject(MatDialog);
    private readonly router = inject(Router);

    // Signals for UI state
    public readonly loading = this.bookingStore.loading;
    public readonly allBookings = this.bookingStore.allBookings;

    // --- Filter Signals ---
    public searchTerm = signal('');
    public statusFilter = signal<StatusFilter>(StatusFilter.ALL);
    public filterStartDate = signal<string | null>(null);
    public filterEndDate = signal<string | null>(null);

    // --- Paginator Signals ---
    public pageSize = signal(10);
    public pageIndex = signal(0);
    public pageSizeOptions = [10, 25, 50];

    public filteredBookings = computed(() => {
        const search = this.searchTerm().toLowerCase().trim();
        const status = this.statusFilter();
        const startDate = this.filterStartDate();
        const endDate = this.filterEndDate();
        const bookingList = this.allBookings();

        if (!bookingList) {
            return [];
        }

        let filtered = bookingList;

        if (startDate) {
            filtered = filtered.filter((booking) => booking.checkInDate === startDate);
        }
        if (endDate) {
            filtered = filtered.filter((booking) => booking.checkOutDate === endDate);
        }

        if (status !== StatusFilter.ALL) {
            filtered = filtered.filter((booking) => booking.status === status.toString());
        }

        if (search) {
            filtered = filtered.filter(
                (booking) => booking.guestName.toLowerCase().includes(search) || booking.id.toString().includes(search),
            );
        }

        filtered = filtered.sort((a, b) => a.id - b.id);
        return filtered;
    });

    public paginatedBookings = computed(() => {
        const filtered = this.filteredBookings();
        const startIndex = this.pageIndex() * this.pageSize();
        return filtered.slice(startIndex, startIndex + this.pageSize());
    });

    async ngOnInit(): Promise<void> {
        await this.bookingService.fetchAllBookings();
    }

    public async onRowClick(bookingId: number): Promise<void> {
        const bookingDetails = await this.bookingService.fetchBookingDetails(bookingId);
        if (bookingDetails) {
            this.dialog.open(BookingDetailDialogComponent, {
                width: '800px',
                maxWidth: '90vw',
                data: bookingDetails,
            });
        } else {
            this.toastr.error('Could not load booking details.', 'Error');
        }
    }

    public async onEditBooking(event: Event, bookingId: number): Promise<void> {
        event.stopPropagation();

        const bookingDetails = await this.bookingService.fetchBookingDetails(bookingId);
        if (!bookingDetails) {
            this.toastr.error('Could not load booking details.', 'Error');
            return;
        }

        const dialogRef = this.dialog.open(BookingEditDialogComponent, {
            width: '900px',
            maxWidth: '95vw',
            maxHeight: '90vh',
            data: {
                bookingId: bookingId,
                booking: bookingDetails,
            },
            disableClose: true,
        });

        const result = await firstValueFrom(dialogRef.afterClosed());
        if (result) {
            // Refresh the booking list after successful update
            await this.bookingService.fetchAllBookings();
            this.toastr.success('Booking updated successfully');
        }
    }

    public onSearchChange(value: string): void {
        this.searchTerm.set(value);
        this.pageIndex.set(0);
    }

    public clearSearch(): void {
        this.searchTerm.set('');
        this.pageIndex.set(0);
    }

    public onStatusFilterChange(status: StatusFilter): void {
        this.statusFilter.set(status);
        this.pageIndex.set(0);
    }

    public onDateChange(type: 'start' | 'end', value: string): void {
        if (type === 'start') {
            this.filterStartDate.set(value || null);
        } else {
            this.filterEndDate.set(value || null);
        }
        this.pageIndex.set(0);
    }

    public onPageChange(event: PageChangeEvent): void {
        this.pageIndex.set(event.pageIndex);
        this.pageSize.set(event.pageSize);
    }

    public displayStatus(status: string): string {
        if (!status) {
            return '';
        }
        return status
            .replace(/_/g, ' ')
            .toLowerCase()
            .replace(/\b\w/g, (char) => char.toUpperCase());
    }
}
