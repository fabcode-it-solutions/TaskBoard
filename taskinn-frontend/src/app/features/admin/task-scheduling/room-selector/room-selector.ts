import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIcon } from '@angular/material/icon';
import { LightRoomDto } from '../../../room/room';
import { RoomConstraintFormModel } from '../../interfaces/task-scheduling';
import { MandatoryMarkComponent } from '../../../../shared/components/mandatory-mark/mandatory-mark.component';

@Component({
    selector: 'tinn-room-selector',
    templateUrl: './room-selector.html',
    imports: [FormsModule, MatIcon, MandatoryMarkComponent],
})
export class RoomSelectorComponent {
    @Input() public rooms: LightRoomDto[] = [];
    @Input() public selectedRoomIds: Set<number> = new Set();
    @Input() public roomConstraints: RoomConstraintFormModel[] = [];
    @Input() public loading: boolean = false;
    @Input() public year: number = new Date().getFullYear();
    @Input() public month: number = new Date().getMonth() + 1;
    @Input() public day: number = new Date().getDate();

    @Output() private readonly roomToggled = new EventEmitter<number>();
    @Output() private readonly selectAll = new EventEmitter<void>();
    @Output() private readonly deselectAll = new EventEmitter<void>();

    public expandedRoomIds: Set<number> = new Set();

    public toggleRoomSelection(roomId: number): void {
        this.roomToggled.emit(roomId);
    }

    public onSelectAll(): void {
        this.selectAll.emit();
    }

    public onDeselectAll(): void {
        this.deselectAll.emit();
    }

    public toggleExpanded(roomId: number): void {
        if (this.expandedRoomIds.has(roomId)) {
            this.expandedRoomIds.delete(roomId);
        } else {
            this.expandedRoomIds.add(roomId);
        }
    }

    public isExpanded(roomId: number): boolean {
        return this.expandedRoomIds.has(roomId);
    }

    public getConstraintForRoom(roomId: number): RoomConstraintFormModel | undefined {
        return this.roomConstraints.find((c) => c.roomId === roomId);
    }

    public getTimeInputValue(date: Date | undefined): string {
        if (!date) {
            return '';
        }
        const d = new Date(date);
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        return `${hours}:${minutes}`;
    }

    public onConstraintDateChange(
        constraint: RoomConstraintFormModel,
        field: 'maxEndTime' | 'minStartTime',
        value: string,
    ): void {
        const [hours, minutes] = value.split(':').map(Number);
        const date = new Date(this.year, this.month - 1, this.day, hours, minutes, 0, 0);

        const updatedConstraint = constraint;
        if (field === 'maxEndTime') {
            updatedConstraint.maxEndTime = date;
        } else {
            updatedConstraint.minStartTime = date;
        }
    }
}
