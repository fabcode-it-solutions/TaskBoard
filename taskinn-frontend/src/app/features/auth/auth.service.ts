import { inject, Injectable, WritableSignal } from '@angular/core';
import { SignInRequest, SignInResponse } from './interfaces/sign-in';
import { QrLoginConfirmRequest, QrTokenExchangeRequest, QrTokenResponse } from './interfaces/qr-token';
import { UserProfile } from './interfaces/user-profile';
import { environment } from '../../../environments/environment';
import { AuthStore } from './auth.store';
import { LocalStorageService } from '../../shared/services/local-storage.service';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ErrorResponse } from '../../shared/interfaces/error';
import { processError } from '../../shared/util/error.utils';
import { ForgotPasswordRequest, ResetPasswordRequest } from './interfaces/forgot-password';

@Injectable({ providedIn: 'root' })
export class AuthService {
    private readonly authStore: AuthStore = inject(AuthStore);
    private readonly http: HttpClient = inject(HttpClient);
    private readonly localStorageService: LocalStorageService = inject(LocalStorageService);

    private readonly authPath = `${environment.apiUrl}/auth`;

    public async signIn(signInRequest: SignInRequest): Promise<void> {
        try {
            this.authStore.setLoading(true);
            this.authStore.setError(null);

            const response = await firstValueFrom(
                this.http.post<SignInResponse>(`${this.authPath}/sign-in`, signInRequest),
            );

            this.localStorageService.setAuthToken(response.accessToken);
            await this.loadUserProfile();
            this.authStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to sign in', `${this.authPath}/sign-in`);
            this.authStore.setError(errorResponse);
            this.authStore.setLoading(false);
        }
    }

    public async loadUserProfile(): Promise<void> {
        try {
            const profile = await firstValueFrom(this.http.get<UserProfile>(`${this.authPath}/me`));
            this.authStore.setUserProfile(profile);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to load user profile', `${this.authPath}/me`);
            this.authStore.setError(errorResponse);
            this.authStore.setUserProfile(null);
        }
    }

    public async forgotPassword(forgetPasswordRequest: ForgotPasswordRequest): Promise<void> {
        try {
            this.authStore.setLoading(true);
            this.authStore.setError(null);

            await firstValueFrom(this.http.post<void>(`${this.authPath}/forgot-password`, forgetPasswordRequest));

            this.authStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to initiate password reset',
                `${this.authPath}/forgot-password`,
            );
            this.authStore.setError(errorResponse);
            this.authStore.setLoading(false);
        }
    }

    public async resetPassword(resetPasswordRequest: ResetPasswordRequest): Promise<void> {
        try {
            this.authStore.setLoading(true);
            this.authStore.setError(null);

            await firstValueFrom(this.http.post<void>(`${this.authPath}/reset-password`, resetPasswordRequest));

            this.authStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to reset password', `${this.authPath}/reset-password`);
            this.authStore.setError(errorResponse);
            this.authStore.setLoading(false);
        }
    }

    public get loading(): WritableSignal<boolean> {
        return this.authStore.loading;
    }

    public get error(): WritableSignal<ErrorResponse | null> {
        return this.authStore.error;
    }

    public async initializeAuth(): Promise<void> {
        const token = this.localStorageService.getAuthToken();
        if (token) {
            await this.loadUserProfile();
        }
    }

    public get userProfile(): WritableSignal<UserProfile | null> {
        return this.authStore.userProfile;
    }

    // TODO: Fully implement when backend has logout method
    public logout(): void {
        this.localStorageService.removeAuthToken();
        this.authStore.setUserProfile(null);
        this.authStore.setError(null);
    }

    public getToken(): string | null {
        return this.localStorageService.getAuthToken();
    }

    public async generateQrToken(): Promise<QrTokenResponse | null> {
        try {
            this.authStore.setLoading(true);
            this.authStore.setError(null);

            const response = await firstValueFrom(
                this.http.post<QrTokenResponse>(`${this.authPath}/mobile/qr-token`, {}),
            );

            this.authStore.setLoading(false);
            return response;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to generate QR token',
                `${this.authPath}/mobile/qr-token`,
            );
            this.authStore.setError(errorResponse);
            this.authStore.setLoading(false);
            return null;
        }
    }

    public async confirmQrLogin(sessionId: string, confirmed: boolean): Promise<void> {
        try {
            this.authStore.setLoading(true);
            this.authStore.setError(null);

            const request: QrLoginConfirmRequest = { sessionId, confirmed };
            await firstValueFrom(this.http.post<void>(`${this.authPath}/mobile/confirm`, request));

            this.authStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to confirm QR login', `${this.authPath}/mobile/confirm`);
            this.authStore.setError(errorResponse);
            this.authStore.setLoading(false);
        }
    }

    public async exchangeQrToken(token: string): Promise<boolean> {
        try {
            this.authStore.setLoading(true);
            this.authStore.setError(null);

            const request: QrTokenExchangeRequest = { token };
            const response = await firstValueFrom(
                this.http.post<SignInResponse>(`${this.authPath}/mobile/exchange`, request),
            );

            this.localStorageService.setAuthToken(response.accessToken);
            await this.loadUserProfile();
            this.authStore.setLoading(false);
            return true;
        } catch (error: unknown) {
            const errorResponse = processError(
                error,
                'Failed to exchange QR token',
                `${this.authPath}/mobile/exchange`,
            );
            this.authStore.setError(errorResponse);
            this.authStore.setLoading(false);
            return false;
        }
    }
}
