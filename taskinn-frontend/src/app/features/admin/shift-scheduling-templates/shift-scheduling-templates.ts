import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ShiftSchedulingService } from '../shift-scheduling.service';
import { SchedulableEmployee, SchedulingRole, ShiftScheduleTemplate } from '../interfaces/shift-scheduling';
import { FormsModule } from '@angular/forms';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { MatIcon } from '@angular/material/icon';

@Component({
    selector: 'tinn-shift-scheduling-templates',
    imports: [FormsModule, PageHeaderComponent, MatIcon],
    templateUrl: './shift-scheduling-templates.html',
})
export class ShiftSchedulingTemplatesComponent implements OnInit {
    private readonly toastr = inject(ToastrService);
    private readonly router = inject(Router);
    private readonly schedulingService = inject(ShiftSchedulingService);

    public readonly dayNames: string[] = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

    public loading = this.schedulingService.loading;
    public error = this.schedulingService.error;

    public schedulableEmployees: SchedulableEmployee[] = [];

    // Scheduling roles
    public schedulingRoles: SchedulingRole[] = [];
    public lastSchedulingRoleNames: string[] = [];
    public schedulingRoleMap: Map<number, SchedulingRole> = new Map(); // Maps role id to role
    public openedRole: number = -1;

    // Scheduling templates
    public shiftTemplates: ShiftScheduleTemplate[] = [];
    public lastShiftTemplateNames: string[] = [];
    public shiftTemplateRoleId: number[] = [];
    public openedTemplate: number = -1;

    public async ngOnInit(): Promise<void> {
        this.loadSchedulableEmployees();
        this.reloadSchedulingRoles();
        this.reloadShiftTemplates();
    }

