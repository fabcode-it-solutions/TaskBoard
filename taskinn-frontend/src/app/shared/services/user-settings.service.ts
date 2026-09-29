import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal, WritableSignal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { firstValueFrom } from 'rxjs';
import {
    NamedSetting,
    PackedUserSetting,
    SettingKindType,
    SettingProps,
    SettingValue,
} from '../interfaces/user-settings';
import { ErrorResponse } from '../interfaces/error';
import { processError } from '../util/error.utils';

const DEFAULT_SETTINGS = {
    task_duration_estimator: {
        base_capacity: { kind: 'NUMBER', value: 10.0, min: -20.0, max: 20.0, step: 0.05 },
        base_single_bed: { kind: 'NUMBER', value: 0.0, min: -20.0, max: 20.0, step: 0.05 },
        base_double_bed: { kind: 'NUMBER', value: 0.0, min: -20.0, max: 20.0, step: 0.05 },
        base_type_checkout: { kind: 'NUMBER', value: 0.0, min: -20.0, max: 20.0, step: 0.05 },
        base_type_stayover: { kind: 'NUMBER', value: 0.0, min: -20.0, max: 20.0, step: 0.05 },
        base_type_deep: { kind: 'NUMBER', value: 0.0, min: -20.0, max: 20.0, step: 0.05 },
        min_duration: { kind: 'NUMBER', value: 5.0, min: 1.0, max: 60.0, step: 0.1 },
        mul_capacity: { kind: 'NUMBER', value: 0.0, min: -10.0, max: 10.0, step: 0.05 },
        mul_single_bed: { kind: 'NUMBER', value: 0.0, min: -10.0, max: 10.0, step: 0.05 },
        mul_double_bed: { kind: 'NUMBER', value: 0.0, min: -10.0, max: 10.0, step: 0.05 },
        mul_type_checkout: { kind: 'NUMBER', value: 0.0, min: -10.0, max: 10.0, step: 0.05 },
        mul_type_stayover: { kind: 'NUMBER', value: 0.0, min: -10.0, max: 10.0, step: 0.05 },
        mul_type_deep: { kind: 'NUMBER', value: 0.0, min: -10.0, max: 10.0, step: 0.05 },
    },
} as const;

type DefaultSettingsRecord = typeof DEFAULT_SETTINGS;
type NamespaceValueRecordOf<T extends Record<string, SettingProps>> = {
    [K in keyof T]?: SettingKindType[T[K]['kind']];
};
type SettingValueRecordOf<T extends Record<string, Record<string, SettingProps>>> = {
    [K in keyof T]?: NamespaceValueRecordOf<T[K]>;
};
export type SettingValueRecord = SettingValueRecordOf<DefaultSettingsRecord>;

type NamespaceListingOf<S extends string, T extends Record<string, SettingProps>> = {
    [K in keyof T & string]: [SettingKindType[T[K]['kind']], Readonly<NamedSetting<S, K> & SettingProps>];
}[keyof T & string];
type SettingListingOf<T extends Record<string, Record<string, SettingProps>>> = {
    [K in keyof T & string]: NamespaceListingOf<K, T[K]>;
}[keyof T & string];
export type SettingListing = SettingListingOf<DefaultSettingsRecord>;

@Injectable({ providedIn: 'root' })
export class UserSettingsService {
    private readonly http = inject(HttpClient);

    public loading: WritableSignal<boolean> = signal(false);
    public error: WritableSignal<ErrorResponse | null> = signal(null);

    private readonly userSettingsPath = `${environment.apiUrl}/user-settings`;

    public async fetchSettingListings(): Promise<SettingListing[]> {
        const savedSettings = await this.fetchAllSettings();
        if (this.error()) {
            return [];
        }
        const savedRecord = this.unpackSettings(savedSettings);
        const result = Object.entries(DEFAULT_SETTINGS).flatMap((x) => {
            const [namespace, settings] = x;
            const savedNamespace = savedRecord[namespace as keyof SettingValueRecord];
            return Object.entries(settings).map((y) => {
                const [name, setting] = y;
                const newSetting = { ...setting };
                if (savedNamespace && name in savedNamespace) {
                    (newSetting.value as SettingValue) = savedNamespace[
                        name as keyof typeof savedNamespace
                    ] as SettingValue;
                }
                const defn = { namespace, name, ...newSetting };
                const value =
                    savedNamespace && name in savedNamespace
                        ? savedNamespace[name as keyof typeof savedNamespace]
                        : setting.value;
                return [value, defn] as SettingListing;
            });
        });
        return result;
    }

    public async fetchSettings<S extends keyof SettingValueRecord>(
        namespace: S,
    ): Promise<SettingValueRecord[S] | void> {
        const url = `${this.userSettingsPath}/${namespace}`;
        try {
            this.loading.set(true);
            this.error.set(null);
            const savedSettings = await firstValueFrom(this.http.get<PackedUserSetting[]>(url));
            const savedRecord = this.unpackSettings(savedSettings);
            const savedNamespace = savedRecord[namespace] as Record<string, SettingValue> | undefined;
            const result: Record<string, SettingValue> = {};
            for (const [name, setting] of Object.entries(DEFAULT_SETTINGS[namespace])) {
                if (savedNamespace && name in savedNamespace) {
                    result[name] = savedNamespace[name];
                } else {
                    result[name] = setting.value;
                }
            }
            return result;
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to fetch user settings', url);
            this.error.set(errorResponse);
        } finally {
            this.loading.set(false);
        }
    }

    private async fetchAllSettings(): Promise<PackedUserSetting[]> {
        const url = `${this.userSettingsPath}/`;
        try {
            this.loading.set(true);
            this.error.set(null);
            return await firstValueFrom(this.http.get<PackedUserSetting[]>(url));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to fetch user settings', url);
            this.error.set(errorResponse);
            return [];
        } finally {
            this.loading.set(false);
        }
    }

    public async putSetting(namespace: string, name: string, value: SettingValue): Promise<void> {
        const url = `${this.userSettingsPath}/${namespace}/${name}`;
        const request: PackedUserSetting = {
            namespace,
            name,
            content: JSON.stringify(value),
        };
        try {
            this.loading.set(true);
            this.error.set(null);
            await firstValueFrom(this.http.put<void>(url, request));
        } catch (error: unknown) {
            const errorResponse = processError(error, 'Failed to put user settings', url);
            this.error.set(errorResponse);
        } finally {
            this.loading.set(false);
        }
    }

    private unpackSettings(packed: PackedUserSetting[]): SettingValueRecord {
        const result: Record<string, Record<string, SettingValue>> = {};
        for (const pack of packed) {
            (result[pack.namespace] ??= {})[pack.name] = JSON.parse(pack.content);
        }
        return result as SettingValueRecord;
    }
}
