import { inject, Injectable, WritableSignal } from '@angular/core';
import { EmployeeStore } from './employee.store';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { processError } from '../../shared/util/error.utils';
import { firstValueFrom } from 'rxjs';
import { EmployeeResponse } from './interfaces/employee';
import { ErrorResponse } from '../../shared/interfaces/error';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
    private readonly employeeStore: EmployeeStore = inject(EmployeeStore);
    private readonly http = inject(HttpClient);

    private readonly employeePath = `${environment.apiUrl}/employees`;

    public async getAllStaff(): Promise<void> {
        try {
            this.employeeStore.setLoading(true);
            this.employeeStore.setError(null);

            const employees = await firstValueFrom(this.http.get<Array<EmployeeResponse>>(`${this.employeePath}`));
            this.employeeStore.setEmployeeList(employees);
            this.employeeStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to load staff members', `${this.employeePath}`);
            this.employeeStore.setError(errorResponse);
            this.employeeStore.setLoading(false);
        }
    }

    public async getEmployeeById(employeeId: string): Promise<void> {
        try {
            this.employeeStore.setLoading(true);
            this.employeeStore.setError(null);

            const employee = await firstValueFrom(
                this.http.get<EmployeeResponse>(`${this.employeePath}/${employeeId}`),
            );
            this.employeeStore.setEmployee(employee);
            this.employeeStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to load employee with ID ${employeeId}`,
                `${this.employeePath}/${employeeId}`,
            );
            this.employeeStore.setError(errorResponse);
            this.employeeStore.setLoading(false);
        }
    }

    public async updateEmployee(employeeId: string, employeeData: Partial<EmployeeResponse>): Promise<void> {
        try {
            this.employeeStore.setLoading(true);
            this.employeeStore.setError(null);

            const updatedEmployee = await firstValueFrom(
                this.http.put<EmployeeResponse>(`${this.employeePath}/${employeeId}`, employeeData),
            );
            this.employeeStore.setEmployee(updatedEmployee);
            this.employeeStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                `Failed to update employee with ID ${employeeId}`,
                `${this.employeePath}/${employeeId}`,
            );
            this.employeeStore.setError(errorResponse);
            this.employeeStore.setLoading(false);
        }
    }

    public get loading(): WritableSignal<boolean> {
        return this.employeeStore.loading;
    }

    public get error(): WritableSignal<ErrorResponse | null> {
        return this.employeeStore.error;
    }

    public get employeeList(): WritableSignal<EmployeeResponse[] | null> {
        return this.employeeStore.employeeList;
    }

    public get employee(): WritableSignal<EmployeeResponse | null> {
        return this.employeeStore.employee;
    }
}
