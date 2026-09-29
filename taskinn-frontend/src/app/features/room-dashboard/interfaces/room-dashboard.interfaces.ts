import { Point, RoomStatus } from '../../room/room';

export interface DashboardRoomDto {
    roomId: number;
    roomNumber: string;
    status: RoomStatus;
    guestName: string | null;
    hasOpenMaintenanceTicket: boolean;
    floorNumber: number;
    svgPoints: Point[];
}
