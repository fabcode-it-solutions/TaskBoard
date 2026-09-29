import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { EmployeeService } from '../employee.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { MatIcon } from '@angular/material/icon';
import { PageChangeEvent, PaginatorComponent } from '../../../shared/components/paginator/paginator.component';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'tinn-staff-list',
    imports: [CommonModule, PageHeaderComponent, MatIcon, FormsModule, PaginatorComponent],
    templateUrl: './staff-list.component.html',
})
export class StaffListComponent implements OnInit {
    private readonly employeeService: EmployeeService = inject(EmployeeService);
    private readonly router = inject(Router);
    private readonly toastr = inject(ToastrService);

    public readonly loading = this.employeeService.loading;
    public readonly error = this.employeeService.error;
    public readonly staffMembers = this.employeeService.employeeList;

    public searchTerm = signal('');
    public statusFilter = signal<'active' | 'inactive' | 'all'>('active');
    public pageSize = signal(6);
    public pageIndex = signal(0);
    public pageSizeOptions = [6, 12, 24, 48];

    public filteredStaffMembers = computed(() => {
        const search = this.searchTerm().toLowerCase().trim();
        const status = this.statusFilter();
        const members = this.staffMembers();

        if (!members) {
            return [];
        }

        let filtered = members;

        if (status === 'active') {
            filtered = filtered.filter((staff) => staff.active);
        } else if (status === 'inactive') {
            filtered = filtered.filter((staff) => !staff.active);
        }

        if (search) {
            filtered = filtered.filter((staff) => {
                const fullName = `${staff.firstName} ${staff.lastName}`.toLowerCase();
                return fullName.includes(search);
            });
        }

        return filtered;
    });

    public paginatedStaffMembers = computed(() => {
        const filtered = this.filteredStaffMembers();
        const startIndex = this.pageIndex() * this.pageSize();
        const endIndex = startIndex + this.pageSize();
        return filtered.slice(startIndex, endIndex);
    });

    async ngOnInit(): Promise<void> {
        await this.employeeService.getAllStaff();
    }

    public onEdit(id: string): void {
        this.router.navigate(['/employee/edit', id]);
    }

    public async onToggleActive(id: string, active: boolean): Promise<void> {
        const employee = this.staffMembers()?.find((e) => e.id === id);
        if (!employee) {
            this.toastr.error('Employee not found', 'Error');
            return;
        }

        const newStatus = !active;
        const employeeData = {
            ...employee,
            active: newStatus,
        };

        await this.employeeService.updateEmployee(id, employeeData);

        if (!this.error()) {
            const message = newStatus ? 'Staff member activated successfully' : 'Staff member deactivated successfully';
            this.toastr.success(message, 'Success');
            await this.employeeService.getAllStaff();
        }

        if (this.error()) {
            const errorMessage = this.error()?.message || 'An error occurred';
            const action = newStatus ? 'activate' : 'deactivate';
            this.toastr.error(errorMessage, `Failed to ${action} staff member`);
        }
    }

    public onSearchChange(value: string): void {
        this.searchTerm.set(value);
        this.pageIndex.set(0);
    }

    public clearSearch(): void {
        this.searchTerm.set('');
        this.pageIndex.set(0);
    }

    public onPageChange(event: PageChangeEvent): void {
        this.pageIndex.set(event.pageIndex);
        this.pageSize.set(event.pageSize);
    }

    public onStatusFilterChange(status: 'active' | 'inactive' | 'all'): void {
        this.statusFilter.set(status);
        this.pageIndex.set(0);
    }
}
