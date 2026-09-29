import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { firstValueFrom } from 'rxjs';
import { RoomPropertiesPanelComponent } from '../room-properties-panel/room-properties-panel.component';
import { RegisteredRoomsComponent } from '../registered-rooms/registered-rooms.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import {
    ConfirmationDialogComponent,
    ConfirmationDialogData,
} from '../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { ToastrService } from 'ngx-toastr';
import { Point, RoomDto, RoomRequestDto, RoomStatus } from '../../room/room';
import { RoomService } from '../../room/room.service';
import { ErrorResponse } from '../../../shared/interfaces/error';
import { SvgUtilityService } from '../../../shared/services/svg-utility.service';
import { StaircaseService } from '../../staircase/staircase.service';
import { StaircaseDto } from '../../staircase/staircase';
import { environment } from '../../../../environments/environment';

@Component({
    selector: 'tinn-floor-plan-editor',
    imports: [CommonModule, MatIcon, RoomPropertiesPanelComponent, RegisteredRoomsComponent, PageHeaderComponent],
    templateUrl: './floor-plan-editor.component.html',
})
export class FloorPlanEditorComponent implements OnInit {
    private readonly roomService = inject(RoomService);
    private readonly staircaseService = inject(StaircaseService);
    private readonly toastr = inject(ToastrService);
    private readonly svgUtil = inject(SvgUtilityService);
    private readonly dialog = inject(MatDialog);
    private readonly http = inject(HttpClient);

    public readonly loading = this.roomService.loading;
    public readonly error = this.roomService.error;
    public blueprintImage = 'hotel-floor-blueprint/Hotel_Blueprint.jpg';
    public selectedFloor = signal<number | null>(1);
    public isDrawing = signal<boolean>(false);
    public isPlacingStaircase = signal<boolean>(false);
    public currentPoints = signal<Point[]>([]);
    public selectedRoom = signal<RoomDto | null>(null);
    public pendingFloors = signal<Set<number>>(new Set());
    public floorsWithRooms = signal<Set<number>>(new Set());
    public showCopyFloorDropdown = signal<boolean>(false);
    public clickCount = 0;
    public showButton = false;

    public currentFloorStaircases = computed<StaircaseDto[]>(() => {
        return this.staircaseService.staircasesByFloor() ?? [];
    });

    public maxFloor = computed<number>(() => this.roomService.maxFloor() ?? 0);

    public floors = computed<number[]>(() => {
        const pending = Array.from(this.pendingFloors());
        const withRooms = Array.from(this.floorsWithRooms());
        const allFloors = new Set([...withRooms, ...pending]);

        if (allFloors.size === 0) {
            allFloors.add(1);
        }

        return Array.from(allFloors).sort((a, b) => a - b);
    });

    public currentFloorRooms = computed<RoomDto[]>(() => {
        return this.roomService.roomsByFloor() ?? [];
    });

    public async ngOnInit(): Promise<void> {
        await this.roomService.getMaxFloor();
        await this.updateFloorsWithRooms();
        const floor = this.selectedFloor();
        if (floor !== null) {
            await this.roomService.getRoomsByFloor(floor);
            await this.staircaseService.getStaircasesByFloor(floor);
        }
    }

    public async addFloor(): Promise<void> {
        const floors = this.floors();
        const newFloorNumber = floors.length > 0 ? Math.max(...floors) + 1 : 1;
        this.pendingFloors.update((pending) => new Set([...pending, newFloorNumber]));

        this.selectedFloor.set(newFloorNumber);
        this.isDrawing.set(false);
        this.currentPoints.set([]);
        this.selectedRoom.set(null);
        await this.roomService.getRoomsByFloor(newFloorNumber);
    }

