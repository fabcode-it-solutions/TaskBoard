import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { LocalStorageService } from '../services/local-storage.service';
import { AuthService } from '../../features/auth/auth.service';
import { environment } from '../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const localStorageService = inject(LocalStorageService);
    const authService = inject(AuthService);
    const router = inject(Router);

    // Only add auth header for requests to the API
    if (!req.url.startsWith(environment.apiUrl)) {
        return next(req);
    }

    const token = localStorageService.getAuthToken();

    // If no token, proceed without adding header
    const authReq = token
        ? req.clone({
              setHeaders: {
                  Authorization: `Bearer ${token}`,
              },
          })
        : req;

    return next(authReq).pipe(
        catchError((error) => {
            // Handle 401 (Unauthorized) and 403 (Forbidden) errors
            if (error.status === 401 || error.status === 403) {
                // Clear auth state and redirect to login
                authService.logout();
                const currentUrl = router.url;
                router.navigate(['/auth/login'], { queryParams: { returnUrl: currentUrl } });
            }
            return throwError(() => error);
        }),
    );
};
