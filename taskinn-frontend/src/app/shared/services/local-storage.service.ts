import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LocalStorageService {
    public setItem(key: string, value: string): void {
        localStorage.setItem(key, value);
    }

    public getItem(key: string): string | null {
        return localStorage.getItem(key);
    }

    public removeItem(key: string): void {
        localStorage.removeItem(key);
    }

    public clear(): void {
        localStorage.clear();
    }

    public setAuthToken(token: string): void {
        this.setItem('auth_token', token);
    }

    public getAuthToken(): string | null {
        return this.getItem('auth_token');
    }

    public removeAuthToken(): void {
        this.removeItem('auth_token');
    }
}
