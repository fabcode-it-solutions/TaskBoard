import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIcon } from '@angular/material/icon';

@Component({
    selector: 'tinn-stats-card',
    imports: [CommonModule, MatIcon],
    templateUrl: './stats-card.component.html',
})
export class StatsCardComponent {
    @Input({ required: true })
    public title!: string;

    @Input({ required: true })
    public value!: number | null;

    @Input({ required: true })
    public icon!: string;

    @Input()
    public suffix?: string = '';

    @Input()
    public color: 'primary' | 'success' | 'warning' | 'error' = 'primary';

    @Input()
    public isLoading: boolean = false;

    public get iconColorClass(): string {
        switch (this.color) {
            case 'success':
                return 'bg-success-100 text-success-600';
            case 'warning':
                return 'bg-warning-100 text-warning-600';
            case 'error':
                return 'bg-error-100 text-error-600';
            default:
                return 'bg-brand-100 text-brand-600';
        }
    }

    public get valueColorClass(): string {
        switch (this.color) {
            case 'success':
                return 'text-success-600';
            case 'warning':
                return 'text-warning-600';
            case 'error':
                return 'text-error-600';
            default:
                return 'text-brand-600';
        }
    }
}
