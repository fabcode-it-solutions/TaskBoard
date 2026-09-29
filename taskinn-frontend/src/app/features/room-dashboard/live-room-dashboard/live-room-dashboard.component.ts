import { Component, computed, effect, inject, OnDestroy, OnInit, signal, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

import { RoomDashboardService } from '../room-dashboard.service';
import { RoomDashboardStore } from '../room-dashboard.store';
import { DashboardRoomDto } from '../interfaces/room-dashboard.interfaces';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { Point, RoomStatus } from '../../room/room';
import { RoomStore } from '../../room/room.store';
import { RoomService } from '../../room/room.service';
import { SvgUtilityService } from '../../../shared/services/svg-utility.service';
import { RoomStatusUtilService } from '../../../shared/services/room-status-util.service';

@Component({
    selector: 'tinn-room-dashboard-page',
    standalone: true,
    imports: [CommonModule, MatIconModule, MatButtonModule, PageHeaderComponent],
    templateUrl: './live-room-dashboard.component.html',
})
export class LiveRoomDashboardComponent implements OnInit, OnDestroy {
    private readonly dashboardService = inject(RoomDashboardService);
    private readonly store = inject(RoomDashboardStore);
    private readonly roomStore = inject(RoomStore);
    private readonly roomService = inject(RoomService);
    private readonly svgUtil = inject(SvgUtilityService);
    private readonly roomStatusUtil = inject(RoomStatusUtilService);

    public readonly loading: Signal<boolean> = this.store.loading;
    public readonly rooms: Signal<DashboardRoomDto[]> = computed(() => {
        return Array.from(this.store.rooms().values());
    });

    public selectedFloor = signal<number>(1);
    public selectedRoomId = signal<number | null>(null);
    public selectedRoom = computed(() => {
        const id = this.selectedRoomId();
        if (!id) {
            return null;
        }

        return this.rooms().find((r) => r.roomId === id) ?? null;
    });

    public readonly RoomStatus = RoomStatus;

    public blueprintImage = 'hotel-floor-blueprint/Hotel_Blueprint.jpg';

    public readonly roomsForSelectedFloor = computed(() => {
        const floor = this.selectedFloor();
        return this.rooms().filter((room) => room.floorNumber === floor);
    });

    private readonly latestRoomUpdate = toSignal(this.dashboardService.roomUpdates$);

    public floorButtonClasses = (floor: number): Signal<string> =>
        computed((): string =>
            this.selectedFloor() === floor
                ? 'bg-brand-primary text-white hover:bg-brand-600'
                : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200',
        );

    constructor() {
        effect(() => {
            const updatedRoom = this.latestRoomUpdate();
            if (updatedRoom) {
                this.store.updateSingleRoom(updatedRoom);
            }
        });
    }

    async ngOnInit(): Promise<void> {
        await Promise.all([this.roomService.getMaxFloor(), this.dashboardService.fetchInitialRoomStates()]);
        this.dashboardService.connect();
    }

    ngOnDestroy(): void {
        this.dashboardService.disconnect();
    }

    public availableFloors = computed(() => {
        const max = this.roomStore.maxFloor() ?? 0;
        return Array.from({ length: max }, (_, i) => i + 1);
    });

    public onFloorSelect(floor: number): void {
        this.selectedFloor.set(floor);
        this.selectedRoomId.set(null);
    }

    public onRoomClick(roomId: number): void {
        this.selectedRoomId.set(roomId);
    }

    public deselectRoom(): void {
        this.selectedRoomId.set(null);
    }

    public displayStatus(status: RoomStatus): string {
        if (!status) {
            return '';
        }
        return this.roomStatusUtil
            .getStatusLabel(status)
            .toLowerCase()
            .replace(/\b\w/g, (char) => char.toUpperCase());
    }

    public pointsToPolygonString(points: Point[]): string {
        if (!points) {
            return '';
        }
        return this.svgUtil.pointsToPolygonString(points);
    }

    public getCenterX(points: Point[]): number {
        if (!points || points.length === 0) {
            return 0;
        }
        return this.svgUtil.getCenterX(points);
    }

    public getCenterY(points: Point[]): number {
        if (!points || points.length === 0) {
            return 0;
        }
        return this.svgUtil.getCenterY(points);
    }
}
