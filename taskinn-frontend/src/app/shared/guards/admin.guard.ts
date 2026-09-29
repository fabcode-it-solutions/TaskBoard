import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../../features/auth/auth.store';

export const adminGuard: CanActivateFn = (route, state) => {
    const authStore = inject(AuthStore);
    const router = inject(Router);

    const userProfile = authStore.userProfile();

    if (!userProfile) {
        router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
        return false;
    }

    if (!userProfile.roles.includes('ADMIN')) {
        // TODO: navigate to staff dashboard
        router.navigate(['/']);
        return false;
    }

    return true;
};
