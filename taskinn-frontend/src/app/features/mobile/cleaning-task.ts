import { RoomStatus } from '../room/room';

export interface CleaningTaskDto {
    id: number;
    scheduledStart: string;
    scheduledEnd: string;
    actualStart: string | null;
    actualEnd: string | null;
    taskType: TaskType;
    status: TaskStatus;
    notes: string | null;
    createdAt: string; // ISO timestamp
    updatedAt: string; // ISO timestamp
    employeeId: number | null;
    employeeName: string | null;
    roomId: number;
    roomNumber: string;
    roomStatus?: RoomStatus;
}

export interface StartCleaningRequestDto {
    roomId: number;
}

export interface FinishCleaningRequestDto {
    roomId: number;
    notes?: string;
}

export interface SkipCleaningRequestDto {
    roomId: number;
    reason?: string;
}

export enum TaskStatus {
    PENDING = 'PENDING',
    IN_PROGRESS = 'IN_PROGRESS',
    COMPLETED = 'COMPLETED',
    BLOCKED = 'BLOCKED',
}

export enum TaskType {
    CHECKOUT_CLEAN = 'CHECKOUT_CLEAN',
    STAYOVER_CLEAN = 'STAYOVER_CLEAN',
    DEEP_CLEAN = 'DEEP_CLEAN',
}
