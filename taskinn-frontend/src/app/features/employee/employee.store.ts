import { Injectable, signal, WritableSignal } from '@angular/core';
import { ErrorResponse } from '../../shared/interfaces/error';
import { EmployeeResponse } from './interfaces/employee';

@Injectable({ providedIn: 'root' })
export class EmployeeStore {
    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);
    public employee: WritableSignal<EmployeeResponse | null> = signal(null);
    public employeeList: WritableSignal<EmployeeResponse[] | null> = signal(null);

    public setLoading(loading: boolean): void {
        this.loading.set(loading);
    }

    public setError(error: ErrorResponse | null): void {
        this.error.set(error);
    }

    public setEmployee(employee: EmployeeResponse | null): void {
        this.employee.set(employee);
    }

    public setEmployeeList(employeeList: EmployeeResponse[] | null): void {
        this.employeeList.set(employeeList);
    }
}
