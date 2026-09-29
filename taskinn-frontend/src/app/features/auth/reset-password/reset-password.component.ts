import { Component, inject, OnInit, signal } from '@angular/core';
import {
    AbstractControl,
    FormBuilder,
    FormGroup,
    ReactiveFormsModule,
    ValidationErrors,
    Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ResetPasswordRequest } from '../interfaces/forgot-password';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { MatIcon } from '@angular/material/icon';
import { MandatoryMarkComponent } from '../../../shared/components/mandatory-mark/mandatory-mark.component';
import { AuthService } from '../auth.service';

@Component({
    selector: 'tinn-reset-password-component',
    imports: [ReactiveFormsModule, PageHeaderComponent, MatIcon, MandatoryMarkComponent],
    templateUrl: './reset-password.component.html',
    styleUrl: './reset-password.component.scss',
})
export class ResetPasswordComponent implements OnInit {
    private readonly fb = inject(FormBuilder);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly toastr = inject(ToastrService);
    private readonly authService = inject(AuthService);

    public readonly error = this.authService.error;
    public readonly loading = this.authService.loading;
    public isPasswordVisible = signal(false);
    public isConfirmPasswordVisible = signal(false);

    private resetToken: string | null = null;

    public resetPasswordForm: FormGroup = this.fb.group(
        {
            password: ['', [Validators.required, Validators.minLength(6)]],
            confirmPassword: ['', [Validators.required]],
        },
        { validators: this.passwordMatchValidator },
    );

    ngOnInit(): void {
        this.resetToken = this.route.snapshot.queryParamMap.get('token');

        if (!this.resetToken) {
            this.toastr.error('Invalid or missing reset token', 'Error');
            this.router.navigate(['/auth/forgot-password']);
        }
    }

    public async onSubmit(): Promise<void> {
        if (this.resetPasswordForm.invalid) {
            this.resetPasswordForm.markAllAsTouched();
            return;
        }

        if (!this.resetToken) {
            this.toastr.error('Invalid reset token', 'Error');
            return;
        }

        this.loading.set(true);
        const request: ResetPasswordRequest = {
            token: this.resetToken,
            newPassword: this.resetPasswordForm.value.password,
        };

        await this.authService.resetPassword(request);

        if (!this.error()) {
            this.toastr.success('Your password has been reset successfully', 'Success');
            await this.router.navigate(['/auth/login']);
        }

        if (this.error()) {
            const errorMessage = this.error()?.message || 'An error occurred';
            this.toastr.error(errorMessage, 'Reset password failed');
            this.resetPasswordForm.markAllAsTouched();
        }
    }

    public togglePasswordVisibility(): void {
        this.isPasswordVisible.set(!this.isPasswordVisible());
    }

    public toggleConfirmPasswordVisibility(): void {
        this.isConfirmPasswordVisible.set(!this.isConfirmPasswordVisible());
    }

    private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
        const password = control.get('password')?.value;
        const confirmPassword = control.get('confirmPassword')?.value;

        if (password && confirmPassword && password !== confirmPassword) {
            control.get('confirmPassword')?.setErrors({ passwordMismatch: true });
            return { passwordMismatch: true };
        }

        return null;
    }
}
