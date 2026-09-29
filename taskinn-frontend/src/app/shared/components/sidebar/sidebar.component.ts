import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '../../../features/auth/auth.service';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../features/admin/admin.service';
import { QrLoginDialogComponent } from '../qr-login-dialog/qr-login-dialog.component';
import { ToastrService } from 'ngx-toastr';

interface NavItem {
    label: string;
    route: string;
    icon: string;
    roles?: string[];
}

@Component({
    selector: 'tinn-sidebar',
    imports: [CommonModule, RouterLink, RouterLinkActive, MatIcon],
    templateUrl: './sidebar.component.html',
})
export class SidebarComponent {
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);
    private readonly adminService = inject(AdminService);
    private readonly dialog = inject(MatDialog);
    private readonly toastr = inject(ToastrService);

    public readonly userProfile = this.authService.userProfile;

    public readonly fullName = computed(() => {
        const profile = this.userProfile();
        if (!profile) {
            return '';
        }
        return `${profile.firstName} ${profile.lastName}`;
    });

    public readonly initials = computed(() => {
        const profile = this.userProfile();
        if (!profile) {
            return '';
        }
        return `${profile.firstName.charAt(0)}${profile.lastName.charAt(0)}`.toUpperCase();
    });

    public readonly primaryRole = computed(() => {
        const profile = this.userProfile();
        if (!profile || profile.roles.length === 0) {
            return 'User';
        }
        return profile.roles[0].charAt(0).toUpperCase() + profile.roles[0].slice(1).toLowerCase();
    });

    public readonly navItems: NavItem[] = [
        { label: 'Overview Dashboard', route: '/admin/overview-dashboard', icon: 'insights', roles: ['ADMIN'] },
        { label: 'Live Room Dashboard', route: '/room-dashboard', icon: 'hotel', roles: ['EMPLOYEE', 'ADMIN'] },
        { label: 'Manager Dashboard', route: '/admin/manager-dashboard', icon: 'dashboard', roles: ['ADMIN'] },
        { label: 'Front-desk Dashboard', route: '/staff/dashboard', icon: 'store', roles: ['EMPLOYEE', 'ADMIN'] },
        { label: 'My Task Schedule', route: '/staff/task-schedule', icon: 'assignment', roles: ['EMPLOYEE'] },
        { label: 'Maintenance Board', route: '/maintenance/board', icon: 'build', roles: ['EMPLOYEE', 'ADMIN'] },
        { label: 'My Schedule', route: '/staff/my-schedule', icon: 'calendar_month', roles: ['EMPLOYEE'] },
        { label: 'Statistics Dashboard', route: '/statistics', icon: 'business', roles: ['ADMIN'] },
    ];

    public readonly filteredNavItems = computed(() => {
        const profile = this.userProfile();
        if (!profile) {
            return [];
        }

        return this.navItems.filter((item) => {
            if (!item.roles || item.roles.length === 0) {
                return true;
            }
            return item.roles.some((role) => profile.roles.includes(role));
        });
    });

    public readonly showMobileLogin = computed(() => {
        const profile = this.userProfile();
        if (!profile) {
            return false;
        }
        // Only show for employees who are not admins
        return profile.roles.includes('EMPLOYEE') && !profile.roles.includes('ADMIN');
    });

    public openMobileLogin(): void {
        this.dialog.open(QrLoginDialogComponent, {
            width: '450px',
            disableClose: false,
        });
    }

    public async onLogout(): Promise<void> {
        this.authService.logout();
        await this.router.navigate(['/auth/login']);
    }

    public async resetDatabase(): Promise<void> {
        const profile = this.userProfile();
        if (!profile?.roles.includes('ADMIN')) {
            return;
        }

        const confirmed = window.confirm('Are you sure you want to reset the database? This action cannot be undone.');
        if (!confirmed) {
            return;
        }

        const doubleConfirmed = window.confirm(
            'This is your last chance to cancel. Do you really want to reset the database?',
        );
        if (!doubleConfirmed) {
            return;
        }

        const toast = this.toastr.info('Resetting database. This may take a moment...', 'Please wait', {
            timeOut: 0,
            extendedTimeOut: 0,
            tapToDismiss: true,
            closeButton: true,
        });

        await this.adminService.resetDb().catch((error) => {
            console.error('Error resetting database:', error);
        });

        this.toastr.remove(toast.toastId);
        this.toastr.success('Database has been reset successfully.', 'Success', {
            timeOut: 0,
            extendedTimeOut: 0,
            tapToDismiss: true,
            closeButton: true,
        });
    }
}
