import { Component, effect, inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { MatIcon } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { MaintenanceService } from '../maintenance.service';
import { MaintenanceTicketDto, TicketStatus } from '../maintenance';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { TicketDialogData, TicketFormDialogComponent } from './ticket-form-dialog/ticket-form-dialog.component';

type DialogMode = 'create' | 'view' | 'edit';

@Component({
    selector: 'tinn-maintenance-board',
    imports: [CommonModule, DragDropModule, MatIcon, PageHeaderComponent],
    templateUrl: './maintenance-board.component.html',
})
export class MaintenanceBoardComponent implements OnInit, OnDestroy {
    private readonly maintenanceService = inject(MaintenanceService);
    private readonly dialog = inject(MatDialog);

    public readonly loading = this.maintenanceService.loading;
    public readonly ticketList = this.maintenanceService.maintenanceTicketList;

    private readonly latestTicketUpdate = toSignal(this.maintenanceService.ticketUpdates$);

    public statusColumns = [
        { status: TicketStatus.OPEN, title: 'Open', tickets: [] as MaintenanceTicketDto[], canAdd: true },
        {
            status: TicketStatus.IN_PROGRESS,
            title: 'In Progress',
            tickets: [] as MaintenanceTicketDto[],
            canAdd: false,
        },
        { status: TicketStatus.RESOLVED, title: 'Resolved', tickets: [] as MaintenanceTicketDto[], canAdd: false },
        { status: TicketStatus.CLOSED, title: 'Closed', tickets: [] as MaintenanceTicketDto[], canAdd: false },
    ];

    constructor() {
        effect(() => {
            const updatedTickets = this.latestTicketUpdate();
            if (updatedTickets) {
                this.maintenanceService.maintenanceTicketList.set(updatedTickets);
                this.organizeTicketsByStatus();
            }
        });
    }

    public async ngOnInit(): Promise<void> {
        await this.loadTickets();
        this.maintenanceService.connect();
    }

    public ngOnDestroy(): void {
        this.maintenanceService.disconnect();
    }

    public async onDrop(event: CdkDragDrop<MaintenanceTicketDto[]>, newStatus: TicketStatus): Promise<void> {
        if (event.previousContainer === event.container) {
            moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
        } else {
            const ticket = event.previousContainer.data[event.previousIndex];

            transferArrayItem(
                event.previousContainer.data,
                event.container.data,
                event.previousIndex,
                event.currentIndex,
            );

            await this.updateTicketStatus(ticket, newStatus);
        }
    }

    public getConnectedLists(): string[] {
        return this.statusColumns.map((col) => col.status);
    }

    public openDialog(mode: DialogMode, ticket?: MaintenanceTicketDto): void {
        const dialogData: TicketDialogData = {
            mode: mode,
            ticket: ticket,
        };

        const dialogRef = this.dialog.open(TicketFormDialogComponent, {
            width: '600px',
            data: dialogData,
            backdropClass: 'dialog-backdrop-blur',
        });

        dialogRef.afterClosed().subscribe(async (result) => {
            if (result) {
                await this.loadTickets();
            }
        });
    }

    public getPriorityColor(priority: string): string {
        switch (priority) {
            case 'URGENT':
                return 'bg-error-600 text-white';
            case 'HIGH':
                return 'bg-orange-500 text-white';
            case 'MEDIUM':
                return 'bg-yellow-400 text-neutral-800';
            case 'LOW':
                return 'bg-green-500 text-white';
            default:
                return 'bg-neutral-300 text-neutral-800';
        }
    }

    private async loadTickets(): Promise<void> {
        await this.maintenanceService.getAllMaintenanceTickets();
        this.organizeTicketsByStatus();
    }

    private organizeTicketsByStatus(): void {
        const tickets = this.ticketList() || [];

        this.statusColumns[0].tickets = tickets.filter((t) => t.status === TicketStatus.OPEN);
        this.statusColumns[1].tickets = tickets.filter((t) => t.status === TicketStatus.IN_PROGRESS);
        this.statusColumns[2].tickets = tickets.filter((t) => t.status === TicketStatus.RESOLVED);
        this.statusColumns[3].tickets = tickets.filter((t) => t.status === TicketStatus.CLOSED);
    }

    private async updateTicketStatus(ticket: MaintenanceTicketDto, newStatus: TicketStatus): Promise<void> {
        const updateData = {
            id: ticket.id,
            title: ticket.title,
            description: ticket.description,
            priority: ticket.priority,
            status: newStatus,
            roomId: ticket.roomId,
        };

        await this.maintenanceService.updateMaintenanceTicket(ticket.id, updateData);
        if (this.maintenanceService.error()) {
            await this.loadTickets();
        }
    }
}
