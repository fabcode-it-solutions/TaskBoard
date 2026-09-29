import { Component, EventEmitter, inject, Input, OnChanges, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SchedulableEmployee, SchedulingRole } from '../../interfaces/shift-scheduling';
import { ShiftSchedulingService } from '../../shift-scheduling.service';

@Component({
    selector: 'tinn-employee-selector',
    imports: [FormsModule],
    templateUrl: './employee-selector.html',
})
export class EmployeeSelectorComponent implements OnInit, OnChanges {
    private readonly shiftSchedulingService = inject(ShiftSchedulingService);

    @Input() public employees: SchedulableEmployee[] = [];
    @Input() public selectedEmployeeIds: Set<number> = new Set();
    @Input() public loading: boolean = false;

    @Output() private readonly employeeToggled = new EventEmitter<number>();
    @Output() private readonly selectAll = new EventEmitter<void>();
    @Output() private readonly deselectAll = new EventEmitter<void>();

    public schedulingRoles: SchedulingRole[] = [];
    public selectedRoleId: number | null = null;
    public filteredEmployees: SchedulableEmployee[] = [];

    public async ngOnInit(): Promise<void> {
        this.schedulingRoles = await this.shiftSchedulingService.fetchSchedulingRoles();
        this.updateFilteredEmployees();
    }

    public ngOnChanges(): void {
        this.updateFilteredEmployees();
    }

    public onRoleFilterChange(roleId: number | null): void {
        this.selectedRoleId = roleId;
        this.updateFilteredEmployees();
    }

    private updateFilteredEmployees(): void {
        if (this.selectedRoleId === null) {
            this.filteredEmployees = this.employees;
        } else {
            this.filteredEmployees = this.employees.filter((e) => e.schedulingRoleId === this.selectedRoleId);
        }
    }

    public toggleEmployeeSelection(employeeId: number): void {
        this.employeeToggled.emit(employeeId);
    }

    public onSelectAll(): void {
        this.filteredEmployees.forEach((employee) => {
            if (!this.selectedEmployeeIds.has(employee.id)) {
                this.employeeToggled.emit(employee.id);
            }
        });
    }

    public onDeselectAll(): void {
        this.filteredEmployees.forEach((employee) => {
            if (this.selectedEmployeeIds.has(employee.id)) {
                this.employeeToggled.emit(employee.id);
            }
        });
    }
}
