export enum TaskType {
    CHECKOUT_CLEAN = 'CHECKOUT_CLEAN',
    STAYOVER_CLEAN = 'STAYOVER_CLEAN',
    DEEP_CLEAN = 'DEEP_CLEAN',
}

export enum TaskStatus {
    PENDING = 'PENDING',
    IN_PROGRESS = 'IN_PROGRESS',
    COMPLETED = 'COMPLETED',
    BLOCKED = 'BLOCKED',
}

export interface CleaningTaskDto {
    id?: number;
    scheduledStart: Date;
    scheduledEnd: Date;
    taskType: TaskType;
    status?: TaskStatus;
    notes?: string;
    employeeId: number;
    employeeName?: string;
    roomId: number;
    roomNumber?: string;
    constraintViolated?: boolean;
}

export interface CleaningTaskSaveDto {
    id?: number;
    scheduledStart: string;
    scheduledEnd: string;
    taskType: TaskType;
    status?: TaskStatus;
    notes?: string;
    employeeId: number;
    roomId: number;
}

export interface TaskScheduleSolveRequestDto {
    startTime: string;
    employeeIds: number[];
    roomIds: number[];
    constraints: TaskConstraintDto[];
}

export interface TaskConstraintDto {
    roomId: number;
    duration: number; // in minutes
    taskType?: TaskType;
    taskStatus?: TaskStatus;
    maxEndTime?: string;
    minStartTime?: string;
}

export interface CleaningTaskExtended extends CleaningTaskDto {
    duration?: number;
    isEditing?: boolean;
}

export interface RoomConstraintFormModel {
    roomId: number;
    roomNumber: string;
    duration: number;
    taskType?: TaskType;
    taskStatus?: TaskStatus;
    constraintType: 'maxEndTime' | 'minStartTime' | 'none';
    maxEndTime?: Date;
    minStartTime?: Date;
    isSelected: boolean;
}
