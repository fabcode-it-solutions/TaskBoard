import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { TaskSchedulingService } from '../task-scheduling.service';
import {
    CleaningTaskDto,
    CleaningTaskExtended,
    CleaningTaskSaveDto,
    RoomConstraintFormModel,
    TaskConstraintDto,
    TaskScheduleSolveRequestDto,
    TaskStatus,
    TaskType,
} from '../interfaces/task-scheduling';
import { FormsModule } from '@angular/forms';
import { MatIcon } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RoomService } from '../../room/room.service';
import { RoomStore } from '../../room/room.store';
import { LightRoomDto, RoomStatus } from '../../room/room';
import { EmployeeSelectorComponent } from './employee-selector/employee-selector';
import { RoomSelectorComponent } from './room-selector/room-selector';
import { TaskAssignmentsTableComponent } from './task-assignments-table/task-assignments-table';
import { OptimizationParametersComponent } from './optimization-parameters/optimization-parameters';
import { ShiftSchedulingService } from '../shift-scheduling.service';
import { SchedulableEmployee } from '../interfaces/shift-scheduling';
import { SettingValueRecord, UserSettingsService } from '../../../shared/services/user-settings.service';
import { BookingService } from '../../booking/booking.service';

@Component({
    selector: 'tinn-task-scheduling',
    imports: [
        FormsModule,
        MatIcon,
        PageHeaderComponent,
        EmployeeSelectorComponent,
        RoomSelectorComponent,
        TaskAssignmentsTableComponent,
        OptimizationParametersComponent,
        RouterLink,
    ],
    templateUrl: './task-scheduling.html',
})
export class TaskSchedulingComponent implements OnInit {
    private readonly router = inject(Router);
    private readonly toastr = inject(ToastrService);
    private readonly taskSchedulingService = inject(TaskSchedulingService);
    private readonly shiftSchedulingService = inject(ShiftSchedulingService);
    private readonly roomService = inject(RoomService);
    private readonly roomStore = inject(RoomStore);
    private readonly settingService = inject(UserSettingsService);
    private readonly bookingService = inject(BookingService);

    public loading = this.taskSchedulingService.loading;
    public error = this.taskSchedulingService.error;

    // Date selection
    public year: number = 2024;
    public month: number = 1;
    public day: number = 1;
    public selectedDate: Date = new Date();

    // Data
    public schedulableEmployees: SchedulableEmployee[] = [];
    public rooms: LightRoomDto[] = [];
    public assignments: CleaningTaskExtended[] = [];
    public roomConstraints: RoomConstraintFormModel[] = [];
    public estimatorSettings: SettingValueRecord['task_duration_estimator'];
    public earlyCheckInIds: Set<number> = new Set();
    public lateCheckOutIds: Set<number> = new Set();

    // UI state
    public editing: boolean = false;
    public viewMode: 'list' | 'optimize' = 'list';

    // Optimization form
    public selectedEmployeeIds: Set<number> = new Set();
    public selectedRoomIds: Set<number> = new Set();
    public optimizationStartTime: string = '08:00';

    public async ngOnInit(): Promise<void> {
        const today = new Date();
        this.year = today.getFullYear();
        this.month = today.getMonth() + 1;
        this.day = today.getDate();
        this.updateSelectedDate();

        await this.loadSettings();
        await this.loadSchedulableEmployees();
        await this.loadRooms();
        await this.loadScheduleForDay();
    }

    public async loadSchedulableEmployees(): Promise<void> {
        this.schedulableEmployees = await this.shiftSchedulingService.fetchSchedulableEmployees();
        if (this.shiftSchedulingService.error()) {
            this.toastr.error(this.shiftSchedulingService.error()?.message, this.shiftSchedulingService.error()?.error);
            this.router.navigate(['/admin/manager-dashboard']);
        }
        this.schedulableEmployees.sort(this.shiftSchedulingService.compareEmployeeNames);
    }

    public async loadRooms(): Promise<void> {
        await this.roomService.getActiveRooms();
        const roomList = this.roomStore.roomList();
        if (this.roomService.error()) {
            this.toastr.error(this.roomService.error()?.message, this.roomService.error()?.error);
        }
        if (roomList) {
            this.rooms = roomList.sort((a, b) => a.roomNumber.localeCompare(b.roomNumber));
        }
    }

    public async loadSettings(): Promise<void> {
        const settings = await this.settingService.fetchSettings('task_duration_estimator');
        if (!settings || this.settingService.error()) {
            this.toastr.error(this.settingService.error()?.message, this.settingService.error()?.error);
            this.router.navigate(['/admin/manager-dashboard']);
            return;
        }
        this.estimatorSettings = settings;
    }

