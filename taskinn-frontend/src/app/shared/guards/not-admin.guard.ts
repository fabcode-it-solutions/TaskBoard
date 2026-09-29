import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../../features/auth/auth.store';

export const notAdminGuard: CanActivateFn = (route, state) => {
    const authStore = inject(AuthStore);
    const router = inject(Router);

    const userProfile = authStore.userProfile();

    if (!userProfile) {
        return router.createUrlTree(['/auth/login'], { queryParams: { returnUrl: state.url } });
    }

    if (userProfile.roles.includes('ADMIN')) {
        return router.createUrlTree(['/']);
    }

    return true;
};
