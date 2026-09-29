export interface EmployeeResponse {
    id: string;
    firstName: string;
    lastName: string;
    email: string;
    active: boolean;
    roles: string[];
    hourlyWage: number;
    hourlyOvertimeWage: number;
    workingHoursPerWeek: number;
}