    public async removeFloor(): Promise<void> {
        const currentFloor = this.selectedFloor();
        if (currentFloor === null) {
            return;
        }

        const floors = this.floors();
        if (floors.length <= 1) {
            return;
        }

        const roomsOnFloor = this.currentFloorRooms();
        if (roomsOnFloor.length > 0) {
            this.toastr.error(
                `Cannot delete floor ${currentFloor}. Please delete all ${roomsOnFloor.length} room(s) first.`,
                'Floor not empty',
            );
            return;
        }

        this.pendingFloors.update((pending) => {
            const newPending = new Set(pending);
            newPending.delete(currentFloor);
            return newPending;
        });

        this.floorsWithRooms.update((currentFloors) => {
            const newFloors = new Set(currentFloors);
            newFloors.delete(currentFloor);
            return newFloors;
        });

        const remainingFloors = floors.filter((f) => f !== currentFloor);
        this.selectedFloor.set(remainingFloors[0] || 1);
    }

    public async copyFloor(fromFloor: number): Promise<void> {
        const currentFloor = this.selectedFloor();
        if (currentFloor === null) {
            return;
        }

        try {
            const copiedRooms = await this.roomService.copyFloor(fromFloor, currentFloor);

            await this.roomService.getMaxFloor();
            await this.roomService.getRoomsByFloor(currentFloor);

            this.toastr.success(
                `Successfully copied ${copiedRooms.length} room(s) from floor ${fromFloor} to floor ${currentFloor}`,
                'Floor copied',
            );
        } catch (error: unknown) {
            const errorResponse = error as ErrorResponse;
            const errorMessage = errorResponse?.message || 'An error occurred while copying the floor';
            this.toastr.error(errorMessage, 'Failed to copy floor');
        } finally {
            this.showCopyFloorDropdown.set(false);
        }
    }

    public toggleCopyFloorDropdown(): void {
        const roomsOnFloor = this.currentFloorRooms();
        if (roomsOnFloor.length > 0) {
            this.toastr.error(
                `Cannot copy to floor ${this.selectedFloor()}. Please delete all ${roomsOnFloor.length} room(s) first.`,
                'Floor not empty',
            );
            return;
        }

        this.showCopyFloorDropdown.update((show) => !show);
    }

    public getAvailableFloorsForCopy(): number[] {
        const currentFloor = this.selectedFloor();

        return this.floors().filter((f) => f !== currentFloor);
    }

    public async selectFloor(floorId: number): Promise<void> {
        this.selectedFloor.set(floorId);
        this.isDrawing.set(false);
        this.isPlacingStaircase.set(false);
        this.currentPoints.set([]);
        this.selectedRoom.set(null);
        await this.roomService.getRoomsByFloor(floorId);
        await this.staircaseService.getStaircasesByFloor(floorId);
    }

    public onBlueprintClick(event: MouseEvent): void {
        if (!this.selectedFloor()) {
            return;
        }

        const svg = event.currentTarget as SVGSVGElement;
        // Use identity transform since editor doesn't have zoom/pan
        const point = this.svgUtil.screenToSVGPoint(svg, event.clientX, event.clientY, { x: 0, y: 0, scale: 1 });

        if (this.isPlacingStaircase()) {
            this.placeStaircase(point);
            return;
        }

        if (!this.isDrawing()) {
            // Start drawing
            this.isDrawing.set(true);
            this.currentPoints.set([point]);
        } else {
            // Add point to polygon
            this.currentPoints.set([...this.currentPoints(), point]);
        }
    }

    public completeDrawing(): void {
        if (this.currentPoints().length < 3) {
            this.toastr.warning('A room must have at least 3 points', 'Invalid Polygon');
            return;
        }

        const existingRoom = this.selectedRoom();

        if (existingRoom && existingRoom.id > 0) {
            // Updating existing room's shape
            this.selectedRoom.set({
                ...existingRoom,
                svgPoints: this.currentPoints(),
            });
        } else {
            // Creating new room
            const tempRoom: RoomDto = {
                id: 0,
                floor: this.selectedFloor()!,
                number: '',
                status: RoomStatus.AVAILABLE,
                svgPoints: this.currentPoints(),
                active: true,
                roomTypeId: null,
            };
            this.selectedRoom.set(tempRoom);
        }

        this.isDrawing.set(false);
    }

