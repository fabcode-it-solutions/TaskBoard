import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { EmployeeService } from '../employee.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { MatIcon } from '@angular/material/icon';
import { MandatoryMarkComponent } from '../../../shared/components/mandatory-mark/mandatory-mark.component';
import { MultiSelectDropdownComponent } from '../../../shared/components/multi-select-dropdown/multi-select-dropdown.component';

@Component({
    selector: 'tinn-staff-form',
    imports: [ReactiveFormsModule, MatIcon, PageHeaderComponent, MandatoryMarkComponent, MultiSelectDropdownComponent],
    templateUrl: './staff-form.component.html',
})
export class StaffFormComponent implements OnInit {
    private readonly employeeService = inject(EmployeeService);
    private readonly fb = inject(FormBuilder);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly toastr = inject(ToastrService);

    public readonly loading = this.employeeService.loading;
    public readonly error = this.employeeService.error;
    public readonly employee = this.employeeService.employee;

    private employeeId: string | null = null;

    public staffForm = this.fb.group({
        firstName: ['', [Validators.required]],
        lastName: ['', [Validators.required]],
        email: ['', [Validators.required, Validators.email]],
        active: [true, [Validators.required]],
        roles: [[] as string[], [Validators.required]],
        hourlyWage: [0, [Validators.min(0)]],
        hourlyOvertimeWage: [0, [Validators.min(0)]],
        workingHoursPerWeek: [0, [Validators.min(0)]],
    });

    public availableRoles: string[] = ['ADMIN', 'EMPLOYEE'];

    async ngOnInit(): Promise<void> {
        this.employeeId = this.route.snapshot.paramMap.get('id');
        if (this.employeeId) {
            await this.employeeService.getEmployeeById(this.employeeId);
            if (this.employee()) {
                this.staffForm.patchValue({
                    firstName: this.employee()?.firstName,
                    lastName: this.employee()?.lastName,
                    email: this.employee()?.email,
                    active: this.employee()?.active,
                    roles: this.employee()?.roles || [],
                    hourlyWage: this.employee()?.hourlyWage,
                    hourlyOvertimeWage: this.employee()?.hourlyOvertimeWage,
                    workingHoursPerWeek: this.employee()?.workingHoursPerWeek,
                });
            }
        }
    }

    public async onSubmit(): Promise<void> {
        if (this.staffForm.invalid) {
            this.staffForm.markAllAsTouched();
            return;
        }

        if (!this.employeeId) {
            this.toastr.error('Employee ID is missing', 'Error');
            return;
        }

        const formValue = this.staffForm.value;
        const employeeData = {
            firstName: formValue.firstName!,
            lastName: formValue.lastName!,
            email: formValue.email!,
            active: formValue.active!,
            roles: formValue.roles!,
            hourlyWage: formValue.hourlyWage!,
            hourlyOvertimeWage: formValue.hourlyOvertimeWage!,
            workingHoursPerWeek: formValue.workingHoursPerWeek!,
        };

        await this.employeeService.updateEmployee(this.employeeId, employeeData);

        if (!this.error()) {
            this.toastr.success('Staff member updated successfully', 'Success');
            await this.router.navigate(['/employee/all']);
            return;
        }

        if (this.error()) {
            const validationErrors = this.error()?.validationErrors;

            // If there are validation errors, show each one
            if (validationErrors && validationErrors.length > 0) {
                validationErrors.forEach((error) => {
                    this.toastr.error(error.message, 'Validation Error');

                    // Set form field errors
                    if (error.field === 'hourlyWage') {
                        this.staffForm.get('hourlyWage')?.setErrors({ invalidHourlyWage: true });
                    }
                    if (error.field === 'hourlyOvertimeWage') {
                        this.staffForm.get('hourlyOvertimeWage')?.setErrors({ invalidHourlyOvertimeWage: true });
                    }
                    if (error.field === 'workingHoursPerWeek') {
                        this.staffForm.get('workingHoursPerWeek')?.setErrors({ invalidWorkingHoursPerWeek: true });
                    }
                });
            } else {
                // Fallback to generic error message
                const errorMessage = this.error()?.message || 'An error occurred';
                this.toastr.error(errorMessage, 'Failed to update staff member');
            }

            this.staffForm.markAllAsTouched();
        }
    }

    public navigateToList(): void {
        this.router.navigate(['/employee/all']);
    }
}