    public updateSelectedDate(): void {
        this.selectedDate = new Date(this.year, this.month - 1, this.day);
    }

    public async loadScheduleForDay(): Promise<void> {
        this.updateSelectedDate();
        const assignments = await this.taskSchedulingService.fetchTaskScheduleForDay(this.year, this.month, this.day);

        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
            return;
        }

        const shiftAssignments = await this.shiftSchedulingService.fetchShiftsForMonth(this.year, this.month);
        this.deselectAllEmployees();
        for (const assignment of shiftAssignments) {
            if (assignment.employee && assignment.dayIndices?.includes(this.day - 1)) {
                this.selectedEmployeeIds.add(assignment.employee.id);
            }
        }

        const bookingConstraints = await this.bookingService.getSpecialBookingConstraints(
            this.year,
            this.month,
            this.day,
        );
        if (bookingConstraints) {
            this.earlyCheckInIds = new Set(bookingConstraints.earlyCheckInRoomIds);
            this.lateCheckOutIds = new Set(bookingConstraints.lateCheckOutRoomIds);
        }

        this.assignments = assignments.map((assignment) => this.enrichAssignment(assignment));
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
        const daysInMonth = new Date(this.year, this.month, 0).getDate();
        if (this.day > daysInMonth) {
            this.day = daysInMonth;
        }
        if (this.day < 1) {
            this.day = 1;
        }

