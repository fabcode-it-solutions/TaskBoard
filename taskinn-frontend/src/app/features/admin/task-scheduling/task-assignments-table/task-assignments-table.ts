import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIcon } from '@angular/material/icon';
import { CleaningTaskExtended } from '../../interfaces/task-scheduling';
import { LightRoomDto } from '../../../room/room';
import { SchedulableEmployee } from '../../interfaces/shift-scheduling';

@Component({
    selector: 'tinn-task-assignments-table',
    templateUrl: './task-assignments-table.html',
    imports: [FormsModule, MatIcon],
})
export class TaskAssignmentsTableComponent {
    @Input() public assignments: CleaningTaskExtended[] = [];
    @Input() public rooms: LightRoomDto[] = [];
    @Input() public schedulableEmployees: SchedulableEmployee[] = [];
    @Input() public editing: boolean = false;
    @Input() public loading: boolean = false;
    @Input() public selectedDate: Date = new Date();

    @Output() public readonly addAssignment = new EventEmitter<void>();
    @Output() public readonly editAssignment = new EventEmitter<number>();
    @Output() public readonly saveAssignment = new EventEmitter<number>();
    @Output() public readonly removeAssignment = new EventEmitter<number>();
    @Output() public readonly assignmentTimeChange = new EventEmitter<{
        assignment: CleaningTaskExtended;
        field: 'scheduledStart' | 'scheduledEnd';
        value: string;
    }>();

    public onAddAssignment(): void {
        this.addAssignment.emit();
    }

    public onEditAssignment(index: number): void {
        this.editAssignment.emit(index);
    }

    public onSaveAssignment(index: number): void {
        this.saveAssignment.emit(index);
    }

    public onRemoveAssignment(index: number): void {
        this.removeAssignment.emit(index);
    }

    public onAssignmentTimeChange(
        assignment: CleaningTaskExtended,
        field: 'scheduledStart' | 'scheduledEnd',
        value: string,
    ): void {
        this.assignmentTimeChange.emit({ assignment, field, value });
    }

    public formatTime(date: Date): string {
        const d = new Date(date);
        return d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false });
    }

    public formatDate(date: Date): string {
        const d = new Date(date);
        return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
    }

    public getTimeInputValue(date: Date): string {
        const d = new Date(date);
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        return `${hours}:${minutes}`;
    }

    public formatTaskType(taskType?: string): string {
        if (!taskType) {
            return 'Pending';
        }
        switch (taskType) {
            case 'CHECKOUT_CLEAN':
                return 'Checkout Clean';
            case 'STAYOVER_CLEAN':
                return 'Stayover Clean';
            case 'DEEP_CLEAN':
                return 'Deep Clean';
            default:
                return taskType;
        }
    }

    public formatTaskStatus(status?: string): string {
        if (!status) {
            return 'Not specified';
        }
        switch (status) {
            case 'PENDING':
                return 'Pending';
            case 'IN_PROGRESS':
                return 'In Progress';
            case 'COMPLETED':
                return 'Completed';
            case 'BLOCKED':
                return 'Blocked';
            default:
                return status;
        }
    }

    public getStatusBadgeClass(status?: string): string {
        const baseClasses = 'px-2 py-1 rounded-full text-xs font-medium';
        if (!status) {
            return `${baseClasses} bg-neutral-100 text-neutral-600`;
        }
        switch (status) {
            case 'PENDING':
                return `${baseClasses} bg-warning-100 text-warning-700`;
            case 'IN_PROGRESS':
                return `${baseClasses} bg-brand-100 text-brand-700`;
            case 'COMPLETED':
                return `${baseClasses} bg-success-100 text-success-700`;
            case 'BLOCKED':
                return `${baseClasses} bg-error-100 text-error-700`;
            default:
                return `${baseClasses} bg-neutral-100 text-neutral-600`;
        }
    }

    public hasConstraintViolations(): boolean {
        return this.assignments.some((a) => a.constraintViolated);
    }
}
