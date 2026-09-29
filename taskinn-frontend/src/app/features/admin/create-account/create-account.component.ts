import { Component, inject } from '@angular/core';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { MatIcon } from '@angular/material/icon';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MandatoryMarkComponent } from '../../../shared/components/mandatory-mark/mandatory-mark.component';
import { MultiSelectDropdownComponent } from '../../../shared/components/multi-select-dropdown/multi-select-dropdown.component';
import { AdminService } from '../admin.service';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { SignUpRequest } from '../interfaces/create-account';

@Component({
    selector: 'tinn-create-account',
    imports: [PageHeaderComponent, MatIcon, ReactiveFormsModule, MandatoryMarkComponent, MultiSelectDropdownComponent],
    templateUrl: './create-account.component.html',
})
export class CreateAccountComponent {
    private readonly adminService = inject(AdminService);
    private readonly fb = inject(FormBuilder);
    private readonly router = inject(Router);
    private readonly toastr = inject(ToastrService);

    public readonly loading = this.adminService.loading;
    private readonly error = this.adminService.error;

    public createAccountForm = this.fb.group({
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(6)]],
        firstName: ['', [Validators.required]],
        lastName: ['', [Validators.required]],
        roles: [[] as string[], [Validators.required]],
        hourlyWage: [0, [Validators.min(0)]],
        hourlyOvertimeWage: [0, [Validators.min(0)]],
        workingHoursPerWeek: [0, [Validators.min(0)]],
    });

    public availableRoles: string[] = ['ADMIN', 'EMPLOYEE'];

    public async onSubmit(): Promise<void> {
        if (this.createAccountForm.invalid) {
            this.createAccountForm.markAllAsTouched();
            return;
        }

        const accountData = this.createAccountForm.value as SignUpRequest;
        await this.adminService.createStaffAccount(accountData);

        if (!this.error()) {
            const roles = accountData.roles || [];
            const isAdmin = roles.includes('ADMIN');
            const accountType = isAdmin ? 'Admin' : 'Employee';

            this.toastr.success(`${accountType} account created successfully`, 'Success');
            await this.router.navigate(['/admin/manager-dashboard']);
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
                        this.createAccountForm.get('hourlyWage')?.setErrors({ invalidHourlyWage: true });
                    }
                    if (error.field === 'hourlyOvertimeWage') {
                        this.createAccountForm
                            .get('hourlyOvertimeWage')
                            ?.setErrors({ invalidHourlyOvertimeWage: true });
                    }
                    if (error.field === 'workingHoursPerWeek') {
                        this.createAccountForm
                            .get('workingHoursPerWeek')
                            ?.setErrors({ invalidWorkingHoursPerWeek: true });
                    }
                });
            } else {
                // Fallback to generic error message
                const errorMessage = this.error()?.message || 'An error occurred';
                this.toastr.error(errorMessage, 'Failed to create account');
            }

            this.createAccountForm.markAllAsTouched();
        }
    }

    public navigateToDashboard(): void {
        this.router.navigate(['/admin/manager-dashboard']);
    }
}
