import { Component, ElementRef, HostListener, Input, ViewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatIcon } from '@angular/material/icon';

@Component({
    selector: 'tinn-multi-select-dropdown',
    imports: [ReactiveFormsModule, MatIcon],
    templateUrl: './multi-select-dropdown.component.html',
})
export class MultiSelectDropdownComponent {
    @ViewChild('dropdownContainer')
    private dropdownContainer!: ElementRef;

    @Input({ required: true })
    public control!: FormControl<string[] | null>;
    @Input({ required: true })
    public options: string[] = [];
    @Input()
    public placeholder: string = 'Select options';

    public isDropdownOpen = false;

    public toggleDropdown(): void {
        this.isDropdownOpen = !this.isDropdownOpen;
    }

    public isOptionSelected(option: string): boolean {
        const currentValues = this.control.value || [];
        return currentValues.includes(option);
    }

    public onOptionToggle(option: string): void {
        const currentValues = this.control.value || [];
        if (this.isOptionSelected(option)) {
            this.control.setValue(currentValues.filter((v: string) => v !== option));
        } else {
            this.control.setValue([...currentValues, option]);
        }
        this.control.markAsTouched();
    }

    public removeOption(option: string): void {
        const currentValues = this.control.value || [];
        this.control.setValue(currentValues.filter((v: string) => v !== option));
        this.control.markAsTouched();
    }

    public getSelectedOptions(): string[] {
        return this.control.value || [];
    }

    public getSelectedCount(): number {
        return this.getSelectedOptions().length;
    }

    @HostListener('document:click', ['$event'])
    public onDocumentClick(event: MouseEvent): void {
        if (!this.dropdownContainer) {
            return;
        }

        const clickedInside = this.dropdownContainer.nativeElement.contains(event.target);
        if (!clickedInside && this.isDropdownOpen) {
            this.isDropdownOpen = false;
        }
    }
}
