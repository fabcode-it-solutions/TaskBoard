import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { MobileHeaderComponent } from '../components/mobile-header/mobile-header';
import { AuthService } from '../../auth/auth.service';
import { MobileNavigationService } from '../mobile-navigation.service';

@Component({
    selector: 'tinn-mobile-landing-page',
    imports: [CommonModule, MatIcon, MobileHeaderComponent],
    templateUrl: './mobile-landing-page.component.html',
    styleUrl: './mobile-landing-page.component.scss',
})
export class MobileLandingPageComponent {
    private readonly router = inject(Router);
    private readonly authService = inject(AuthService);
    private readonly navigationService = inject(MobileNavigationService);

    public navigateToReport(): void {
        this.navigationService.navigateTo('/mobile/report');
    }

    public navigateToMap(): void {
        this.navigationService.navigateTo('/mobile/map');
    }

    public navigateToTaskList(): void {
        this.navigationService.navigateTo('/mobile/tasks');
    }

    public onLogout(): void {
        this.authService.logout();
        this.router.navigate(['/auth/login'], { queryParams: { returnUrl: '/mobile' } });
    }
}
