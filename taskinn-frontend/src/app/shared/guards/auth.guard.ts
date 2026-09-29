import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../../features/auth/auth.store';

export const authGuard: CanActivateFn = (route, state) => {
    const authStore = inject(AuthStore);
    const router = inject(Router);

    const userProfile = authStore.userProfile();

    if (!userProfile) {
        router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
        return false;
    }

    return true;
};