    public async loadSchedulableEmployees(): Promise<void> {
        const employees = await this.schedulingService.fetchSchedulableEmployees();
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
            this.router.navigate([]);
        }
        employees.sort(this.schedulingService.compareEmployeeNames);
        this.schedulableEmployees = employees;
    }

    public async reloadSchedulingRoles(): Promise<void> {
        const roles = await this.schedulingService.fetchSchedulingRoles();
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
            this.router.navigate([]);
        }

        this.schedulingRoleMap.clear();
        const roleIdSet: Set<number> = new Set();
        for (const role of roles) {
            this.schedulingRoleMap.set(role.id, role);
            roleIdSet.add(role.id);
        }

        for (let i = 0; i < this.shiftTemplateRoleId.length; i++) {
            if (!roleIdSet.has(this.shiftTemplateRoleId[i])) {
                this.shiftTemplateRoleId[i] = -1;
            }
        }

        this.schedulingRoles = roles;
        this.lastSchedulingRoleNames = this.schedulingRoles.map((x) => x.name ?? 'New Scheduling Role');
    }

    public async addSchedulingRole(): Promise<void> {
        const request: SchedulingRole = {
            id: 0,
            name: this.generateUniqueRoleName(),
            employees: [],
        };
        await this.schedulingService.addSchedulingRole(request);
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
        }
        await this.reloadSchedulingRoles();
        this.openedRole = this.schedulingRoles.length - 1;
    }

    private generateUniqueRoleName(): string {
        const baseName = 'New Scheduling Role';
        const existingNames = this.schedulingRoles.map((role) => role.name?.trim().toLowerCase() || '');

        if (!existingNames.includes(baseName.toLowerCase())) {
            return baseName;
        }

        let counter = 2;
        while (existingNames.includes(`${baseName} ${counter}`.toLowerCase())) {
            counter++;
        }
        return `${baseName} ${counter}`;
    }

    public async deleteSchedulingRole(index: number): Promise<void> {
        await this.schedulingService.deleteSchedulingRole(this.schedulingRoles[index].id);
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
        }
        this.reloadSchedulingRoles();
    }

    public async assignEmployeeOnEvent(roleIndex: number, event: Event): Promise<void> {
        const index = Number.parseInt((<HTMLSelectElement>event.target).value);
        (<HTMLSelectElement>event.target).value = '-1'; // eslint-disable-line no-param-reassign
        this.assignEmployee(roleIndex, index);
    }
    public async assignEmployee(roleIndex: number, employeeIndex: number): Promise<void> {
        if (Number.isNaN(employeeIndex) || employeeIndex < 0) {
            return;
        }
        const currentRole = this.schedulingRoles[roleIndex];
        const request: SchedulingRole = {
            id: currentRole.id,
            name: currentRole.name,
            employees: [...(currentRole.employees || []), this.schedulableEmployees[employeeIndex]],
        };
        await this.schedulingService.updateSchedulingRole(currentRole.id, request);
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
        }
        this.reloadSchedulingRoles();
    }

    public async unassignEmployee(roleIndex: number, employeeIndex: number): Promise<void> {
        const currentRole = this.schedulingRoles[roleIndex];
        const currentEmployees = currentRole.employees || [];
        const request: SchedulingRole = {
            id: currentRole.id,
            name: currentRole.name,
            employees: [
                ...currentEmployees.slice(0, employeeIndex),
                ...currentEmployees.slice(employeeIndex + 1, currentEmployees.length + 1),
            ],
        };
        await this.schedulingService.updateSchedulingRole(currentRole.id, request);
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
        }
        this.reloadSchedulingRoles();
    }

    public async updateRoleTitleOnEvent(roleIndex: number, event: Event): Promise<void> {
        let newTitle = (<HTMLInputElement>event.target).value;
        if (newTitle.length === 0) {
            newTitle = this.lastSchedulingRoleNames[roleIndex];
            (<HTMLInputElement>event.target).value = newTitle; // eslint-disable-line no-param-reassign
            this.toastr.warning('Role name cannot be empty');
            return;
        }
        if (this.isRoleNameDuplicate(roleIndex, newTitle)) {
            (<HTMLInputElement>event.target).value = this.lastSchedulingRoleNames[roleIndex]; // eslint-disable-line no-param-reassign
            this.toastr.error('A role with this name already exists');
            return;
        }
        this.updateRoleTitle(roleIndex, (<HTMLInputElement>event.target).value);
    }
    public async updateRoleTitle(roleIndex: number, newTitle: string): Promise<void> {
        const currentRole = this.schedulingRoles[roleIndex];
        const request: SchedulingRole = {
            id: currentRole.id,
            name: newTitle,
            employees: currentRole.employees,
        };
        await this.schedulingService.updateSchedulingRole(currentRole.id, request);
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
            this.reloadSchedulingRoles();
            return;
        }
        this.reloadSchedulingRoles();
    }

    public toggleOpenedRole(roleIndex: number): void {
        if (this.openedRole === roleIndex) {
            this.openedRole = -1;
        } else {
            this.openedRole = roleIndex;
        }
    }

    public isRoleNameDuplicate(roleIndex: number, name: string): boolean {
        const trimmedName = name.trim().toLowerCase();
        if (trimmedName.length === 0) {
            return false;
        }
        return this.schedulingRoles.some(
            (role, index) => index !== roleIndex && role.name?.trim().toLowerCase() === trimmedName,
        );
    }

    public updateShiftTemplateRoleIdModel(templateIndex: number, stringValue: string): void {
        const id = Number.parseInt(stringValue);
        if (!Number.isNaN(stringValue)) {
            this.shiftTemplateRoleId[templateIndex] = id;
        }
    }

    public async reloadShiftTemplates(showErrorToast: boolean = true): Promise<void> {
        const templates = await this.schedulingService.fetchAllShiftTemplates();
        if (this.error()) {
            if (showErrorToast) {
                this.toastr.error(this.error()?.message, this.error()?.error);
            }
            return;
        }
        this.shiftTemplateRoleId = [];
        for (const template of templates) {
            this.shiftTemplateRoleId.push((template.content.associatedRole ?? { id: -1 }).id);
        }
        this.shiftTemplates = templates;
        this.lastShiftTemplateNames = this.shiftTemplates.map((x) => x.name);
    }

    public async addShiftTemplate(): Promise<void> {
        await this.schedulingService.addShiftTemplate({
            id: 0,
            name: this.generateUniqueTemplateName(),
            content: {
                dailyHours: [0, 0, 0, 0, 0, 0, 0],
                staffingLevels: [0, 0, 0, 0, 0, 0, 0],
            },
        });
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
        }
        await this.reloadShiftTemplates();
        this.openedTemplate = this.shiftTemplates.length - 1;
    }

    private generateUniqueTemplateName(): string {
        const baseName = 'New Shift Template';
        const existingNames = this.shiftTemplates.map((template) => template.name.trim().toLowerCase());

        if (!existingNames.includes(baseName.toLowerCase())) {
            return baseName;
        }

        let counter = 2;
        while (existingNames.includes(`${baseName} ${counter}`.toLowerCase())) {
            counter++;
        }
        return `${baseName} ${counter}`;
    }

    public async deleteShiftTemplate(templateIndex: number): Promise<void> {
        await this.schedulingService.deleteShiftTemplate(this.shiftTemplates[templateIndex].id);
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
        }
        this.reloadShiftTemplates();
    }

    public async updateTemplateNameOnEvent(templateIndex: number, event: Event): Promise<void> {
        const newName = (<HTMLInputElement>event.target).value;
        this.shiftTemplates[templateIndex].name = newName;
    }

    public async saveShiftTemplate(templateIndex: number): Promise<void> {
        const template = this.shiftTemplates[templateIndex];
        for (let i = 0; i < this.dayNames.length; i++) {
            if (template.content.dailyHours) {
                template.content.dailyHours[i] = Math.min(24, Math.max(0, template.content.dailyHours[i]));
            }
            if (template.content.staffingLevels) {
                template.content.staffingLevels[i] = Math.max(0, template.content.staffingLevels[i]);
            }
        }
        if (template.name.length === 0) {
            template.name = this.lastShiftTemplateNames[templateIndex];
            this.toastr.warning('Template name cannot be empty');
            return;
        }
        if (this.isTemplateNameDuplicate(templateIndex, template.name)) {
            template.name = this.lastShiftTemplateNames[templateIndex];
            this.toastr.error('A template with this name already exists');
            return;
        }

        const roleId = this.shiftTemplateRoleId[templateIndex];
        if (!Number.isNaN(roleId)) {
            if (roleId >= 0) {
                const role = this.schedulingRoleMap.get(roleId);
                if (role) {
                    template.content.associatedRole = {
                        id: role.id,
                        name: role.name,
                    };
                }
            } else {
                template.content.associatedRole = undefined;
            }
        }
        await this.schedulingService.updateShiftTemplate(template.id, template);
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
            return;
        }
        await this.reloadShiftTemplates(false);
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
            return;
        }
        this.toastr.success('Template saved');
    }

    public toggleOpenedTemplate(templateIndex: number): void {
        if (this.openedTemplate === templateIndex) {
            const currentName = this.shiftTemplates[templateIndex].name;
            if (currentName.length === 0) {
                this.shiftTemplates[templateIndex].name = this.lastShiftTemplateNames[templateIndex];
                this.toastr.warning('Template name cannot be empty');
            } else if (this.isTemplateNameDuplicate(templateIndex, currentName)) {
                this.shiftTemplates[templateIndex].name = this.lastShiftTemplateNames[templateIndex];
                this.toastr.error('A template with this name already exists');
            }
            this.openedTemplate = -1;
        } else {
            this.openedTemplate = templateIndex;
        }
    }

    public isTemplateNameDuplicate(templateIndex: number, name: string): boolean {
        const trimmedName = name.trim().toLowerCase();
        if (trimmedName.length === 0) {
            return false;
        }
        return this.shiftTemplates.some(
            (template, index) => index !== templateIndex && template.name.trim().toLowerCase() === trimmedName,
        );
    }
}