        await this.loadScheduleForDay();
    }

    public addNewAssignment(): void {
        const defaultStartTime = new Date(this.year, this.month - 1, this.day, 8, 0);
        const defaultEndTime = new Date(this.year, this.month - 1, this.day, 9, 0);

        const newAssignment: CleaningTaskExtended = {
            roomId: this.rooms[0]?.id || 0,
            employeeId: this.schedulableEmployees[0]?.id || 0,
            scheduledStart: defaultStartTime,
            scheduledEnd: defaultEndTime,
            taskType: TaskType.CHECKOUT_CLEAN, // Default task type
            roomNumber: this.rooms[0]?.roomNumber || '',
            employeeName: this.schedulableEmployees[0] ? this.getEmployeeFullName(this.schedulableEmployees[0]) : '',
            duration: 60,
            isEditing: true,
        };

        this.assignments.push(newAssignment);
    }

    public removeAssignment(index: number): void {
        this.assignments.splice(index, 1);
    }

    public editAssignment(index: number): void {
        this.assignments[index].isEditing = true;
    }

    public saveAssignment(index: number): void {
        this.assignments[index].isEditing = false;
        this.assignments[index] = this.enrichAssignment(this.assignments[index]);
    }

    public async saveSchedule(): Promise<void> {
        try {
            const assignmentDtos: CleaningTaskSaveDto[] = this.assignments.map((a) => ({
                id: a.id,
                roomId: a.roomId,
                employeeId: a.employeeId,
                scheduledStart: this.toLocalDateTimeString(new Date(a.scheduledStart)),
                scheduledEnd: this.toLocalDateTimeString(new Date(a.scheduledEnd)),
                taskType: a.taskType,
                status: a.status,
                notes: a.notes,
            }));

            await this.taskSchedulingService.saveTaskSchedule(this.year, this.month, this.day, assignmentDtos);

            if (this.error()) {
                // Display validation errors if present
                if (this.error()?.validationErrors && this.error()!.validationErrors!.length > 0) {
                    this.error()!.validationErrors!.forEach((validationError) => {
                        this.toastr.error(validationError.message, 'Validation Error');
                    });
                } else {
                    this.toastr.error(this.error()?.message, this.error()?.error);
                }
            } else {
                this.toastr.success('Schedule saved successfully');
                this.editing = false;
                await this.loadScheduleForDay();
            }
            // eslint-disable-next-line unused-imports/no-unused-vars
        } catch (error) {
            // Error handling is done in the service
        }
    }

    public toggleEmployeeSelection(employeeId: number): void {
        if (this.selectedEmployeeIds.has(employeeId)) {
            this.selectedEmployeeIds.delete(employeeId);
        } else {
            this.selectedEmployeeIds.add(employeeId);
        }
    }

    public toggleRoomSelection(roomId: number): void {
        if (this.selectedRoomIds.has(roomId)) {
            this.selectedRoomIds.delete(roomId);
        } else {
            this.selectedRoomIds.add(roomId);
        }
        this.updateRoomConstraints();
    }

    public taskTypeFromRoomStatus(roomStatus: RoomStatus | undefined): TaskType | undefined {
        if (!roomStatus) {
            return undefined;
        }
        switch (roomStatus) {
            case RoomStatus.AVAILABLE:
                return TaskType.STAYOVER_CLEAN;
            case RoomStatus.OCCUPIED:
                return TaskType.STAYOVER_CLEAN;
            case RoomStatus.DIRTY:
                return TaskType.DEEP_CLEAN;
            case RoomStatus.CLEANING:
                return TaskType.STAYOVER_CLEAN;
            case RoomStatus.OUT_OF_ORDER:
                return TaskType.STAYOVER_CLEAN;
            case RoomStatus.PERMANENTLY_INACTIVE:
                return TaskType.STAYOVER_CLEAN;
            default:
                return TaskType.STAYOVER_CLEAN;
        }
    }

    public updateRoomConstraints(): void {
        this.roomConstraints = Array.from(this.selectedRoomIds)
            .map((roomId) => {
                const room = this.rooms.find((r) => r.id === roomId);
                const existing = this.roomConstraints.find((c) => c.roomId === roomId);

                if (existing) {
                    return existing;
                }

                const existingAssignment = this.assignments.find((a) => a.roomId === roomId);

                const taskType =
                    existingAssignment?.taskType ||
                    this.taskTypeFromRoomStatus(room?.status) ||
                    TaskType.STAYOVER_CLEAN;

                let duration = 60;
                if (this.estimatorSettings && room) {
                    duration =
                        room.effectiveCapacity * (this.estimatorSettings.base_capacity ?? 0) +
                        (room.effectiveSingleBed ?? 0) * (this.estimatorSettings.base_single_bed ?? 0) +
                        (room.effectiveDoubleBed ?? 0) * (this.estimatorSettings.base_double_bed ?? 0);
                    if (taskType === TaskType.CHECKOUT_CLEAN) {
                        duration += this.estimatorSettings.base_type_checkout ?? 0;
                        if (this.estimatorSettings.mul_type_checkout && this.estimatorSettings.mul_type_checkout > 0) {
                            duration *= this.estimatorSettings.mul_type_checkout;
                        }
                    } else if (taskType === TaskType.STAYOVER_CLEAN) {
                        duration += this.estimatorSettings.base_type_stayover ?? 0;
                        if (this.estimatorSettings.mul_type_stayover && this.estimatorSettings.mul_type_stayover > 0) {
                            duration *= this.estimatorSettings.mul_type_stayover;
                        }
                    } else if (taskType === TaskType.DEEP_CLEAN) {
                        duration += this.estimatorSettings.base_type_deep ?? 0;
                        if (this.estimatorSettings.mul_type_deep && this.estimatorSettings.mul_type_deep > 0) {
                            duration *= this.estimatorSettings.mul_type_deep;
                        }
                    }

                    const mulCapacity = this.estimatorSettings.mul_capacity ?? 0;
                    if (mulCapacity > 0) {
                        duration *= room.effectiveCapacity * mulCapacity;
                    }
                    const mulSingleBed = this.estimatorSettings.mul_single_bed ?? 0;
                    if (mulSingleBed > 0) {
                        duration *= (room.effectiveSingleBed ?? 1) * mulSingleBed;
                    }
                    const mulDoubleBed = this.estimatorSettings.mul_double_bed ?? 0;
                    if (mulDoubleBed > 0) {
                        duration *= (room.effectiveDoubleBed ?? 1) * mulDoubleBed;
                    }

                    duration = Math.max(this.estimatorSettings.min_duration ?? 1, duration);
                }

                let constraintType: RoomConstraintFormModel['constraintType'] = 'none';
                let maxEndTime: Date | undefined;
                let minStartTime: Date | undefined;
                if (room && this.earlyCheckInIds.has(roomId)) {
                    constraintType = 'maxEndTime';
                    maxEndTime = new Date(this.year, this.month - 1, this.day, 10, 0, 0);
                } else if (room && this.lateCheckOutIds.has(roomId)) {
                    constraintType = 'minStartTime';
                    minStartTime = new Date(this.year, this.month - 1, this.day, 14, 0, 0);
                }

                return {
                    roomId,
                    roomNumber: room?.roomNumber || `Room ${roomId}`,
                    duration: Math.round(existingAssignment?.duration || duration),
                    taskType: taskType,
                    taskStatus: existingAssignment?.status || TaskStatus.PENDING,
                    constraintType: constraintType,
                    maxEndTime: maxEndTime,
                    minStartTime: minStartTime,
                    isSelected: true,
                };
            })
            .filter((c) => c !== null) as RoomConstraintFormModel[];
    }

    public async generateOptimizedSchedule(): Promise<void> {
        if (this.selectedEmployeeIds.size === 0 || this.selectedRoomIds.size === 0) {
            this.toastr.warning('Please select at least one employee and one room');
            return;
        }

        const constraints: TaskConstraintDto[] = this.roomConstraints.map((constraint) => {
            const dto: TaskConstraintDto = {
                roomId: constraint.roomId,
                duration: constraint.duration,
            };

            if (constraint.taskType) {
                dto.taskType = constraint.taskType;
            }
            if (constraint.taskStatus) {
                dto.taskStatus = constraint.taskStatus;
            }

            if (constraint.constraintType === 'maxEndTime' && constraint.maxEndTime) {
                dto.maxEndTime = this.toLocalDateTimeString(constraint.maxEndTime);
            } else if (constraint.constraintType === 'minStartTime' && constraint.minStartTime) {
                dto.minStartTime = this.toLocalDateTimeString(constraint.minStartTime);
            }

            return dto;
        });

        const [hours, minutes] = this.optimizationStartTime.split(':').map(Number);
        const startTimeDate = new Date(this.year, this.month - 1, this.day, hours, minutes);
        const startTime = this.toLocalDateTimeString(startTimeDate);

        const request: TaskScheduleSolveRequestDto = {
            startTime,
            employeeIds: Array.from(this.selectedEmployeeIds),
            roomIds: Array.from(this.selectedRoomIds),
            constraints,
        };

        const optimizedAssignments = await this.taskSchedulingService.fetchOptimizedTaskSchedule(request);

        if (this.error()) {
            // Display validation errors if present
            if (this.error()?.validationErrors && this.error()!.validationErrors!.length > 0) {
                this.error()!.validationErrors!.forEach((validationError) => {
                    this.toastr.error(validationError.message, 'Validation Error');
                });
            } else {
                this.toastr.error(this.error()?.message, this.error()?.error);
            }
            return;
        }

        if (optimizedAssignments.length === 0) {
            this.toastr.warning('No optimized schedule could be generated');
            return;
        }

        this.assignments = optimizedAssignments.map((a) => this.enrichAssignment(a));
        this.toastr.success('Optimized schedule generated successfully. Review and save to persist.');
        this.setViewMode('list');
        this.editing = true;
    }

    public selectAllEmployees(): void {
        this.selectedEmployeeIds = new Set(this.schedulableEmployees.map((e) => e.id));
    }

    public deselectAllEmployees(): void {
        this.selectedEmployeeIds.clear();
    }

    public selectAllRooms(): void {
        this.selectedRoomIds = new Set(this.rooms.map((r) => r.id));
        this.updateRoomConstraints();
    }

    public deselectAllRooms(): void {
        this.selectedRoomIds.clear();
        this.roomConstraints = [];
    }

    public setViewMode(mode: 'list' | 'optimize'): void {
        this.viewMode = mode;
        // Clear optimization selections when switching to list view
        if (mode === 'list') {
            this.selectedEmployeeIds.clear();
            this.selectedRoomIds.clear();
            this.roomConstraints = [];
        }
    }

    public canSaveSchedule(): boolean {
        return this.assignments.every((a) => a.taskType);
    }

    public formatDate(date: Date): string {
        const d = new Date(date);
        return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
    }

    public onAssignmentTimeChange(
        assignment: CleaningTaskExtended,
        field: 'scheduledStart' | 'scheduledEnd',
        value: string,
    ): void {
        const index = this.assignments.indexOf(assignment);
        if (index === -1) {
            return;
        }

        const [hours, minutes] = value.split(':').map(Number);
        // Create date on the selected day with the specified time
        const date = new Date(this.year, this.month - 1, this.day, hours, minutes, 0, 0);

        if (field === 'scheduledStart') {
            this.assignments[index].scheduledStart = date;
        } else {
            this.assignments[index].scheduledEnd = date;
        }

        const start = new Date(this.assignments[index].scheduledStart);
        const end = new Date(this.assignments[index].scheduledEnd);
        this.assignments[index].duration = Math.round((end.getTime() - start.getTime()) / (1000 * 60));
    }

    private toLocalDateTimeString(date: Date): string {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
    }

    private enrichAssignment(assignment: CleaningTaskDto): CleaningTaskExtended {
        const room = this.rooms.find((r) => r.id === assignment.roomId);
        const worker = this.schedulableEmployees.find((e) => e.id === assignment.employeeId);

        const startTime = new Date(assignment.scheduledStart);
        const endTime = new Date(assignment.scheduledEnd);
        const duration = Math.round((endTime.getTime() - startTime.getTime()) / (1000 * 60));

        return {
            ...assignment,
            roomNumber: room?.roomNumber || `Room ${assignment.roomId}`,
            employeeName: worker ? this.getEmployeeFullName(worker) : `Employee ${assignment.employeeId}`,
            duration,
            isEditing: false,
        };
    }

    private getEmployeeFullName(employee: SchedulableEmployee): string {
        return `${employee.firstName} ${employee.lastName}`;
    }
}
