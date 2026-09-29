import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SettingListing, UserSettingsService } from '../../../shared/services/user-settings.service';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { MatIcon } from '@angular/material/icon';
import { SettingValue } from '../../../shared/interfaces/user-settings';

@Component({
    selector: 'tinn-user-settings',
    imports: [FormsModule, PageHeaderComponent, MatIcon],
    templateUrl: './user-settings.html',
})
export class UserSettingsComponent implements OnInit {
    private readonly router = inject(Router);
    private readonly toastr = inject(ToastrService);
    private readonly settingService = inject(UserSettingsService);

    public loading = this.settingService.loading;
    public error = this.settingService.error;

    public settings: SettingListing[] = [];

    public async ngOnInit(): Promise<void> {
        await this.reloadSettings();
    }

    public async reloadSettings(): Promise<void> {
        this.settings = await this.settingService.fetchSettingListings();
        if (this.error()) {
            this.toastr.error(this.error()?.message, this.error()?.error);
            this.router.navigate([]);
        }
        this.settings.sort((l, r) => {
            const [nl, nr] = [l, r].map((x) => x[1].namespace + x[1].name);
            if (nl < nr) {
                return -1;
            } else if (nl > nr) {
                return 1;
            } else {
                return 0;
            }
        });
    }

    public async saveSetting(index: number): Promise<void> {
        const [value, definition] = this.settings[index];
        await this.settingService.putSetting(definition.namespace, definition.name, value);
        (definition.value as SettingValue) = value;
    }

    public resetSetting(index: number): void {
        (this.settings[index][0] as SettingValue) = this.settings[index][1].value;
    }

    public settingChanged(index: number): boolean {
        return this.settings[index][0] !== this.settings[index][1].value;
    }

    public nullInputAttr<T>(x?: T): T | null {
        return x === undefined ? null : x;
    }
}
