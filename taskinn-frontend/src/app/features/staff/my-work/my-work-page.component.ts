import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StaffService } from '../staff.service';
import { MonthShifts } from '../staff.store';
import { Pto } from '../pto/interfaces/pto';
import {
    DateInterval,
    DateRangeCalendarComponent,
} from '../../../shared/components/date-range-calendar/date-range-calendar.component';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'tinn-my-work-page',
    templateUrl: './my-work-page.component.html',
    imports: [CommonModule, PageHeaderComponent, DateRangeCalendarComponent, MatIconModule],
})
export class MyWorkPageComponent implements OnInit {
    @ViewChild(DateRangeCalendarComponent) private calendar!: DateRangeCalendarComponent;

    private readonly staffService = inject(StaffService);
    private readonly toastr = inject(ToastrService);

    private readonly error = this.staffService.error;

    // Dialog state
    public showCreateDialog = signal(false);
    public showDeleteDialog = signal(false);
    public pendingCreateRange = signal<{ startDate: Date; endDate: Date } | null>(null);
    public pendingDeleteInterval = signal<DateInterval | null>(null);

    async ngOnInit(): Promise<void> {
        // Load PTOs
        await this.staffService.getPTOsOfStaff();

        // Load shifts for current and next months
        const today = new Date();
        const currentYear = today.getFullYear();
        const currentMonth = today.getMonth() + 1;

        // Fetch shifts for the next 3 months
        for (let i = 0; i < 3; i++) {
            let month = currentMonth + i;
            let year = currentYear;
            if (month > 12) {
                month -= 12;
                year += 1;
            }
            await this.staffService.getMyShifts(year, month);
        }
    }

    public readonly dateIntervals = computed<DateInterval[]>(() =>
        this.staffService.ptos().map((pto: Pto) => ({
            id: pto.id,
            startDate: pto.startDate,
            endDate: pto.endDate,
        })),
    );

    public readonly shiftDays = computed<MonthShifts[]>(() => this.staffService.shifts());

    public onRangeSelected(range: { startDate: Date; endDate: Date }): void {
        // Check if any day in the range overlaps with a scheduled shift
        if (this.doesRangeOverlapWithShifts(range.startDate, range.endDate)) {
            this.toastr.error('You cannot request PTO on days you are scheduled to work.', 'Conflict');
            this.calendar?.clearSelection();
            return;
        }

        this.pendingCreateRange.set(range);
        this.showCreateDialog.set(true);
    }

    private doesRangeOverlapWithShifts(startDate: Date, endDate: Date): boolean {
        const shifts = this.shiftDays();

        // Iterate through each day in the range
        const currentDate = new Date(startDate);
        while (currentDate <= endDate) {
            const year = currentDate.getFullYear();
            const month = currentDate.getMonth() + 1;
            const dayIndex = currentDate.getDate() - 1;

            // Check if this day is a shift day
            for (const monthShifts of shifts) {
                if (monthShifts.year === year && monthShifts.month === month) {
                    if (monthShifts.dayIndices.includes(dayIndex)) {
                        return true;
                    }
                }
            }

            // Move to next day
            currentDate.setDate(currentDate.getDate() + 1);
        }

        return false;
    }

    public onIntervalClicked(interval: DateInterval): void {
        this.pendingDeleteInterval.set(interval);
        this.showDeleteDialog.set(true);
    }

    public async confirmCreatePto(): Promise<void> {
        const range = this.pendingCreateRange();
        if (!range) {
            return;
        }

        const request = {
            startDate: this.formatDateToIso(range.startDate),
            endDate: this.formatDateToIso(range.endDate),
        };

        await this.staffService.createPTO(request);
        if (this.error()) {
            const errorMessage = this.error()?.message || 'An error occurred';
            this.toastr.error(errorMessage, 'Failed to create PTO');
        } else {
            this.toastr.success('PTO was successfully created.', 'Success');
        }
        this.closeCreateDialog();
    }

    public closeCreateDialog(): void {
        this.showCreateDialog.set(false);
        this.pendingCreateRange.set(null);
        this.calendar?.clearSelection();
    }

    public async confirmDeletePto(): Promise<void> {
        const interval = this.pendingDeleteInterval();
        if (!interval?.id) {
            return;
        }

        await this.staffService.deletePTO(interval.id);
        if (this.error()) {
            const errorMessage = this.error()?.message || 'An error occurred';
            this.toastr.error(errorMessage, 'Failed to delete PTO');
        } else {
            this.toastr.success('PTO was successfully deleted.', 'Success');
        }
        this.closeDeleteDialog();
    }

    public closeDeleteDialog(): void {
        this.showDeleteDialog.set(false);
        this.pendingDeleteInterval.set(null);
    }

    public formatDate(date: Date | string): string {
        const d = typeof date === 'string' ? new Date(date) : date;
        return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' });
    }

    private formatDateToIso(date: Date): string {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }
}
