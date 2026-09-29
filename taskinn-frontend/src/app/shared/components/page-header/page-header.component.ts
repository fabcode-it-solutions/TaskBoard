import { Component, Input } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

@Component({
    selector: 'tinn-page-header',
    imports: [MatIcon],
    templateUrl: './page-header.component.html',
})
export class PageHeaderComponent {
    @Input({ required: true })
    public title!: string;

    @Input({ required: true })
    public icon!: string;

    @Input({ required: true })
    public subtitle!: string;
}
