import {
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild,
    ElementRef,
    AfterViewInit,
    OnDestroy,
    OnChanges,
    SimpleChanges,
    input,
    effect,
    inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';

export interface DateInterval {
    id?: number;
    startDate: string;
    endDate: string;
}

export type CalendarMode = 'booking' | 'pto' | 'work';

interface CalendarDay {
    date: Date;
    day: number;
    isCurrentMonth: boolean;
    isDisabled: boolean;
    isRangeStart: boolean;
    isRangeEnd: boolean;
    isInRange: boolean;
    isToday: boolean;
    isIntervalStart: boolean;
    isIntervalEnd: boolean;
    isInInterval: boolean;
    intervalId: number | null;
    isShiftDay: boolean;
}

interface CalendarMonth {
    year: number;
    month: number;
    monthName: string;
    weeks: CalendarDay[][];
    isCurrentMonth: boolean;
}

@Component({
    selector: 'tinn-date-range-calendar',
    imports: [CommonModule],
    templateUrl: './date-range-calendar.component.html',
    styleUrl: './date-range-calendar.component.css',
})
export class DateRangeCalendarComponent implements OnInit, AfterViewInit, OnDestroy, OnChanges {
    private readonly toastr = inject(ToastrService);
    public dateIntervals = input<DateInterval[]>();
    public shiftDays = input<{ year: number; month: number; dayIndices: number[] }[]>();
    @Input() public minDate: Date = new Date();
    @Input() public initialStartDate: string | null = null;
    @Input() public initialEndDate: string | null = null;
    @Input() public maxHeightInRem: number | null = null;
    @Input() public mode: CalendarMode = 'booking';

    @Output() public readonly startDateChange = new EventEmitter<Date | null>();
    @Output() public readonly endDateChange = new EventEmitter<Date | null>();
    @Output() public readonly rangeSelected = new EventEmitter<{ startDate: Date; endDate: Date }>();
    @Output() public readonly intervalClicked = new EventEmitter<DateInterval>();
    @Output() public readonly monthChanged = new EventEmitter<{ year: number; month: number }>();

    private parsedIntervals: { id: number | null; start: Date; end: Date }[] = [];
    private parsedShiftDays: Set<string> = new Set();

    @ViewChild('calendarContainer') private calendarContainer!: ElementRef<HTMLDivElement>;

    public startDate: Date | null = null;
    public endDate: Date | null = null;
    public months: CalendarMonth[] = [];
    public weekDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

    private scrollListener: (() => void) | null = null;

    constructor() {
        effect(() => {
            const intervals = this.dateIntervals();
            this.parsedIntervals = (intervals ?? []).map((interval) => ({
                id: interval.id ?? null,
                start: this.parseDate(interval.startDate),
                end: this.parseDate(interval.endDate),
            }));
            if (this.months.length > 0) {
                this.regenerateCalendar();
            }
        });
        effect(() => {
            const shifts = this.shiftDays();
            this.parsedShiftDays.clear();
            if (shifts) {
                for (const monthShifts of shifts) {
                    for (const dayIndex of monthShifts.dayIndices) {
                        // Create key: year-month-dayIndex (dayIndex is 0-based)
                        const key = `${monthShifts.year}-${monthShifts.month}-${dayIndex}`;
                        this.parsedShiftDays.add(key);
                    }
                }
            }
            if (this.months.length > 0) {
                this.regenerateCalendar();
            }
        });
    }

    public ngOnInit(): void {
        this.startDate = this.initialStartDate ? this.parseDate(this.initialStartDate) : null;
        this.endDate = this.initialEndDate ? this.parseDate(this.initialEndDate) : null;
        this.generateCalendar();
    }

    public ngOnChanges(changes: SimpleChanges): void {
        if (changes['initialStartDate'] || changes['initialEndDate']) {
            this.startDate = this.initialStartDate ? this.parseDate(this.initialStartDate) : null;
            this.endDate = this.initialEndDate ? this.parseDate(this.initialEndDate) : null;

            if (this.months.length > 0) {
                this.regenerateCalendar();
            }
        }
    }

    public ngAfterViewInit(): void {
        if (this.calendarContainer) {
            this.calendarContainer.nativeElement.scrollTop = 0;
        }
        this.scrollListener = this.onScroll.bind(this);
        this.calendarContainer.nativeElement.addEventListener('scroll', this.scrollListener);
    }

    public ngOnDestroy(): void {
        if (this.scrollListener && this.calendarContainer) {
            this.calendarContainer.nativeElement.removeEventListener('scroll', this.scrollListener);
        }
    }

    public onDateClick(day: CalendarDay): void {
        if (day.isDisabled || !day.isCurrentMonth) {
            return;
        }

        if (this.mode === 'work') {
            return;
        }

        if (this.mode === 'pto') {
            this.handlePtoModeClick(day);
            return;
        }

        this.handleBookingModeClick(day);
    }

    private handleBookingModeClick(day: CalendarDay): void {
        if (!this.startDate) {
            this.startDate = day.date;
            this.startDateChange.emit(this.startDate);
            this.regenerateCalendar();
            return;
        }

        if (!this.endDate) {
            if (day.date < this.startDate) {
                this.endDate = this.startDate;
                this.startDate = day.date;
            } else {
                this.endDate = day.date;
            }

            this.startDateChange.emit(this.startDate);
            this.endDateChange.emit(this.endDate);
            this.regenerateCalendar();
            return;
        }

        this.startDate = day.date;
        this.endDate = null;
        this.startDateChange.emit(this.startDate);
        this.endDateChange.emit(null);
        this.regenerateCalendar();
    }

    private handlePtoModeClick(day: CalendarDay): void {
        // If clicking on an existing interval, emit for deletion
        if (day.intervalId !== null) {
            const interval = this.findIntervalById(day.intervalId);
            if (interval) {
                this.intervalClicked.emit(interval);
            }
            return;
        }

        // Otherwise, handle range selection for new PTO
        if (!this.startDate) {
            this.startDate = day.date;
            this.regenerateCalendar();
            return;
        }

        if (!this.endDate) {
            if (day.date < this.startDate) {
                this.endDate = this.startDate;
                this.startDate = day.date;
            } else {
                this.endDate = day.date;
            }

            if (this.doesRangeOverlapExistingIntervals(this.startDate, this.endDate)) {
                this.toastr.error('The selected period overlaps with an existing PTO.', 'Error');
                this.clearSelection();
                return;
            }

            this.regenerateCalendar();
            // Emit the selected range for the parent to handle
            this.rangeSelected.emit({ startDate: this.startDate, endDate: this.endDate });
            return;
        }

        // Reset and start new selection
        this.startDate = day.date;
        this.endDate = null;
        this.regenerateCalendar();
    }

    private findIntervalById(id: number): DateInterval | null {
        const intervals = this.dateIntervals();
        return intervals?.find((interval) => interval.id === id) ?? null;
    }

    public clearSelection(): void {
        this.startDate = null;
        this.endDate = null;
        this.regenerateCalendar();
    }

    public getDayClasses(day: CalendarDay): string {
        const classes: string[] = ['calendar-day'];

        if (!day.isCurrentMonth) {
            classes.push('other-month');
            return classes.join(' ');
        }

        if (day.isDisabled) {
            classes.push('disabled');
            return classes.join(' ');
        }

        classes.push('enabled');

        if (day.isToday) {
            classes.push('today');
        }

        if (day.isRangeStart) {
            classes.push('range-start');
        }

        if (day.isRangeEnd) {
            classes.push('range-end');
        }

        if (day.isInRange) {
            classes.push('in-range');
        }

        if (day.isIntervalStart) {
            classes.push('interval-start');
        }

        if (day.isIntervalEnd) {
            classes.push('interval-end');
        }

        if (day.isInInterval) {
            classes.push('in-interval');
        }

        if (day.isShiftDay) {
            classes.push('shift-day');
        }

        return classes.join(' ');
    }

    private isDateInIntervalRange(date: Date, interval: { start: Date; end: Date }): boolean {
        const dateTime = date.getTime();
        return dateTime >= interval.start.getTime() && dateTime <= interval.end.getTime();
    }

    private isIntervalStart(date: Date): boolean {
        return this.parsedIntervals.some((interval) => this.isSameDay(date, interval.start));
    }

    private isIntervalEnd(date: Date): boolean {
        return this.parsedIntervals.some((interval) => this.isSameDay(date, interval.end));
    }

    private isInAnyInterval(date: Date): boolean {
        return this.parsedIntervals.some(
            (interval) => date.getTime() > interval.start.getTime() && date.getTime() < interval.end.getTime(),
        );
    }

    private getIntervalIdForDate(date: Date): number | null {
        const interval = this.parsedIntervals.find((int) => this.isDateInIntervalRange(date, int));
        return interval?.id ?? null;
    }

    private isShiftDay(date: Date): boolean {
        const year = date.getFullYear();
        const month = date.getMonth() + 1;
        const dayIndex = date.getDate() - 1;
        const key = `${year}-${month}-${dayIndex}`;
        return this.parsedShiftDays.has(key);
    }

    private doesRangeOverlapExistingIntervals(start: Date, end: Date): boolean {
        return this.parsedIntervals.some((interval) => this.rangesOverlap(start, end, interval.start, interval.end));
    }

    private rangesOverlap(aStart: Date, aEnd: Date, bStart: Date, bEnd: Date): boolean {
        return aStart.getTime() <= bEnd.getTime() && bStart.getTime() <= aEnd.getTime();
    }

    /**
     * Parses a date string in dd-MM-yyyy or yyyy-MM-dd (ISO) format to a Date object
     */
    private parseDate(dateString: string): Date {
        const parts = dateString.split('-');
        if (parts.length === 3) {
            if (parts[0].length === 4) {
                const year = parseInt(parts[0], 10);
                const month = parseInt(parts[1], 10) - 1;
                const day = parseInt(parts[2], 10);
                return new Date(year, month, day);
            }
            const day = parseInt(parts[0], 10);
            const month = parseInt(parts[1], 10) - 1;
            const year = parseInt(parts[2], 10);
            return new Date(year, month, day);
        }
        return new Date(dateString);
    }

    private onScroll(): void {
        if (!this.calendarContainer) {
            return;
        }

        const container = this.calendarContainer.nativeElement;
        const scrollTop = container.scrollTop;
        const scrollHeight = container.scrollHeight;
        const clientHeight = container.clientHeight;

        // If user is within 200px of the bottom, load more months
        const threshold = 200;
        if (scrollTop + clientHeight >= scrollHeight - threshold) {
            this.loadMoreMonths();
        }
    }

    private loadMoreMonths(): void {
        const lastMonth = this.months[this.months.length - 1];
        const startDate = new Date(lastMonth.year, lastMonth.month + 1, 1);

        for (let i = 0; i < 6; i++) {
            const monthDate = new Date(startDate.getFullYear(), startDate.getMonth() + i, 1);
            this.months.push(this.generateMonth(monthDate, false));
        }
    }

    private generateCalendar(): void {
        const today = new Date();
        const currentMonth = today.getMonth();
        const currentYear = today.getFullYear();
        this.months = [];

        for (let i = 0; i <= 5; i++) {
            const monthDate = new Date(today.getFullYear(), today.getMonth() + i, 1);
            const isCurrentMonth = monthDate.getMonth() === currentMonth && monthDate.getFullYear() === currentYear;
            this.months.push(this.generateMonth(monthDate, isCurrentMonth));
        }
    }

    private generateMonth(date: Date, isCurrentMonth: boolean = false): CalendarMonth {
        const year = date.getFullYear();
        const month = date.getMonth();
        const monthName = date.toLocaleString('default', { month: 'long', year: 'numeric' });

        const firstDay = new Date(year, month, 1);
        const lastDay = new Date(year, month + 1, 0);
        const startingDayOfWeek = (firstDay.getDay() + 6) % 7;

        const allWeeks: CalendarDay[][] = [];
        let currentWeek: CalendarDay[] = [];

        const prevMonthLastDay = new Date(year, month, 0).getDate();
        for (let i = startingDayOfWeek - 1; i >= 0; i--) {
            const day = prevMonthLastDay - i;
            const dayDate = new Date(year, month - 1, day);
            currentWeek.push(this.createCalendarDay(dayDate, false));
        }

        for (let day = 1; day <= lastDay.getDate(); day++) {
            const dayDate = new Date(year, month, day);
            currentWeek.push(this.createCalendarDay(dayDate, true));

            if (currentWeek.length === 7) {
                allWeeks.push(currentWeek);
                currentWeek = [];
            }
        }

        if (currentWeek.length > 0) {
            let nextMonthDay = 1;
            while (currentWeek.length < 7) {
                const dayDate = new Date(year, month + 1, nextMonthDay);
                currentWeek.push(this.createCalendarDay(dayDate, false));
                nextMonthDay++;
            }
            allWeeks.push(currentWeek);
        }

        return { year, month, monthName, weeks: allWeeks, isCurrentMonth };
    }

    private createCalendarDay(date: Date, isCurrentMonth: boolean): CalendarDay {
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const dateWithoutTime = new Date(date);
        dateWithoutTime.setHours(0, 0, 0, 0);

        const isDisabled = dateWithoutTime < this.minDate;
        const isToday = dateWithoutTime.getTime() === today.getTime();
        const isRangeStart = this.startDate ? this.isSameDay(dateWithoutTime, this.startDate) : false;
        const isRangeEnd = this.endDate ? this.isSameDay(dateWithoutTime, this.endDate) : false;
        const isInRange = this.isDateInRange(dateWithoutTime);
        const isIntervalStart = this.isIntervalStart(dateWithoutTime);
        const isIntervalEnd = this.isIntervalEnd(dateWithoutTime);
        const isInInterval = this.isInAnyInterval(dateWithoutTime);
        const intervalId = this.getIntervalIdForDate(dateWithoutTime);

        const isShiftDay = this.isShiftDay(dateWithoutTime);

        return {
            date: dateWithoutTime,
            day: date.getDate(),
            isCurrentMonth,
            isDisabled,
            isRangeStart,
            isRangeEnd,
            isInRange,
            isToday,
            isIntervalStart,
            isIntervalEnd,
            isInInterval,
            intervalId,
            isShiftDay,
        };
    }

    private isSameDay(date1: Date, date2: Date): boolean {
        return (
            date1.getFullYear() === date2.getFullYear() &&
            date1.getMonth() === date2.getMonth() &&
            date1.getDate() === date2.getDate()
        );
    }

    private isDateInRange(date: Date): boolean {
        if (!this.startDate || !this.endDate) {
            return false;
        }

        const startDate = new Date(this.startDate);
        const endDate = new Date(this.endDate);
        startDate.setHours(0, 0, 0, 0);
        endDate.setHours(0, 0, 0, 0);

        const dateTime = date.getTime();
        return dateTime > startDate.getTime() && dateTime < endDate.getTime();
    }

    private regenerateCalendar(): void {
        this.months = this.months.map((month) => {
            const monthDate = new Date(month.year, month.month, 1);
            const today = new Date();
            const isCurrentMonth = month.month === today.getMonth() && month.year === today.getFullYear();
            return this.generateMonth(monthDate, isCurrentMonth);
        });
    }
}
