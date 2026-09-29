import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../auth/auth.service';

type AuthState = 'loading' | 'success' | 'error';

@Component({
    selector: 'tinn-mobile-qr-auth',
    imports: [CommonModule, MatIcon, MatProgressSpinnerModule],
    templateUrl: './mobile-qr-auth.component.html',
})
export class MobileQrAuthComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly authService = inject(AuthService);

    public readonly state = signal<AuthState>('loading');
    public readonly errorMessage = signal<string>('');
    public readonly statusMessage = signal<string>('Waiting for confirmation...');

    public async ngOnInit(): Promise<void> {
        const token = this.route.snapshot.queryParamMap.get('token');

        if (!token) {
            this.errorMessage.set('No token provided. Please scan a valid QR code.');
            this.state.set('error');
            return;
        }

        await this.exchangeToken(token);
    }

    private async exchangeToken(token: string): Promise<void> {
        this.state.set('loading');
        this.statusMessage.set('Waiting for confirmation from your PC...');

        const success = await this.authService.exchangeQrToken(token);

        if (success) {
            this.state.set('success');
            this.statusMessage.set('Login successful! Redirecting...');
            setTimeout(() => {
                this.router.navigate(['/mobile']);
            }, 1500);
        } else {
            const error = this.authService.error();
            this.errorMessage.set(error?.message || 'Failed to authenticate. The QR code may be expired or invalid.');
            this.state.set('error');
        }
    }

    public goToLogin(): void {
        this.router.navigate(['/auth/login'], { queryParams: { returnUrl: '/mobile' } });
    }
}
