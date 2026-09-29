import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { ToastrService } from 'ngx-toastr';
import { MandatoryMarkComponent } from '../../../shared/components/mandatory-mark/mandatory-mark.component';
import { AuthService } from '../auth.service';
import { ForgotPasswordRequest } from '../interfaces/forgot-password';

@Component({
    selector: 'tinn-forgot-password',
    imports: [ReactiveFormsModule, PageHeaderComponent, RouterLink, MandatoryMarkComponent],
    templateUrl: './forgot-password.component.html',
})
export class ForgotPasswordComponent {
    private readonly fb = inject(FormBuilder);
    private readonly router = inject(Router);
    private readonly toastr = inject(ToastrService);
    private readonly authService = inject(AuthService);

    public readonly loading = this.authService.loading;
    public readonly error = this.authService.error;

    public forgotPasswordForm: FormGroup = this.fb.group({
        email: ['', [Validators.required, Validators.email]],
    });

    public async onSubmit(): Promise<void> {
        if (this.forgotPasswordForm.invalid) {
            this.forgotPasswordForm.markAllAsTouched();
            return;
        }

        this.loading.set(true);
        const request: ForgotPasswordRequest = this.forgotPasswordForm.value;
        await this.authService.forgotPassword(request);

        if (!this.error()) {
            this.toastr.success('Password reset link has been sent to your email', 'Success');
            await this.router.navigate(['/auth/login']);
        }

        if (this.error()) {
            const errorMessage = this.error()?.message || 'An error occurred';
            this.toastr.error(errorMessage, 'Forgot password failed');
            this.forgotPasswordForm.markAllAsTouched();
        }
    }
}
