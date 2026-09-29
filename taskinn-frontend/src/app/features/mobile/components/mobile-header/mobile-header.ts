import { Component, input, output } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

@Component({
    selector: 'tinn-mobile-header',
    imports: [MatIcon],
    templateUrl: './mobile-header.html',
    styleUrl: './mobile-header.scss',
})
export class MobileHeaderComponent {
    public title = input.required<string>();
    public showBackButton = input<boolean>(false);
    public readonly backClick = output<void>();

    public onBackClick(): void {
        this.backClick.emit();
    }
}
