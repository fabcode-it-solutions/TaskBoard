/**
 * Represents the light, summary information for a Room.
 * Used for list views where the full details are not required.
 */
export interface LightRoomDto {
    id: number;
    roomNumber: string;
    floor: number;
    active: boolean;
    status: RoomStatus; // e.g., 'AVAILABLE', 'OCCUPIED', etc.
    roomType: RoomTypeInfo;
    // Effective values (override if set, otherwise inherited from RoomType)
    effectiveCapacity: number;
    effectiveBasePrice: number;
    effectiveSingleBed?: number;
    effectiveDoubleBed?: number;
}

/**
 * A nested interface containing summary information about the RoomType.
 */
export interface RoomTypeInfo {
    id: number;
    name: string;
    capacity: number;
    basePrice?: number;
    singleBed?: number;
    doubleBed?: number;
}

export interface Point {
    x: number;
    y: number;
}

export interface RoomDto {
    id: number;
    number: string;
    floor: number;
    status: RoomStatus;
    active: boolean;
    svgPoints: Point[];
    roomTypeId: number | null;
    // Override fields - null/undefined means inherit from RoomType
    capacityOverride?: number | null;
    basePriceOverride?: number | null;
    singleBedOverride?: number | null;
    doubleBedOverride?: number | null;
}

export interface RoomRequestDto {
    id?: number; // Optional, only for updates
    roomNumber: string;
    floor: number;
    status: RoomStatus;
    active: boolean;
    svgPoints: Point[];
    roomTypeId: number;
    // Override fields - null/undefined means inherit from RoomType
    capacityOverride?: number | null;
    basePriceOverride?: number | null;
    singleBedOverride?: number | null;
    doubleBedOverride?: number | null;
}

export interface LiteRoomType {
    id: number;
    name: string;
}

export interface RoomTypeDto {
    id: number;
    name: string;
    description?: string;
    capacity: number;
    basePrice: number;
    singleBed?: number;
    doubleBed?: number;
}

export interface RoomTypeRequestDto {
    name: string;
    description?: string;
    capacity: number;
    basePrice: number;
    singleBed?: number;
    doubleBed?: number;
}

/**
 * Represents the occupancy distribution for a specific floor.
 */
export interface FloorOccupancyDto {
    floor: number;
    totalRooms: number;
    occupiedRooms: number;
    occupancyRate: number;
}

/**
 * An enum representing the possible operational statuses of a Room.
 * This should be kept in sync with the backend enum.
 */
export enum RoomStatus {
    AVAILABLE = 'AVAILABLE',
    OCCUPIED = 'OCCUPIED',
    DIRTY = 'DIRTY',
    CLEANING = 'CLEANING',
    OUT_OF_ORDER = 'OUT_OF_ORDER',
    PERMANENTLY_INACTIVE = 'PERMANENTLY_INACTIVE',
}
