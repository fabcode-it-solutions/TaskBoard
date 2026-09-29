import { inject, Injectable, WritableSignal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { SignUpRequest } from './interfaces/create-account';
import { AdminStore } from './admin.store';
import { firstValueFrom } from 'rxjs';
import { processError } from '../../shared/util/error.utils';
import { ErrorResponse } from '../../shared/interfaces/error';

@Injectable({
    providedIn: 'root',
})
export class AdminService {
    private readonly adminStore: AdminStore = inject(AdminStore);
    private readonly http: HttpClient = inject(HttpClient);

    private readonly adminPath = `${environment.apiUrl}/admin`;

    public async createStaffAccount(accountData: SignUpRequest): Promise<void> {
        try {
            this.adminStore.setLoading(true);
            this.adminStore.setError(null);

            await firstValueFrom(this.http.post<void>(`${this.adminPath}/account`, accountData));

            this.adminStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to create staff account', `${this.adminPath}/account`);
            this.adminStore.setError(errorResponse);
            this.adminStore.setLoading(false);
        }
    }

    public async resetDb(): Promise<void> {
        try {
            this.adminStore.setLoading(true);
            this.adminStore.setError(null);

            await firstValueFrom(this.http.post<void>(`${this.adminPath}/ops/reset-database`, {}));

            this.adminStore.setLoading(false);
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to reset database', `${this.adminPath}/reset-db`);
            this.adminStore.setError(errorResponse);
            this.adminStore.setLoading(false);
        }
    }

    public get loading(): WritableSignal<boolean> {
        return this.adminStore.loading;
    }

    public get error(): WritableSignal<ErrorResponse | null> {
        return this.adminStore.error;
    }
}
