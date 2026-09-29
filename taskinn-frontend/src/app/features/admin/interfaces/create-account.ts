export interface SignUpRequest {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    roles: string[];
    hourlyWage: number;
    hourlyOvertimeWage: number;
    workingHoursPerWeek: number;
}
