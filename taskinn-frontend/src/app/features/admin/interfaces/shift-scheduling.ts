export interface SchedulableEmployee {
    id: number;
    firstName?: string;
    lastName?: string;
    schedulingRoleId?: number;
    schedulingRoleName?: string;
    hourlyWage?: number;
    hourlyOvertimeWage?: number;
    workingHoursPerWeek?: number;
}

export interface EmployeeMonthShift {
    employee?: SchedulableEmployee;
    dayIndices?: number[];
    ptoIndices?: number[];
}

export interface ShiftScheduleSolveRequest {
    year: number;
    month: number;
    employeeIds: number[];
    dailyHours: number[];
    staffingLevels: number[];
}

export interface ShiftScheduleTemplateContent {
    dailyHours?: number[];
    staffingLevels?: number[];
    associatedRole?: SchedulingRole;
}

export interface ShiftScheduleTemplate {
    id: number;
    name: string;
    content: ShiftScheduleTemplateContent;
}

export interface PackedShiftScheduleTemplate {
    id: number;
    name: string;
    content: string;
}

export interface SchedulingRole {
    id: number;
    name?: string;
    employees?: SchedulableEmployee[];
}
