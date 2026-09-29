import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CheckInService } from '../check-in.service';
import { CheckInStore } from '../check-in.store';
import { MatIcon } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MandatoryMarkComponent } from '../../../shared/components/mandatory-mark/mandatory-mark.component';

@Component({
    selector: 'tinn-check-in-initial-page',
    imports: [MatIcon, PageHeaderComponent, ReactiveFormsModule, MandatoryMarkComponent],
    templateUrl: './check-in-initial-page.component.html',
})
export class CheckInInitialPageComponent {
    private readonly fb = inject(FormBuilder);
    private readonly service = inject(CheckInService);
    private readonly store = inject(CheckInStore);
    private readonly router = inject(Router);

    public readonly loading = this.store.loading;

    public bookingForm: FormGroup;
    public errorMsg: string | null = null;

    constructor() {
        this.bookingForm = this.fb.group({
            bookingId: ['', Validators.required],
        });
    }

    public async onSubmit(): Promise<void> {
        if (this.bookingForm.invalid) {
            this.bookingForm.markAllAsTouched();
            return;
        }

        this.errorMsg = null;
        const bookingId = Number(this.bookingForm.value.bookingId);

        await this.service.loadCheckInData(bookingId);

        const error = this.store.error();
        if (error?.validationErrors?.[0]) {
            this.errorMsg = error.validationErrors[0].message;
            return;
        }
        if (error?.message) {
            this.errorMsg = error.message;
            return;
        }

        this.router.navigate(['/checkin/form', bookingId]);
    }

    public onClickBack(): void {
        this.router.navigate(['/staff/dashboard']);
    }
}
