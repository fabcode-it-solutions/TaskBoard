import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../../features/auth/auth.store';

export const unauthGuard: CanActivateFn = () => {
    const authStore = inject(AuthStore);
    const router = inject(Router);

    const userProfile = authStore.userProfile();

    if (userProfile) {
        if (userProfile.roles.includes('ADMIN')) {
            router.navigate(['/admin/overview-dashboard']);
        } else if (userProfile.roles.includes('EMPLOYEE')) {
            router.navigate(['/staff/dashboard']);
        }
        return false;
    }

    return true;
};
