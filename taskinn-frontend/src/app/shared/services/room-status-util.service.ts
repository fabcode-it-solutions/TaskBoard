import { Injectable } from '@angular/core';
import { RoomStatus } from '../../features/room/room';
import { getRoomStatusColor, getRoomStatusStrokeColor, getRoomStatusBgClass } from '../util/status-colors';

/**
 * Service for room status utilities and color calculations.
 * Provides simplified color schemes for mobile map view and status checking.
 */
@Injectable({
    providedIn: 'root',
})
export class RoomStatusUtilService {
    /**
     * Get simplified room color for mobile map view.
     * Groups statuses into actionable categories: needs cleaning (amber), being cleaned (violet), out of order (red), or neutral (gray).
     * @param status Room status
     * @returns Hex color string
     */
    public getSimplifiedRoomColor(status: RoomStatus): string {
        switch (status) {
            case RoomStatus.AVAILABLE:
                return '#10b981'; // Green - available
            case RoomStatus.OCCUPIED:
                return '#3b82f6'; // Blue - occupied
            case RoomStatus.DIRTY:
                return '#f59e0b'; // Amber/Orange - needs cleaning
            case RoomStatus.CLEANING:
                return '#8b5cf6'; // Violet - being cleaned
            case RoomStatus.OUT_OF_ORDER:
            case RoomStatus.PERMANENTLY_INACTIVE:
                return '#ef4444'; // Red - out of order
            default:
                return '#d1d5db'; // Neutral gray fallback
        }
    }

    /**
     * Get simplified room stroke color for mobile map view borders.
     * @param status Room status
     * @returns Hex color string
     */
    public getSimplifiedRoomStrokeColor(status: RoomStatus): string {
        switch (status) {
            case RoomStatus.AVAILABLE:
                return '#059669'; // Darker green
            case RoomStatus.OCCUPIED:
                return '#2563eb'; // Darker blue
            case RoomStatus.DIRTY:
                return '#d97706'; // Darker amber
            case RoomStatus.CLEANING:
                return '#7c3aed'; // Darker violet
            case RoomStatus.OUT_OF_ORDER:
            case RoomStatus.PERMANENTLY_INACTIVE:
                return '#dc2626'; // Darker red
            default:
                return '#9ca3af'; // Neutral gray stroke fallback
        }
    }

    /**
     * Get full room fill color (delegates to utility function).
     * @param status Room status
     * @returns Hex color string
     */
    public getRoomColor(status: RoomStatus): string {
        return getRoomStatusColor(status);
    }

    /**
     * Get full room stroke color (delegates to utility function).
     * @param status Room status
     * @returns Hex color string
     */
    public getRoomStrokeColor(status: RoomStatus): string {
        return getRoomStatusStrokeColor(status);
    }

    /**
     * Get Tailwind background class for room status badge.
     * @param status Room status
     * @returns Tailwind CSS class string
     */
    public getRoomBgClass(status: RoomStatus): string {
        return getRoomStatusBgClass(status);
    }

    /**
     * Check if room is occupied.
     * @param status Room status
     * @returns true if room is occupied
     */
    public isRoomOccupied(status: RoomStatus): boolean {
        return status === RoomStatus.OCCUPIED;
    }

    /**
     * Check if room is available.
     * @param status Room status
     * @returns true if room is available
     */
    public isRoomAvailable(status: RoomStatus): boolean {
        return status === RoomStatus.AVAILABLE;
    }

    /**
     * Check if room is dirty and needs cleaning.
     * @param status Room status
     * @returns true if room is dirty
     */
    public isRoomDirty(status: RoomStatus): boolean {
        return status === RoomStatus.DIRTY;
    }

    /**
     * Check if room is currently being cleaned.
     * @param status Room status
     * @returns true if room is being cleaned
     */
    public isRoomCleaning(status: RoomStatus): boolean {
        return status === RoomStatus.CLEANING;
    }

    /**
     * Check if room requires cleaning action (dirty or currently being cleaned).
     * @param status Room status
     * @returns true if room requires cleaning attention
     */
    public requiresCleaningAction(status: RoomStatus): boolean {
        return this.isRoomDirty(status) || this.isRoomCleaning(status);
    }

    /**
     * Check if room is out of service.
     * @param status Room status
     * @returns true if room is out of order or permanently inactive
     */
    public isRoomOutOfService(status: RoomStatus): boolean {
        return status === RoomStatus.OUT_OF_ORDER || status === RoomStatus.PERMANENTLY_INACTIVE;
    }

    /**
     * Get human-readable status label from enum.
     * @param status Room status enum value
     * @returns Formatted status label
     */
    public getStatusLabel(status: RoomStatus): string {
        return status.replace(/_/g, ' ');
    }

    /**
     * Check if room status represents an actionable state for cleaning staff.
     * Actionable states: DIRTY (needs cleaning), CLEANING (in progress), OUT_OF_ORDER (needs attention).
     * @param status Room status
     * @returns true if status requires action
     */
    public isActionableStatus(status: RoomStatus): boolean {
        return status === RoomStatus.DIRTY || status === RoomStatus.CLEANING || status === RoomStatus.OUT_OF_ORDER;
    }
}
