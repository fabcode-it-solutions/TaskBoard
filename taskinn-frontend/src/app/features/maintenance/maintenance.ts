export enum TicketStatus {
    OPEN = 'OPEN',
    IN_PROGRESS = 'IN_PROGRESS',
    RESOLVED = 'RESOLVED',
    CLOSED = 'CLOSED',
}

export enum Priority {
    LOW = 'LOW',
    MEDIUM = 'MEDIUM',
    HIGH = 'HIGH',
    URGENT = 'URGENT',
}

export interface MaintenanceTicketDto {
    id: number;
    title: string;
    description: string;
    priority: Priority;
    status: TicketStatus;
    roomId: number;
    roomNumber: string;
    reporterId: number;
    reporterName: string;
}

export interface MaintenanceTicketCreationUpdateDto {
    id: number;
    title: string;
    description: string;
    priority: Priority;
    status: TicketStatus;
    roomId: number;
}
