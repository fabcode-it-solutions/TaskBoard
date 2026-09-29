import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIcon } from '@angular/material/icon';

@Component({
    selector: 'tinn-optimization-parameters',
    templateUrl: './optimization-parameters.html',
    imports: [FormsModule, MatIcon],
})
export class OptimizationParametersComponent {
    @Input() public optimizationStartTime: string = '08:00';
    @Input() public loading: boolean = false;
    @Input() public canGenerate: boolean = false;

    @Output() private readonly optimizationStartTimeChange = new EventEmitter<string>();
    @Output() private readonly generateSchedule = new EventEmitter<void>();

    public onOptimizationStartTimeChange(value: string): void {
        this.optimizationStartTimeChange.emit(value);
    }

    public onGenerateSchedule(): void {
        this.generateSchedule.emit();
    }
}
