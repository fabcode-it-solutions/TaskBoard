import { Injectable, signal, WritableSignal } from '@angular/core';
import { UserProfile } from './interfaces/user-profile';
import { ErrorResponse } from '../../shared/interfaces/error';

@Injectable({ providedIn: 'root' })
export class AuthStore {
    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);
    public userProfile: WritableSignal<UserProfile | null> = signal(null);

    public setLoading(loading: boolean): void {
        this.loading.set(loading);
    }

    public setError(error: ErrorResponse | null): void {
        this.error.set(error);
    }

    public setUserProfile(profile: UserProfile | null): void {
        this.userProfile.set(profile);
    }
}
