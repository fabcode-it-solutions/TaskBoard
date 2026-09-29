import { Component, ElementRef, HostListener, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIcon } from '@angular/material/icon';

export interface DropdownMenuItem {
    label: string;
    icon: string;
    action: string;
    danger?: boolean;
}

@Component({
    selector: 'tinn-dropdown-menu',
    imports: [CommonModule, MatIcon],
    templateUrl: './dropdown-menu.component.html',
})
export class DropdownMenuComponent {
    public readonly items = input.required<DropdownMenuItem[]>();
    public readonly itemClicked = output<string>();

    public isOpen = false;

    constructor(private elementRef: ElementRef) {}

    public toggleMenu(event: Event): void {
        event.stopPropagation();
        this.isOpen = !this.isOpen;
    }

    public onItemClick(action: string, event: Event): void {
        event.stopPropagation();
        this.itemClicked.emit(action);
        this.isOpen = false;
    }

    @HostListener('document:click', ['$event'])
    public onPageClick(event: Event): void {
        if (!this.elementRef.nativeElement.contains(event.target)) {
            this.isOpen = false;
        }
    }
}
