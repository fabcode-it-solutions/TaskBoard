import { Injectable, inject } from '@angular/core';
import { Location } from '@angular/common';
import { Router, NavigationEnd, ActivatedRoute, NavigationExtras, Params } from '@angular/router';
import { filter } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Injectable({ providedIn: 'root' })
export class MobileNavigationService {
    private readonly location = inject(Location);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private history: string[] = [];

    constructor() {
        this.router.events
            .pipe(
                filter((event) => event instanceof NavigationEnd),
                takeUntilDestroyed(),
            )
            .subscribe((event: NavigationEnd) => {
                this.history.push(event.urlAfterRedirects);
            });
    }

    public back(fallbackUrl: string = '/mobile'): void {
        if (this.history.length > 1) {
            this.history.pop();
            this.location.back();
        } else {
            this.navigateTo(fallbackUrl);
        }
    }

    public navigateTo(url: string, extras?: NavigationExtras): void {
        const preservedParams = this.getCurrentPreservedParams();
        const mergedExtras: NavigationExtras = {
            ...extras,
            queryParams: {
                ...preservedParams,
                ...(extras?.queryParams || {}),
            },
        };

        this.router.navigate([url], mergedExtras);
    }

    public async navigateWithPreservedParams(commands: unknown[], extras?: NavigationExtras): Promise<boolean> {
        const preservedParams = this.getCurrentPreservedParams();
        const mergedExtras: NavigationExtras = {
            ...extras,
            queryParams: {
                ...preservedParams,
                ...(extras?.queryParams || {}),
            },
        };

        return this.router.navigate(commands, mergedExtras);
    }

    private getCurrentPreservedParams(): Params {
        const params: Params = {};
        const currentTime = this.route.snapshot.queryParamMap.get('currentTime');

        if (currentTime) {
            params['currentTime'] = currentTime;
        }

        return params;
    }
}
