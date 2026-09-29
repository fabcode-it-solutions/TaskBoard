import { TaskStatus } from '../../features/mobile/cleaning-task';
import { RoomStatus } from '../../features/room/room';

/**
 * Centralized color definitions for task and room statuses.
 * This file serves as the single source of truth for all status colors
 * across the application, ensuring consistency between timeline and map views.
 */

// Task status colors (hex values)
export const TASK_STATUS_COLORS: Record<TaskStatus, string> = {
    [TaskStatus.PENDING]: '#3b82f6', // Blue
    [TaskStatus.IN_PROGRESS]: '#7c3aed', // Violet
    [TaskStatus.COMPLETED]: '#10b981', // Green
    [TaskStatus.BLOCKED]: '#ef4444', // Red
};

// Room status colors (hex values)
export const ROOM_STATUS_COLORS: Record<RoomStatus, string> = {
    [RoomStatus.AVAILABLE]: '#10b981', // Green
    [RoomStatus.OCCUPIED]: '#3b82f6', // Blue
    [RoomStatus.DIRTY]: '#f59e0b', // Amber/Orange
    [RoomStatus.CLEANING]: '#7c3aed', // Violet
    [RoomStatus.OUT_OF_ORDER]: '#ef4444', // Red
    [RoomStatus.PERMANENTLY_INACTIVE]: '#6b7280', // Gray
};

// Room status stroke colors (darker variants for map borders)
export const ROOM_STATUS_STROKE_COLORS: Record<RoomStatus, string> = {
    [RoomStatus.AVAILABLE]: '#059669', // Darker green
    [RoomStatus.OCCUPIED]: '#2563eb', // Darker blue
    [RoomStatus.DIRTY]: '#d97706', // Darker amber
    [RoomStatus.CLEANING]: '#7c3aed', // Same violet
    [RoomStatus.OUT_OF_ORDER]: '#dc2626', // Darker red
    [RoomStatus.PERMANENTLY_INACTIVE]: '#6b7280', // Same gray
};

// Tailwind CSS classes for room status badges
export const ROOM_STATUS_BG_CLASSES: Record<RoomStatus, string> = {
    [RoomStatus.AVAILABLE]: 'bg-green-500',
    [RoomStatus.OCCUPIED]: 'bg-blue-500',
    [RoomStatus.DIRTY]: 'bg-amber-500',
    [RoomStatus.CLEANING]: 'bg-violet-500',
    [RoomStatus.OUT_OF_ORDER]: 'bg-red-500',
    [RoomStatus.PERMANENTLY_INACTIVE]: 'bg-gray-400',
};

/**
 * Get the color for a given task status
 * @param status The task status
 * @returns Hex color string, defaults to gray if status not found
 */
export function getTaskStatusColor(status: TaskStatus): string {
    return TASK_STATUS_COLORS[status] ?? '#6b7280';
}

/**
 * Get the fill color for a given room status
 * @param status The room status
 * @returns Hex color string, defaults to gray if status not found
 */
export function getRoomStatusColor(status: RoomStatus): string {
    return ROOM_STATUS_COLORS[status] ?? '#9ca3af';
}

/**
 * Get the stroke color for a given room status (used for map borders)
 * @param status The room status
 * @returns Hex color string, defaults to gray if status not found
 */
export function getRoomStatusStrokeColor(status: RoomStatus): string {
    return ROOM_STATUS_STROKE_COLORS[status] ?? '#6b7280';
}

/**
 * Get the Tailwind background class for a given room status
 * @param status The room status
 * @returns Tailwind CSS class string, defaults to gray if status not found
 */
export function getRoomStatusBgClass(status: RoomStatus): string {
    return ROOM_STATUS_BG_CLASSES[status] ?? 'bg-gray-400';
}
