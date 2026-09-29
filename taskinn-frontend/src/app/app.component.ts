import { Component, computed, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { AuthStore } from './features/auth/auth.store';

@Component({
    selector: 'tinn-root',
    templateUrl: './app.component.html',
    imports: [RouterOutlet, SidebarComponent],
})
export class AppComponent {
    private readonly router = inject(Router);
    private readonly authStore = inject(AuthStore);

    private readonly currentRoute = toSignal(this.router.events.pipe(map(() => this.router.url)));

    public readonly showSidebar = computed(() => {
        const isAuthenticated = this.authStore.userProfile() !== null;
        const isMobilePage = this.isOnMobilePage();

        return isAuthenticated && !isMobilePage;
    });

    public readonly isOnMobilePage = computed(() => {
        const route = this.currentRoute() || this.router.url;

        return route.startsWith('/mobile');
    });
}
