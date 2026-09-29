import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIcon } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';

export interface PageChangeEvent {
    pageIndex: number;
    pageSize: number;
    previousPageIndex?: number;
}

@Component({
    selector: 'tinn-paginator',
    imports: [CommonModule, MatIcon, FormsModule],
    templateUrl: './paginator.component.html',
})
export class PaginatorComponent {
    public length = input.required<number>();
    public pageSize = input.required<number>();
    public pageIndex = input.required<number>();
    public pageSizeOptions = input<number[]>([5, 10, 25, 50]);
    public showFirstLastButtons = input<boolean>(true);

    public readonly pageChange = output<PageChangeEvent>();

    public get totalPages(): number {
        return Math.ceil(this.length() / this.pageSize());
    }

    public get startIndex(): number {
        return this.pageIndex() * this.pageSize() + 1;
    }

    public get endIndex(): number {
        const end = (this.pageIndex() + 1) * this.pageSize();
        return Math.min(end, this.length());
    }

    public get isFirstPage(): boolean {
        return this.pageIndex() === 0;
    }

    public get isLastPage(): boolean {
        return this.pageIndex() >= this.totalPages - 1;
    }

    public onPageSizeChange(newSize: string): void {
        const size = parseInt(newSize, 10);
        const previousPageIndex = this.pageIndex();

        const newPageIndex = Math.floor((this.pageIndex() * this.pageSize()) / size);

        this.pageChange.emit({
            pageIndex: newPageIndex,
            pageSize: size,
            previousPageIndex,
        });
    }

    public goToFirstPage(): void {
        if (!this.isFirstPage) {
            this.changePage(0);
        }
    }

    public goToPreviousPage(): void {
        if (!this.isFirstPage) {
            this.changePage(this.pageIndex() - 1);
        }
    }

    public goToNextPage(): void {
        if (!this.isLastPage) {
            this.changePage(this.pageIndex() + 1);
        }
    }

    public goToLastPage(): void {
        if (!this.isLastPage) {
            this.changePage(this.totalPages - 1);
        }
    }

    private changePage(newPageIndex: number): void {
        this.pageChange.emit({
            pageIndex: newPageIndex,
            pageSize: this.pageSize(),
            previousPageIndex: this.pageIndex(),
        });
    }
}
