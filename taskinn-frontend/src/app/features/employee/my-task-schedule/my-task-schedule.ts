import { Component, inject, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { TaskSchedulingService } from '../../admin/task-scheduling.service';
import { Router } from '@angular/router';
import { CleaningTaskDto, CleaningTaskExtended } from '../../admin/interfaces/task-scheduling';
import { FormsModule } from '@angular/forms';
import { MatIcon } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { LightRoomDto } from '../../room/room';
import { AuthService } from '../../auth/auth.service';

@Component({
    selector: 'tinn-employee-task-schedule',
    imports: [FormsModule, MatIcon, PageHeaderComponent],
    templateUrl: './my-task-schedule.html',
})
export class EmployeeTaskScheduleComponent implements OnInit {
    protected readonly Math = Math;

    private readonly toastr = inject(ToastrService);
    private readonly router = inject(Router);
    private readonly authService = inject(AuthService);
    private readonly taskSchedulingService = inject(TaskSchedulingService);

    public loading = this.taskSchedulingService.loading;
    public error = this.taskSchedulingService.error;

    // Date selection
    public year: number = 2024;
    public month: number = 1;
    public day: number = 1;
    public selectedDate: Date = new Date();

    // Data
    public rooms: LightRoomDto[] = [];
    public assignments: CleaningTaskExtended[] = [];
    public employeeId: number = 0;
    public employeeName: string = '';

    public async ngOnInit(): Promise<void> {
        // Get current employee from auth service
        const userProfile = this.authService.userProfile();
        if (!userProfile) {
            this.toastr.error('User not authenticated');
            this.router.navigate(['/auth/login']);
            return;
        }

        this.employeeId = userProfile.id;
        this.employeeName = `${userProfile.firstName} ${userProfile.lastName}`;

        const today = new Date();
        this.year = today.getFullYear();
        this.month = today.getMonth() + 1;
        this.day = today.getDate();
        this.updateSelectedDate();

        await this.loadScheduleForDay();
    }

    public updateSelectedDate(): void {
        this.selectedDate = new Date(this.year, this.month - 1, this.day);
    }

    public async loadScheduleForDay(): Promise<void> {
        this.updateSelectedDate();
        const assignments = await this.taskSchedulingService.fetchTaskScheduleForEmployee(
            this.year,
            this.month,
            this.day,
            this.employeeId,
        );

        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
            return;
        }

        this.assignments = assignments
            .map((assignment) => this.enrichAssignment(assignment))
            .sort((a, b) => new Date(a.scheduledStart).getTime() - new Date(b.scheduledStart).getTime());
    }

    private enrichAssignment(assignment: CleaningTaskDto): CleaningTaskExtended {
        const room = this.rooms.find((r) => r.id === assignment.roomId);

        const startTime = new Date(assignment.scheduledStart);
        const endTime = new Date(assignment.scheduledEnd);
        const duration = Math.round((endTime.getTime() - startTime.getTime()) / (1000 * 60));

        return {
            ...assignment,
            roomNumber: assignment.roomNumber || room?.roomNumber || `Room ${assignment.roomId}`,
            employeeName: assignment.employeeName || this.employeeName,
            duration,
            isEditing: false,
        };
    }

    public async stepDay(direction: number): Promise<void> {
        const currentDate = new Date(this.year, this.month - 1, this.day);
        currentDate.setDate(currentDate.getDate() + direction);

        this.year = currentDate.getFullYear();
        this.month = currentDate.getMonth() + 1;
        this.day = currentDate.getDate();

        await this.loadScheduleForDay();
    }

    public async onDateChange(): Promise<void> {
        // Validate and adjust day if needed
        const daysInMonth = new Date(this.year, this.month, 0).getDate();
        if (this.day > daysInMonth) {
            this.day = daysInMonth;
        }
        if (this.day < 1) {
            this.day = 1;
        }

        await this.loadScheduleForDay();
    }

    public async goToToday(): Promise<void> {
        const today = new Date();
        this.year = today.getFullYear();
        this.month = today.getMonth() + 1;
        this.day = today.getDate();

        await this.loadScheduleForDay();
    }

    public formatTime(date: Date): string {
        const d = new Date(date);
        return d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false });
    }

    public formatDate(date: Date): string {
        const d = new Date(date);
        return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
    }

    public getTotalWorkTime(): number {
        return this.assignments.reduce((total, assignment) => total + (assignment.duration || 0), 0);
    }

    public getFormattedTotalWorkTime(): string {
        const totalMinutes = this.getTotalWorkTime();
        const hours = Math.floor(totalMinutes / 60);
        const minutes = totalMinutes % 60;
        return `${hours}h ${minutes}m`;
    }
}