    public cancelDrawing(): void {
        this.isDrawing.set(false);
        this.currentPoints.set([]);
    }

    public async onRoomSaved(roomRequest: RoomRequestDto): Promise<void> {
        let requestToSave = roomRequest;
        if (this.currentPoints().length >= 3) {
            requestToSave = {
                ...roomRequest,
                svgPoints: this.currentPoints(),
            };
            this.isDrawing.set(false);
        }

        if (requestToSave.id && requestToSave.id > 0) {
            await this.roomService.updateRoom(requestToSave.id, requestToSave);
        } else {
            const { id: _, ...createRequest } = requestToSave;
            await this.roomService.createRoom(createRequest as RoomRequestDto);
        }

        if (this.error()) {
            const error = this.error();
            let errorMessage = error?.message || 'An error occurred';

            if (error?.validationErrors && error.validationErrors.length > 0) {
                errorMessage = error.validationErrors.map((err) => err.message).join('\n');
            }

            this.toastr.error(errorMessage, 'Failed to save room');
            return;
        }

        this.pendingFloors.update((pending) => {
            const newPending = new Set(pending);
            newPending.delete(roomRequest.floor);
            return newPending;
        });

        this.floorsWithRooms.update((floors) => {
            const newFloors = new Set(floors);
            newFloors.add(roomRequest.floor);
            return newFloors;
        });

        this.selectedRoom.set(null);
        this.currentPoints.set([]);

        const currentFloor = this.selectedFloor();
        if (currentFloor !== null) {
            await this.roomService.getRoomsByFloor(currentFloor);
        }
        await this.roomService.getMaxFloor();

        const action = roomRequest.id && roomRequest.id > 0 ? 'updated' : 'created';
        this.toastr.success(`Room ${action} successfully`, 'Success');
    }

    public onRoomCancelled(): void {
        this.selectedRoom.set(null);
        this.currentPoints.set([]);
    }

    public editRoom(room: RoomDto): void {
        this.selectedRoom.set(room);
    }

    public async disableRoom(roomId: number): Promise<void> {
        try {
            await this.roomService.disableRoom(roomId);

            const currentFloor = this.selectedFloor();
            if (currentFloor !== null) {
                await this.roomService.getRoomsByFloor(currentFloor);
            }

            this.toastr.success('Room deleted successfully', 'Success');
        } catch (error: unknown) {
            const errorResponse = error as ErrorResponse;
            const errorMessage = errorResponse?.message || 'An error occurred';
            this.toastr.error(errorMessage, 'Failed to delete room');
        }
    }

    public pointsToPolygonString(points: Point[]): string {
        return this.svgUtil.pointsToPolygonString(points);
    }

    public getCenterX(points: Point[]): number {
        return this.svgUtil.getCenterX(points);
    }

    public getCenterY(points: Point[]): number {
        return this.svgUtil.getCenterY(points);
    }

    private async updateFloorsWithRooms(): Promise<void> {
        const maxFloor = this.maxFloor();
        const floorsWithRooms = new Set<number>();

        // Check each floor from 1 to maxFloor to see if it has rooms
        for (let floor = 1; floor <= maxFloor; floor++) {
            await this.roomService.getRoomsByFloor(floor);
            const rooms = this.roomService.roomsByFloor();
            if (rooms && rooms.length > 0) {
                floorsWithRooms.add(floor);
            }
        }

        this.floorsWithRooms.set(floorsWithRooms);
    }

    public toggleStaircaseMode(): void {
        if (this.isPlacingStaircase()) {
            this.isPlacingStaircase.set(false);
        } else {
            this.isDrawing.set(false);
            this.currentPoints.set([]);
            this.selectedRoom.set(null);
            this.isPlacingStaircase.set(true);
        }
    }

    public cancelStaircasePlacement(): void {
        this.isPlacingStaircase.set(false);
    }

