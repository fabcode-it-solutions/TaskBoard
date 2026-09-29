import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { SignInRequest } from '../interfaces/sign-in';
import { ToastrService } from 'ngx-toastr';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

@Component({
    selector: 'tinn-login-component',
    imports: [MatCardModule, MatIcon, ReactiveFormsModule, PageHeaderComponent, RouterLink],
    templateUrl: './login-component.html',
    styleUrl: './login-component.scss',
})
export class LoginComponent {
    private readonly authService = inject(AuthService);
    private readonly fb = inject(FormBuilder);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly toastr = inject(ToastrService);
    private readonly returnUrl = this.route.snapshot.queryParams['returnUrl'] || null;

    public readonly loading = this.authService.loading;
    public readonly error = this.authService.error;

    public isPasswordVisible = signal(false);

    public loginForm: FormGroup = this.fb.group({
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(6)]],
    });

    constructor() {
        // Clear custom errors when user types
        this.loginForm.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
            if (this.loginForm.get('email')?.hasError('invalidCredentials')) {
                this.loginForm.get('email')?.setErrors(null);
            }
            if (this.loginForm.get('password')?.hasError('invalidCredentials')) {
                this.loginForm.get('password')?.setErrors(null);
            }
            // Also clear auth service error when user makes changes
            if (this.error()) {
                this.error.set(null);
            }
        });
    }

    public async onSubmit(): Promise<void> {
        if (this.loginForm.invalid) {
            this.loginForm.markAllAsTouched();
            return;
        }

        const credentials: SignInRequest = this.loginForm.value;
        await this.authService.signIn(credentials);

        if (!this.error()) {
            if (this.returnUrl && !this.returnUrl.startsWith('/auth') && this.returnUrl !== '/') {
                await this.router.navigateByUrl(this.returnUrl);
                return;
            }

            if (this.authService.userProfile()?.roles.includes('ADMIN')) {
                await this.router.navigate(['/admin/overview-dashboard']);
                return;
            }
            if (this.authService.userProfile()?.roles.includes('EMPLOYEE')) {
                await this.router.navigate(['/staff/dashboard']);
                return;
            }

            // Fallback to staff dashboard if no specific role matched (race condition)
            await this.router.navigate(['/staff/dashboard']);
            return;
        }
        if (this.error()) {
            const errorMessage = this.error()?.message || 'An error occurred';
            this.toastr.error(errorMessage, 'Login Failed');
            this.loginForm.get('email')?.setErrors({ invalidCredentials: true });
            this.loginForm.get('password')?.setErrors({ invalidCredentials: true });
            this.loginForm.markAllAsTouched();
        }
    }

    public togglePasswordVisibility(): void {
        this.isPasswordVisible.set(!this.isPasswordVisible());
    }
}
