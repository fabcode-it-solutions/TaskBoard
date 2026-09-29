import { Component, inject, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ShiftSchedulingService } from '../shift-scheduling.service';
import { Router } from '@angular/router';
import {
    EmployeeMonthShift,
    SchedulableEmployee,
    ShiftScheduleSolveRequest,
    ShiftScheduleTemplate,
} from '../interfaces/shift-scheduling';
import { FormsModule } from '@angular/forms';
import { MatIcon } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

interface EmployeeScheduleData {
    employee: SchedulableEmployee;
    // 0: Not assigned, 1: Assigned, 2: PTO
    schedule: number[];
}

interface RoleData {
    id: number;
    name: string;
    employeeIndices: number[];
}

@Component({
    selector: 'tinn-shift-scheduling',
    imports: [FormsModule, MatIcon, PageHeaderComponent],
    templateUrl: './shift-scheduling.html',
    styleUrl: './shift-scheduling.scss',
})
export class ShiftSchedulingComponent implements OnInit {
    private readonly toastr = inject(ToastrService);
    private readonly router = inject(Router);
    private readonly schedulingService = inject(ShiftSchedulingService);

    public readonly monthNames: string[] = [
        'January',
        'February',
        'March',
        'April',
        'May',
        'June',
        'July',
        'August',
        'September',
        'October',
        'November',
        'December',
    ];
    public readonly dayNames: string[] = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'So'];

    public loading = this.schedulingService.loading;
    public error = this.schedulingService.error;
    public year: number = 2024;
    public month: number = 1;
    public daysInMonth: number = 31;
    public weekdays: number[] = [];
    public dailyHours: number[] = [];
    public staffingLevels: number[] = [];
    public monthlyShifts: Map<number, EmployeeMonthShift> = new Map();
    public schedulableEmployees: SchedulableEmployee[] = [];
    public roles: RoleData[] = [];
    public roleIndexMap: Map<number, number> = new Map();
    public templates: ShiftScheduleTemplate[] = [];
    public employees: EmployeeScheduleData[] = [];
    public addedEmployeeIds: Set<number> = new Set();
    public editing: boolean = false;

    public async ngOnInit(): Promise<void> {
        const today = new Date();
        this.year = today.getFullYear();
        this.month = today.getMonth() + 1;
        await this.loadSchedulableEmployees();
        await this.loadMonth();
        await this.loadShiftTemplates();
    }

    public async loadSchedulableEmployees(): Promise<void> {
        const employees = await this.schedulingService.fetchSchedulableEmployees();
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
            this.router.navigate([]);
        }
        employees.sort(this.schedulingService.compareEmployeeNames);

        const roleMap: Map<number, RoleData> = new Map();
        for (let i = 0; i < employees.length; i++) {
            const employee = employees[i];
            if (!employee.schedulingRoleId || !employee.schedulingRoleName) {
                continue;
            }
            if (!roleMap.has(employee.schedulingRoleId)) {
                roleMap.set(employee.schedulingRoleId, {
                    id: employee.schedulingRoleId,
                    name: employee.schedulingRoleName,
                    employeeIndices: [],
                });
            }
            roleMap.get(employee.schedulingRoleId)!.employeeIndices.push(i);
        }

        this.schedulableEmployees = employees;
        this.roles = Array.from(roleMap.values());
        for (let i = 0; i < this.roles.length; i++) {
            const role = this.roles[i];
            this.roleIndexMap.set(role.id, i);
        }
    }

    public async loadShiftTemplates(): Promise<void> {
        const templates = await this.schedulingService.fetchAllShiftTemplates();
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
            return;
        }
        this.templates = templates;
    }

    public async stepMonth(direction: number): Promise<void> {
        if (this.month === 1 && direction < 0) {
            if (this.year === 2024) {
                return;
            }
            this.month = 12;
            this.year -= 1;
        } else if (this.month === 12 && direction > 0) {
            this.month = 1;
            this.year += 1;
        } else {
            this.month -= direction > 0 ? -1 : 1;
        }
        this.loadMonth();
    }

    public async updateMonthModel(stringValue: string): Promise<void> {
        const month = Number.parseInt(stringValue);
        if (Number.isNaN(month)) {
            this.month = 1;
        } else {
            this.month = month;
        }
        this.loadMonth();
    }

    public async loadMonth(): Promise<void> {
        if (this.year < 2024) {
            this.year = 2024;
        }
        if (this.month <= 0) {
            this.month = 1;
        }
        if (this.month > 12) {
            this.month = 12;
        }
        this.daysInMonth = new Date(this.year, this.month, 0).getDate();

        this.weekdays = new Array(this.daysInMonth);
        const dayDate = new Date(this.year, this.month - 1, 1);
        const convertWeekday = (x: number): number => (x === 0 ? 6 : x - 1);
        for (let day = 0; day < this.daysInMonth; day++) {
            this.weekdays[day] = convertWeekday(dayDate.getDay());
            dayDate.setDate(dayDate.getDate() + 1);
        }

        this.dailyHours = Array(this.daysInMonth).fill(8);
        this.staffingLevels = Array(this.daysInMonth).fill(0);

        this.monthlyShifts.clear();
        const shifts = await this.schedulingService.fetchShiftsForMonth(this.year, this.month);
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
            return;
        }
        for (const shift of shifts) {
            if (shift.employee?.id) {
                this.monthlyShifts.set(shift.employee?.id, shift);
            }
        }
        for (const employee of this.employees) {
            employee.schedule = this.generateEmployeeSchedule(employee.employee.id);
        }
    }

    public async optimizeShiftSchedule(): Promise<void> {
        for (let i = 0; i < this.weekdays.length; i++) {
            this.dailyHours[i] = Math.min(24, Math.max(0, this.dailyHours[i]));
            this.staffingLevels[i] = Math.min(this.employees.length, Math.max(0, this.staffingLevels[i]));
        }
        const request: ShiftScheduleSolveRequest = {
            year: this.year,
            month: this.month,
            employeeIds: this.employees.map((x) => x.employee.id),
            dailyHours: this.dailyHours,
            staffingLevels: this.staffingLevels,
        };
        const response = await this.schedulingService.fetchOptimizedShiftSchedule(request);
        if (this.error()) {
            this.toastr.warning(this.error()?.message, this.error()?.error);
            return;
        }
        const newShifts: Map<number, number[]> = new Map();
        for (const shift of response) {
            if (shift.employee && shift.dayIndices) {
                newShifts.set(shift.employee.id, shift.dayIndices);
            }
        }

        // eslint-disable-next-line @typescript-eslint/prefer-for-of
        for (let i = 0; i < this.employees.length; i++) {
            const employeeId = this.employees[i].employee.id;
            // eslint-disable-next-line @typescript-eslint/prefer-for-of
            for (let j = 0; j < this.weekdays.length; j++) {
                const schedule = this.employees[i].schedule[j];
                if (schedule <= 1) {
                    this.employees[i].schedule[j] = 0;
                }
            }
            const newIndices = newShifts.get(employeeId);
            if (newIndices) {
                for (const index of newIndices) {
                    this.employees[i].schedule[index] = 1;
                }
            }
        }
        this.toastr.info('Schedule has been generated');
    }

    public async saveShiftSchedule(): Promise<void> {
        if (this.employees.length === 0) {
            return;
        }
        const request: EmployeeMonthShift[] = [];
        for (const employee of this.employees) {
            const monthShift: EmployeeMonthShift = {
                employee: employee.employee,
                dayIndices: [],
                ptoIndices: [],
            };
            for (let i = 0; i < employee.schedule.length; i++) {
                if (employee.schedule[i] === 1) {
                    monthShift.dayIndices?.push(i);
                }
            }
            request.push(monthShift);
        }
        await this.schedulingService.putShiftScheduleForMonth(this.year, this.month, request);
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
            return;
        }
        this.toastr.success('Saved shifts');
    }

    public loadTemplateFromEvent(event: Event): void {
        const index = Number.parseInt((<HTMLSelectElement>event.target).value);
        (<HTMLSelectElement>event.target).value = '-1'; // eslint-disable-line no-param-reassign
        this.loadTemplate(index);
    }
    public loadTemplate(index: number): void {
        if (Number.isNaN(index) || index < 0) {
            return;
        }
        const template = this.templates[index];
        if (template.content.associatedRole) {
            const roleId = template.content.associatedRole.id;
            const roleIndex = this.roleIndexMap.get(roleId);
            if (roleIndex !== undefined) {
                this.selectByRole(roleIndex);
            }
        }
        if (template.content.dailyHours) {
            for (let i = 0; i < this.weekdays.length; i++) {
                const weekday = this.weekdays[i];
                this.dailyHours[i] = template.content.dailyHours[weekday];
            }
        }
        if (template.content.staffingLevels) {
            for (let i = 0; i < this.weekdays.length; i++) {
                const weekday = this.weekdays[i];
                this.staffingLevels[i] = Math.min(template.content.staffingLevels[weekday], this.employees.length);
            }
        }
    }

    public selectByRoleOnEvent(event: Event): void {
        const index = Number.parseInt((<HTMLSelectElement>event.target).value);
        (<HTMLSelectElement>event.target).value = '-1'; // eslint-disable-line no-param-reassign
        this.selectByRole(index);
    }
    public selectByRole(index: number): void {
        if (Number.isNaN(index) || index < 0) {
            return;
        }
        this.removeAllEmployees();

        const role = this.roles[index];
        for (const employeeIndex of role.employeeIndices) {
            this.addEmployee(employeeIndex);
        }
    }

    public addEmployeeOnEvent(event: Event): void {
        const index = Number.parseInt((<HTMLSelectElement>event.target).value);
        (<HTMLSelectElement>event.target).value = '-1'; // eslint-disable-line no-param-reassign
        this.addEmployee(index);
    }
    public addEmployee(index: number): void {
        if (Number.isNaN(index) || index < 0) {
            return;
        }
        const employee = this.schedulableEmployees[index];
        if (this.addedEmployeeIds.has(employee.id)) {
            return;
        }
        this.addedEmployeeIds.add(employee.id);
        const schedule = this.generateEmployeeSchedule(employee.id);
        this.employees.push({
            employee: employee,
            schedule: schedule,
        });
        this.employees.sort((l, r) => this.schedulingService.compareEmployeeNames(l.employee, r.employee));
        for (let i = 0; i < this.staffingLevels.length; i++) {
            this.staffingLevels[i] = 0;
        }
    }

    public removeEmployee(index: number): void {
        const employee = this.employees[index];
        this.employees.splice(index, 1);
        this.addedEmployeeIds.delete(employee.employee.id);
        for (let i = 0; i < this.staffingLevels.length; i++) {
            this.staffingLevels[i] = 0;
        }
    }

    public removeAllEmployees(): void {
        this.employees = [];
        this.addedEmployeeIds.clear();
        for (let i = 0; i < this.staffingLevels.length; i++) {
            this.staffingLevels[i] = 0;
        }
    }

    public switchEmployeeAssignment(employeeIndex: number, dayIndex: number): void {
        if (this.loading() || !this.editing) {
            return;
        }
        const schedule = this.employees[employeeIndex].schedule[dayIndex];
        let result = schedule;
        if (schedule === 0) {
            result = 1;
        } else if (schedule === 1) {
            result = 0;
        }
        this.employees[employeeIndex].schedule[dayIndex] = result;
    }

    public generateEmployeeSchedule(id: number): number[] {
        const result = Array(this.daysInMonth).fill(0);
        const shift = this.monthlyShifts.get(id);
        if (shift?.dayIndices) {
            for (const i of shift.dayIndices) {
                result[i] = 1;
            }
        }
        if (shift?.ptoIndices) {
            for (const i of shift.ptoIndices) {
                result[i] = 2;
            }
        }
        return result;
    }

    public computeScheduleSymbol(value: number): string {
        if (value === 0) {
            return this.editing ? 'remove' : '';
        } else if (value === 1) {
            return 'check';
        } else {
            return 'block';
        }
    }

    public async openPdf(): Promise<void> {
        const pdfBlob = await this.schedulingService.fetchShiftSchedulePdf(this.year, this.month);
        if (pdfBlob) {
            const url = window.URL.createObjectURL(pdfBlob);
            window.open(url, '_blank');
            setTimeout(() => window.URL.revokeObjectURL(url), 10000);
        } else {
            this.toastr.error('Failed to generate PDF, please try again later');
        }
    }
}