    private async placeStaircase(point: Point): Promise<void> {
        const floor = this.selectedFloor();
        if (floor === null) {
            return;
        }

        const staircaseData = {
            floor: floor,
            x: point.x,
            y: point.y,
            name: null,
            active: true,
        };

        const created = await this.staircaseService.createStaircase(staircaseData);
        if (created) {
            this.toastr.success('Staircase added successfully', 'Success');
            await this.staircaseService.getStaircasesByFloor(floor);
        } else {
            this.toastr.error('Failed to add staircase', 'Error');
        }

        this.isPlacingStaircase.set(false);
    }

    public async deleteStaircase(staircaseId: number): Promise<void> {
        const confirmData: ConfirmationDialogData = {
            title: 'Delete Staircase',
            message: 'Are you sure you want to delete this staircase? This action cannot be undone.',
            confirmText: 'Delete',
            cancelText: 'Cancel',
            danger: true,
        };

        const confirmDialogRef = this.dialog.open(ConfirmationDialogComponent, {
            width: '500px',
            data: confirmData,
        });

        const confirmed = await firstValueFrom(confirmDialogRef.afterClosed());
        if (!confirmed) {
            return;
        }

        const success = await this.staircaseService.deleteStaircase(staircaseId);
        if (success) {
            this.toastr.success('Staircase deleted successfully', 'Success');
            const floor = this.selectedFloor();
            if (floor !== null) {
                await this.staircaseService.getStaircasesByFloor(floor);
            }
        } else {
            this.toastr.error('Failed to delete staircase', 'Error');
        }
    }

    public async exportRoomsCsv(): Promise<void> {
        try {
            const allRooms = await this.fetchAllRoomsForExport();

            if (allRooms.length === 0) {
                this.toastr.warning('No rooms found to export', 'Export CSV');
                return;
            }

            const csvContent = this.convertRoomsToCsv(allRooms);
            this.downloadCsv(csvContent, 'ROOM_SVG_POINTS.csv');
            this.toastr.success(`Exported ${allRooms.length} room(s) successfully`, 'Export CSV');
        } catch (error: unknown) {
            const errorResponse = error as ErrorResponse;
            const errorMessage = errorResponse?.message || 'An error occurred while exporting rooms';
            this.toastr.error(errorMessage, 'Export failed');
        }
    }

    public handleAreaClick(): void {
        if (this.showButton) {
            return;
        }

        this.clickCount++;

        if (this.clickCount >= 5) {
            this.showButton = true;
        }
    }

    private async fetchAllRoomsForExport(): Promise<RoomDto[]> {
        const maxFloorValue = this.maxFloor();
        if (maxFloorValue === 0) {
            return [];
        }

        const allRooms: RoomDto[] = [];
        const roomPath = `${environment.apiUrl}/room`;

        for (let floor = 1; floor <= maxFloorValue; floor++) {
            const rooms = await firstValueFrom(this.http.get<RoomDto[]>(`${roomPath}/floor/${floor}/all`));
            allRooms.push(...rooms);
        }

        return allRooms;
    }

    private convertRoomsToCsv(rooms: RoomDto[]): string {
        const lines: string[] = [];

        // Only export rooms from floor 1 since all floors have the same layout
        const floor1Rooms = rooms.filter((room) => room.floor === 1);
        const roomsToExport = floor1Rooms.length > 0 ? floor1Rooms : rooms;

        let shapeId = 1;
        const seenShapes = new Set<string>();

        for (const room of roomsToExport) {
            const shapeKey = room.svgPoints.map((p) => `${p.x},${p.y}`).join('|');
            if (seenShapes.has(shapeKey)) {
                continue;
            }
            seenShapes.add(shapeKey);

            for (let index = 0; index < room.svgPoints.length; index++) {
                const point = room.svgPoints[index];
                // Format: SHAPE_ID,X,Y,POINT_INDEX
                lines.push(`${shapeId},${point.x},${point.y},${index}`);
            }
            shapeId++;
        }

        return lines.join('\n');
    }

    private downloadCsv(content: string, filename: string): void {
        const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.setAttribute('href', url);
        link.setAttribute('download', filename);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    }
}
