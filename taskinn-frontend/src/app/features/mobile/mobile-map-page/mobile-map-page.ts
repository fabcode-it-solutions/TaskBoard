import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    OnDestroy,
    OnInit,
    signal,
    ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

import { RoomService } from '../../room/room.service';
import { RoomTypeService } from '../../room/room-type.service';
import { MobileHeaderComponent } from '../components/mobile-header/mobile-header';
import { Point, RoomDto, RoomStatus } from '../../room/room';
import { CleaningTaskService } from '../cleaning-task.service';
import { DemoTimeService } from '../demo-time.service';
import { CleaningTaskDto, TaskStatus } from '../cleaning-task';
import { ReportDialogComponent } from '../components/report-dialog/report-dialog.component';
import { MinibarDialogComponent } from '../components/minibar-dialog/minibar-dialog.component';
import { MinibarService } from '../minibar.service';
import { NavigationSequence, NavigationStep } from '../navigation-sequence';
import { MobileNavigationService } from '../mobile-navigation.service';
import { AuthService } from '../../auth/auth.service';
import { RoomStatusUtilService } from '../../../shared/services/room-status-util.service';
import { SvgUtilityService } from '../../../shared/services/svg-utility.service';
import { StaircaseService } from '../../staircase/staircase.service';
import { StaircaseDto } from '../../staircase/staircase';

interface TransformState {
    x: number;
    y: number;
    scale: number;
}

interface TouchPoint {
    x: number;
    y: number;
}

interface TouchGestureState {
    startTransform: TransformState;
    startTouches: TouchPoint[];
    initialDistance: number | null;
    startScreenCenter: TouchPoint | null;
    startSvgCenter: TouchPoint | null;
}

@Component({
    selector: 'tinn-mobile-map-page',
    imports: [CommonModule, FormsModule, MatIcon, MobileHeaderComponent],
    templateUrl: './mobile-map-page.html',
    styleUrl: './mobile-map-page.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MobileMapPageComponent implements OnInit, OnDestroy {
    @ViewChild('mapSvg', { static: false }) public mapSvg!: ElementRef<SVGSVGElement>;

    private readonly roomService = inject(RoomService);
    private readonly roomTypeService = inject(RoomTypeService);
    private readonly cleaningTaskService = inject(CleaningTaskService);
    private readonly staircaseService = inject(StaircaseService);
    private readonly minibarService = inject(MinibarService);
    private readonly authService = inject(AuthService);
    private readonly route = inject(ActivatedRoute);
    private readonly dialog = inject(MatDialog);
    private readonly toastr = inject(ToastrService);
    private readonly navigationService = inject(MobileNavigationService);
    public readonly demoTimeService = inject(DemoTimeService);
    private readonly roomStatusUtil = inject(RoomStatusUtilService);
    private readonly svgUtil = inject(SvgUtilityService);

    public readonly blueprintImage = 'hotel-floor-blueprint/Hotel_Blueprint.jpg';
    public readonly RoomStatus = RoomStatus;

    private readonly MIN_SCALE = 0.5;
    private readonly MAX_SCALE = 5;
    private readonly TAP_THRESHOLD = 10; // pixels
    private readonly ZOOM_SENSITIVITY = 0.05; // Lower = slower zoom
    private readonly PAN_SENSITIVITY = 1.5; // Higher = faster pan
    private readonly TOUCH_PAN_SENSITIVITY = 3.4; // Higher = faster touch pan
    private readonly ROOM_ZOOM_SCALE = 4; // Zoom level when room is selected
    private readonly INITIAL_ZOOM_SCALE = 1.8; // Initial zoom level when map loads
    private readonly ZOOM_ANIMATION_DURATION = 400; // Animation duration in milliseconds
    private readonly SHOW_TIME_SCALE_THRESHOLD = 2.5; // Show scheduled time on polygons when zoomed in past this level

    public selectedFloor = signal<number>(1);
    public maxFloor = computed(() => this.roomService.maxFloor() ?? 0);
    public floors = computed(() => {
        const max = this.maxFloor();
        return Array.from({ length: max }, (_, i) => i + 1);
    });

    public currentFloorRooms = computed(() => this.roomService.roomsByFloor() ?? []);
    public currentFloorStaircases = computed(() => this.staircaseService.staircasesByFloor() ?? []);
    public loading = this.roomService.loading;
    public error = computed(() => this.roomService.error());

    public transformState = signal<TransformState>({ x: 0, y: 0, scale: 1 });

    public needsRecenter = computed(() => {
        const transform = this.transformState();
        return transform.scale !== 1 || Math.abs(transform.x) > 10 || Math.abs(transform.y) > 10;
    });

    public isDetailZoom = computed(() => {
        return this.transformState().scale >= this.SHOW_TIME_SCALE_THRESHOLD;
    });

    public selectedRoom = signal<RoomDto | null>(null);

    public currentCleaningTask = this.cleaningTaskService.currentTask;
    public cleaningLoading = this.cleaningTaskService.loading;
    public cleaningError = this.cleaningTaskService.error;
    public inProgressTasks = this.cleaningTaskService.inProgressTasks;

    public hasActiveCleaningTask = computed(() => {
        const task = this.currentCleaningTask();
        const room = this.selectedRoom();
        return task && room && task.roomId === room.id && task.status === TaskStatus.IN_PROGRESS;
    });

    public isSelectedRoomAlreadyCleaned = computed(() => {
        const room = this.selectedRoom();
        const tasks = this.dailyTasks();

        if (!room || tasks.length === 0) {
            return false;
        }

        return tasks.some((task) => task.roomId === room.id && task.status === TaskStatus.COMPLETED);
    });

    public isSelectedRoomAlreadySkipped = computed(() => {
        const room = this.selectedRoom();
        const tasks = this.dailyTasks();

        if (!room || tasks.length === 0) {
            return false;
        }

        return tasks.some((task) => task.roomId === room.id && task.status === TaskStatus.BLOCKED);
    });

    public userHasActiveCleaningTask = computed(() => {
        const userProfile = this.authService.userProfile();
        if (!userProfile) {
            return null;
        }
        return this.cleaningTaskService.getActiveTaskForEmployee(userProfile.id);
    });

    public canStartCleaning = computed(
        () =>
            !this.hasActiveCleaningTask() &&
            !this.userHasActiveCleaningTask() &&
            !this.cleaningLoading() &&
            this.isSelectedRoomAssigned() &&
            !this.isSelectedRoomAlreadyCleaned() &&
            !this.isSelectedRoomAlreadySkipped(),
    );

    public canFinishCleaning = computed(
        () => this.hasActiveCleaningTask() && !this.cleaningLoading() && this.isSelectedRoomAssigned(),
    );

    public roomsBeingCleaned = computed(() => {
        const tasks = this.inProgressTasks();
        return new Set(tasks.map((task) => task.roomId));
    });

    public dailyTasks = this.cleaningTaskService.dailyTasks;

    public isSelectedRoomAssigned = computed(() => {
        const room = this.selectedRoom();
        const tasks = this.dailyTasks();

        if (!room || tasks.length === 0) {
            return false;
        }

        return tasks.some((task) => task.roomId === room.id);
    });

    public canReportMinibarForRoom = signal<boolean>(false);
    public minibarEligibilityLoading = signal<boolean>(false);

    private disabledMinibarClickCount = 0;
    private disabledMinibarClickTimer: ReturnType<typeof setTimeout> | null = null;
    private readonly DOUBLE_CLICK_THRESHOLD = 600;

    public canReportMinibar = computed(() => {
        return this.canReportMinibarForRoom();
    });

    public minibarDisabledReason = computed(() => {
        if (!this.canReportMinibarForRoom()) {
            return 'No active or recent booking for this room';
        }
        return '';
    });

    public selectedRoomTask = computed(() => {
        const room = this.selectedRoom();
        const tasks = this.dailyTasks();
        if (!room || tasks.length === 0) {
            return null;
        }
        return tasks.find((task) => task.roomId === room.id) ?? null;
    });

    public tasksByRoomId = computed(() => {
        const tasks = this.dailyTasks();
        const map = new Map<number, CleaningTaskDto>();
        tasks.forEach((task) => map.set(task.roomId, task));
        return map;
    });

    public navigationSequence = computed<NavigationSequence>(() => {
        return this.generateNavigationSequenceFromTasks(this.dailyTasks());
    });

    public completedRoomNumbers = computed<Set<string>>(() => {
        const tasks = this.dailyTasks();
        const completedTasks = tasks.filter((task) => task.status === TaskStatus.COMPLETED);
        return new Set(completedTasks.map((task) => task.roomNumber));
    });

    public isAllTasksComplete = computed(() => {
        const tasks = this.dailyTasks();
        if (tasks.length === 0) {
            return false;
        }
        return tasks.every((task) => task.status === TaskStatus.COMPLETED || task.status === TaskStatus.BLOCKED);
    });

    public currentSequenceIndex = computed(() => {
        const completed = this.completedRoomNumbers();
        const sequence = this.navigationSequence();
        return sequence.roomNumbers.findIndex((number) => !completed.has(number));
    });

    public nextRoomInSequence = computed(() => {
        const index = this.currentSequenceIndex();
        const sequence = this.navigationSequence();
        return index >= 0 && index < sequence.roomNumbers.length ? sequence.roomNumbers[index] : null;
    });

    public nextRoomOnCurrentFloor = computed(() => {
        const nextRoomNumber = this.nextRoomInSequence();
        if (!nextRoomNumber) {
            return null;
        }
        const rooms = this.currentFloorRooms();
        return rooms.find((r) => r.number === nextRoomNumber) ?? null;
    });

    public navigationSteps = signal<NavigationStep[]>([]);

    public roomMetadata = computed(() => {
        const rooms = this.currentFloorRooms();
        const metadata = new Map<
            number,
            {
                centerX: number;
                centerY: number;
                fillColor: string;
                strokeColor: string;
                polygonString: string;
            }
        >();

        rooms.forEach((room) => {
            const points = room.svgPoints;
            const centerX = this.svgUtil.getCenterX(points);
            const centerY = this.svgUtil.getCenterY(points);

            metadata.set(room.id, {
                centerX,
                centerY,
                fillColor: this.roomStatusUtil.getSimplifiedRoomColor(room.status),
                strokeColor: this.roomStatusUtil.getSimplifiedRoomStrokeColor(room.status),
                polygonString: this.svgUtil.pointsToPolygonString(points),
            });
        });

        return metadata;
    });

    public navigationStepsWithRooms = computed(() => {
        const steps = this.navigationSteps();
        const rooms = this.currentFloorRooms();
        const staircases = this.currentFloorStaircases();
        const metadata = this.roomMetadata();

        return steps.map((step) => {
            const fromRoom = rooms.find((r) => r.number === step.fromRoomNumber) ?? null;
            const toRoom = rooms.find((r) => r.number === step.toRoomNumber) ?? null;

            let arrowPath: string | null = null;
            let targetStaircase: StaircaseDto | null = null;
            let staircaseToRoomArrowPath: string | null = null;
            let sourceStaircase: StaircaseDto | null = null;

            if (fromRoom) {
                const fromMeta = metadata.get(fromRoom.id);

                if (step.isCrossFloor && fromMeta) {
                    const fromCenter = { x: fromMeta.centerX, y: fromMeta.centerY };
                    targetStaircase = this.findNearestStaircase(fromCenter, staircases);

                    if (targetStaircase) {
                        const toCenter = { x: targetStaircase.x, y: targetStaircase.y };
                        const midX = (fromCenter.x + toCenter.x) / 2;
                        const midY = (fromCenter.y + toCenter.y) / 2;
                        const controlOffset = 30;

                        arrowPath = `M ${fromCenter.x},${fromCenter.y} Q ${midX},${midY - controlOffset} ${toCenter.x},${toCenter.y}`;
                    }
                } else if (toRoom && fromMeta) {
                    // Same-floor navigation: point arrow to destination room
                    const toMeta = metadata.get(toRoom.id);

                    if (toMeta) {
                        const fromCenter = { x: fromMeta.centerX, y: fromMeta.centerY };
                        const toCenter = { x: toMeta.centerX, y: toMeta.centerY };
                        const midX = (fromCenter.x + toCenter.x) / 2;
                        const midY = (fromCenter.y + toCenter.y) / 2;
                        const controlOffset = 30;

                        arrowPath = `M ${fromCenter.x},${fromCenter.y} Q ${midX},${midY - controlOffset} ${toCenter.x},${toCenter.y}`;
                    }
                }
            }

            if (step.isCrossFloor && toRoom && step.toFloor === this.selectedFloor()) {
                const toMeta = metadata.get(toRoom.id);
                if (toMeta) {
                    const toCenter = { x: toMeta.centerX, y: toMeta.centerY };
                    sourceStaircase = this.findNearestStaircase(toCenter, staircases);

                    if (sourceStaircase) {
                        const fromCenter = { x: sourceStaircase.x, y: sourceStaircase.y };
                        const midX = (fromCenter.x + toCenter.x) / 2;
                        const midY = (fromCenter.y + toCenter.y) / 2;
                        const controlOffset = 30;

                        staircaseToRoomArrowPath = `M ${fromCenter.x},${fromCenter.y} Q ${midX},${midY - controlOffset} ${toCenter.x},${toCenter.y}`;
                    }
                }
            }

            return {
                ...step,
                fromRoom,
                toRoom,
                fromMeta: fromRoom ? (metadata.get(fromRoom.id) ?? null) : null,
                toMeta: toRoom ? (metadata.get(toRoom.id) ?? null) : null,
                arrowPath,
                targetStaircase,
                staircaseToRoomArrowPath,
                sourceStaircase,
            };
        });
    });

    private gestureState = signal<TouchGestureState | null>(null);
    private hasShownDayCompleteToast = signal(false);
    public isLegendExpanded = signal(false);
    private isDragging = false;
    private lastMousePosition: { x: number; y: number } | null = null;
    private animationFrameId: number | null = null;
    private inertiaFrameId: number | null = null;
    private lastTouchMoveTime: number | null = null;
    private lastTouchMovePoint: TouchPoint | null = null;
    private touchVelocity: TouchPoint = { x: 0, y: 0 };
    private timeUpdateInterval: ReturnType<typeof setInterval> | null = null;

    public currentTimeDisplay = signal<string>('');

    constructor() {
        this.currentTimeDisplay.set(this.formatCurrentTime());
        this.timeUpdateInterval = setInterval(() => {
            this.currentTimeDisplay.set(this.formatCurrentTime());
        }, 1000);
        effect(() => {
            const steps = this.navigationSteps();
            const step = steps[0];

            if (step && step.isCrossFloor && step.fromFloor === this.selectedFloor()) {
                const direction = step.toFloor > step.fromFloor ? 'up' : 'down';
                this.toastr.info(
                    `Go ${direction} to Floor ${step.toFloor} for room ${step.toRoomNumber}`,
                    'Floor Change Required',
                    {
                        timeOut: 5000,
                        progressBar: true,
                    },
                );
            }
        });

        effect(() => {
            const isComplete = this.isAllTasksComplete();
            const tasks = this.dailyTasks();
            const hasShown = this.hasShownDayCompleteToast();

            if (isComplete && tasks.length > 0 && !hasShown) {
                this.hasShownDayCompleteToast.set(true);
                this.toastr.success(
                    'Great work! You have completed all your cleaning tasks for today.',
                    'Day Complete!',
                    { timeOut: 8000, progressBar: true },
                );
            }

            if (!isComplete && hasShown) {
                this.hasShownDayCompleteToast.set(false);
            }
        });
    }

    public async ngOnInit(): Promise<void> {
        // Parse demo time from query param
        const currentTimeParam = this.route.snapshot.queryParamMap.get('currentTime');
        this.demoTimeService.setDemoTimeFromQuery(currentTimeParam);

        const floorParam = this.route.snapshot.queryParamMap.get('floor');
        const roomParam = this.route.snapshot.queryParamMap.get('room');
        const initialFloor = floorParam ? parseInt(floorParam, 10) : 1;
        this.selectedFloor.set(initialFloor);

        await this.roomService.getMaxFloor();
        await this.roomService.getRoomsByFloor(initialFloor);
        await this.staircaseService.getStaircasesByFloor(initialFloor);
        await this.roomTypeService.getAllRoomTypes();
        await this.cleaningTaskService.getAllInProgressTasks();

        // Fetch daily tasks for navigation sequence
        const userProfile = this.authService.userProfile();
        if (userProfile) {
            const today = this.demoTimeService.getCurrentTime();
            await this.cleaningTaskService.getDailyTasksForEmployee(
                userProfile.id,
                today.getFullYear(),
                today.getMonth() + 1,
                today.getDate(),
            );
        } else {
            console.warn('No user profile found, navigation sequence will be unavailable');
        }

        if (roomParam) {
            const room = this.findRoomByNumber(roomParam);
            if (room) {
                setTimeout(() => {
                    this.onRoomSelected(room);
                }, 100);
            }
        } else {
            setTimeout(() => {
                this.zoomToCenter();
            }, 100);
        }
    }

    public async onFloorChange(floor: number | string): Promise<void> {
        if (this.loading()) {
            return;
        }

        const floorNumber = typeof floor === 'string' ? parseInt(floor, 10) : floor;

        this.selectedFloor.set(floorNumber);
        this.selectedRoom.set(null);

        // Only clear navigation if not arriving at cross-floor destination
        const currentStep = this.navigationSteps()[0];
        if (!currentStep?.isCrossFloor || currentStep.toFloor !== floorNumber) {
            this.navigationSteps.set([]);
        }

        await this.navigationService.navigateWithPreservedParams([], {
            relativeTo: this.route,
            queryParams: { floor: floorNumber },
            queryParamsHandling: 'merge',
            replaceUrl: true,
        });

        await this.roomService.getRoomsByFloor(floorNumber);
        await this.staircaseService.getStaircasesByFloor(floorNumber);
        await this.cleaningTaskService.getAllInProgressTasks();

        setTimeout(() => {
            this.zoomToCenter();
        }, 100);
    }

    public onBackClick(): void {
        this.navigationService.back('/mobile');
    }

    public getCenterX(points: Point[]): number {
        return this.svgUtil.getCenterX(points);
    }

    public getCenterY(points: Point[]): number {
        return this.svgUtil.getCenterY(points);
    }

    public getTransformString(): string {
        return this.svgUtil.getTransformString(this.transformState());
    }

    public onTouchStart(event: TouchEvent): void {
        this.cancelZoomAnimation();
        this.cancelInertia();
        event.preventDefault();

        const touches = Array.from(event.touches).map((t) => ({
            x: t.clientX,
            y: t.clientY,
        }));

        const currentTransform = this.transformState();

        let initialDistance = null;
        let startScreenCenter: TouchPoint | null = null;
        let startSvgCenter: TouchPoint | null = null;
        if (touches.length === 2) {
            initialDistance = this.calculateDistance(touches[0], touches[1]);
            startScreenCenter = this.getTouchCenter(touches);
            startSvgCenter = this.screenToSVGPoint(startScreenCenter.x, startScreenCenter.y);
        }

        this.gestureState.set({
            startTransform: { ...currentTransform },
            startTouches: touches,
            initialDistance,
            startScreenCenter,
            startSvgCenter,
        });

        this.lastTouchMoveTime = performance.now();
        this.lastTouchMovePoint = touches[0] ?? null;
        this.touchVelocity = { x: 0, y: 0 };
    }

    public onTouchMove(event: TouchEvent): void {
        event.preventDefault();

        const gestureState = this.gestureState();
        if (!gestureState) {
            return;
        }

        const touches = Array.from(event.touches).map((t) => ({
            x: t.clientX,
            y: t.clientY,
        }));

        if (touches.length === 1 && gestureState.startTouches.length === 1) {
            this.handlePan(touches[0], gestureState);
            this.trackTouchVelocity(touches[0]);
        } else if (touches.length === 2 && gestureState.initialDistance !== null) {
            this.handlePinch(touches, gestureState);
        }
    }

    public onTouchEnd(event: TouchEvent): void {
        event.preventDefault();

        if (event.touches.length === 0) {
            const gestureState = this.gestureState();
            if (gestureState && this.wasTap(gestureState)) {
                this.handleTap(event.changedTouches[0]);
            }
            this.gestureState.set(null);
            this.startInertiaIfNeeded();
        } else {
            const touches = Array.from(event.touches).map((t) => ({
                x: t.clientX,
                y: t.clientY,
            }));
            const currentGesture = this.gestureState();
            if (currentGesture) {
                const startScreenCenter = touches.length === 2 ? this.getTouchCenter(touches) : null;
                const startSvgCenter = startScreenCenter
                    ? this.screenToSVGPoint(startScreenCenter.x, startScreenCenter.y)
                    : null;
                this.gestureState.update((state) => ({
                    ...state!,
                    startTouches: touches,
                    startTransform: this.transformState(),
                    initialDistance: touches.length === 2 ? this.calculateDistance(touches[0], touches[1]) : null,
                    startScreenCenter,
                    startSvgCenter,
                }));
            }
        }
    }

    private handlePan(touch: TouchPoint, gestureState: TouchGestureState): void {
        const dx = (touch.x - gestureState.startTouches[0].x) * this.TOUCH_PAN_SENSITIVITY;
        const dy = (touch.y - gestureState.startTouches[0].y) * this.TOUCH_PAN_SENSITIVITY;

        this.transformState.update((state) => ({
            ...state,
            x: gestureState.startTransform.x + dx,
            y: gestureState.startTransform.y + dy,
        }));
    }

    private handlePinch(touches: TouchPoint[], gestureState: TouchGestureState): void {
        const currentDistance = this.calculateDistance(touches[0], touches[1]);
        const initialDistance = gestureState.initialDistance!;

        const scaleDelta = currentDistance / initialDistance;
        const newScale = Math.max(
            this.MIN_SCALE,
            Math.min(this.MAX_SCALE, gestureState.startTransform.scale * scaleDelta),
        );

        const center = this.getTouchCenter(touches);
        const startSvgCenter =
            gestureState.startSvgCenter ??
            this.screenToSVGPoint(
                (gestureState.startTouches[0].x + gestureState.startTouches[1].x) / 2,
                (gestureState.startTouches[0].y + gestureState.startTouches[1].y) / 2,
            );

        const svg = this.mapSvg.nativeElement;
        const pt = svg.createSVGPoint();
        pt.x = center.x;
        pt.y = center.y;
        const svgP = pt.matrixTransform(svg.getScreenCTM()!.inverse());

        const newX = svgP.x - startSvgCenter.x * newScale;
        const newY = svgP.y - startSvgCenter.y * newScale;

        this.transformState.set({
            scale: newScale,
            x: newX,
            y: newY,
        });
    }

    private calculateDistance(p1: TouchPoint, p2: TouchPoint): number {
        return this.svgUtil.distance(p1, p2);
    }

    private trackTouchVelocity(touch: TouchPoint): void {
        const now = performance.now();
        const lastTime = this.lastTouchMoveTime;
        const lastPoint = this.lastTouchMovePoint;

        if (lastTime !== null && lastPoint) {
            const dt = Math.max(now - lastTime, 1);
            const dx = (touch.x - lastPoint.x) * this.TOUCH_PAN_SENSITIVITY;
            const dy = (touch.y - lastPoint.y) * this.TOUCH_PAN_SENSITIVITY;
            this.touchVelocity = { x: dx / dt, y: dy / dt };
        }

        this.lastTouchMoveTime = now;
        this.lastTouchMovePoint = { ...touch };
    }

    private startInertiaIfNeeded(): void {
        const minSpeed = 0.05;
        const speed = Math.hypot(this.touchVelocity.x, this.touchVelocity.y);
        if (speed < minSpeed) {
            return;
        }

        const decay = 0.92;
        const step = (lastTime: number): void => {
            const now = performance.now();
            const dt = Math.max(now - lastTime, 1);

            this.touchVelocity = {
                x: this.touchVelocity.x * decay,
                y: this.touchVelocity.y * decay,
            };

            this.transformState.update((state) => ({
                ...state,
                x: state.x + this.touchVelocity.x * dt,
                y: state.y + this.touchVelocity.y * dt,
            }));

            if (Math.hypot(this.touchVelocity.x, this.touchVelocity.y) < minSpeed) {
                this.inertiaFrameId = null;
                return;
            }

            this.inertiaFrameId = requestAnimationFrame(() => step(now));
        };

        this.inertiaFrameId = requestAnimationFrame(() => step(performance.now()));
    }

    private cancelInertia(): void {
        if (this.inertiaFrameId !== null) {
            cancelAnimationFrame(this.inertiaFrameId);
            this.inertiaFrameId = null;
        }
    }

    private getTouchCenter(touches: TouchPoint[]): TouchPoint {
        if (touches.length === 0) {
            return { x: 0, y: 0 };
        }
        if (touches.length === 1) {
            return { ...touches[0] };
        }
        return {
            x: (touches[0].x + touches[1].x) / 2,
            y: (touches[0].y + touches[1].y) / 2,
        };
    }

    private wasTap(gestureState: TouchGestureState): boolean {
        const current = this.transformState();

        const dx = Math.abs(current.x - gestureState.startTransform.x);
        const dy = Math.abs(current.y - gestureState.startTransform.y);

        return dx < this.TAP_THRESHOLD && dy < this.TAP_THRESHOLD;
    }

    private handleTap(touch: Touch): void {
        try {
            const svgPoint = this.screenToSVGPoint(touch.clientX, touch.clientY);
            const tappedRoom = this.findRoomAtPoint(svgPoint.x, svgPoint.y);

            if (tappedRoom) {
                this.onRoomSelected(tappedRoom);
            }
        } catch {
            // Ignore errors from tapping on non-room SVG elements
        }
    }

    /**
     * Converts screen coordinates to SVG coordinates, accounting for zoom/pan transform
     */
    private screenToSVGPoint(screenX: number, screenY: number): { x: number; y: number } {
        const svg = this.mapSvg.nativeElement;
        return this.svgUtil.screenToSVGPoint(svg, screenX, screenY, this.transformState());
    }

    private findRoomAtPoint(x: number, y: number): RoomDto | null {
        const rooms = this.currentFloorRooms();

        for (let i = rooms.length - 1; i >= 0; i--) {
            const room = rooms[i];
            if (this.isPointInPolygon(x, y, room.svgPoints)) {
                return room;
            }
        }

        return null;
    }

    private isPointInPolygon(x: number, y: number, polygon: Point[]): boolean {
        return this.svgUtil.isPointInPolygon({ x, y }, polygon);
    }

    public async onRoomSelected(room: RoomDto): Promise<void> {
        this.selectedRoom.set(room);
        this.zoomToRoom(room);

        await this.cleaningTaskService.getCurrentTaskForRoom(room.id);
        await this.checkMinibarEligibility(room.id);

        if ('vibrate' in navigator) {
            navigator.vibrate(10);
        }
    }

    private async checkMinibarEligibility(roomId: number): Promise<void> {
        this.minibarEligibilityLoading.set(true);
        this.canReportMinibarForRoom.set(false);

        try {
            const canReport = await this.minibarService.canReportMinibar(roomId);
            this.canReportMinibarForRoom.set(canReport);
        } finally {
            this.minibarEligibilityLoading.set(false);
        }
    }

    public closeRoomDetails(): void {
        this.selectedRoom.set(null);
    }

    public async navigateToNextRoom(): Promise<void> {
        const nextRoomNumber = this.nextRoomInSequence();
        if (!nextRoomNumber) {
            return;
        }

        const targetFloor = parseInt(nextRoomNumber.charAt(0), 10);
        const currentFloor = this.selectedFloor();

        if (targetFloor !== currentFloor) {
            await this.onFloorChange(targetFloor);
            await new Promise((resolve) => setTimeout(resolve, 150));
        }

        const room = this.findRoomByNumber(nextRoomNumber);
        if (room) {
            await this.onRoomSelected(room);
        }
    }

    public recenterMap(): void {
        this.transformState.set({ x: 0, y: 0, scale: 1 });
    }

    public toggleLegend(): void {
        this.isLegendExpanded.update((v) => !v);
    }

    private zoomToCenter(): void {
        this.animateZoomTo({
            centerX: 800,
            centerY: 500,
            targetScale: this.INITIAL_ZOOM_SCALE,
            screenYRatio: 0.5,
        });
    }

    private zoomToRoom(room: RoomDto): void {
        this.animateZoomTo({
            centerX: this.getCenterX(room.svgPoints),
            centerY: this.getCenterY(room.svgPoints),
            targetScale: this.ROOM_ZOOM_SCALE,
            screenYRatio: 0.2,
        });
    }

    private animateZoomTo(options: {
        centerX: number;
        centerY: number;
        targetScale: number;
        screenYRatio: number;
    }): void {
        if (this.animationFrameId !== null) {
            cancelAnimationFrame(this.animationFrameId);
            this.animationFrameId = null;
        }

        const { centerX, centerY, targetScale, screenYRatio } = options;

        const svg = this.mapSvg.nativeElement;
        const bbox = svg.getBoundingClientRect();
        const viewportWidth = bbox.width;
        const viewportHeight = bbox.height;

        const targetScreenX = viewportWidth / 2;
        const targetScreenY = viewportHeight * screenYRatio;

        const pt = svg.createSVGPoint();
        pt.x = bbox.left + targetScreenX;
        pt.y = bbox.top + targetScreenY;
        const svgTarget = pt.matrixTransform(svg.getScreenCTM()!.inverse());

        const targetX = svgTarget.x - centerX * targetScale;
        const targetY = svgTarget.y - centerY * targetScale;

        const startTransform = this.transformState();
        const startTime = performance.now();

        const animate = (currentTime: number): void => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / this.ZOOM_ANIMATION_DURATION, 1);

            const eased = 1 - Math.pow(1 - progress, 3);

            const x = startTransform.x + (targetX - startTransform.x) * eased;
            const y = startTransform.y + (targetY - startTransform.y) * eased;
            const scale = startTransform.scale + (targetScale - startTransform.scale) * eased;

            this.transformState.set({ x, y, scale });

            if (progress < 1) {
                this.animationFrameId = requestAnimationFrame(animate);
            } else {
                this.animationFrameId = null;
            }
        };

        this.animationFrameId = requestAnimationFrame(animate);
    }

    private cancelZoomAnimation(): void {
        if (this.animationFrameId !== null) {
            cancelAnimationFrame(this.animationFrameId);
            this.animationFrameId = null;
        }
    }

    public getStatusColorClass(status: RoomStatus): string {
        return this.roomStatusUtil.getRoomBgClass(status);
    }

    public getStatusLabel(status: RoomStatus): string {
        return this.roomStatusUtil.getStatusLabel(status);
    }

    public isRoomOccupied(status: RoomStatus): boolean {
        return this.roomStatusUtil.isRoomOccupied(status);
    }

    public isRoomAvailable(status: RoomStatus): boolean {
        return this.roomStatusUtil.isRoomAvailable(status);
    }

    public getSelectedRoomTypeName(): string {
        const room = this.selectedRoom();
        if (!room) {
            return 'Unknown';
        }

        const roomTypes = this.roomTypeService.roomTypeList() ?? [];
        const roomType = roomTypes.find((rt) => rt.id === room.roomTypeId);
        return roomType?.name ?? 'Unknown Type';
    }

    public formatScheduledTime(task: CleaningTaskDto): string {
        const start = new Date(task.scheduledStart);
        const end = new Date(task.scheduledEnd);

        const formatTime = (date: Date): string =>
            date.toLocaleTimeString('en-US', {
                hour: 'numeric',
                minute: '2-digit',
                hour12: true,
            });

        return `${formatTime(start)} - ${formatTime(end)}`;
    }

    public formatShortTime(task: CleaningTaskDto): string {
        const start = new Date(task.scheduledStart);
        return start.toLocaleTimeString('en-US', {
            hour: 'numeric',
            minute: '2-digit',
            hour12: true,
        });
    }

    // Trackpad support
    public onWheel(event: WheelEvent): void {
        this.cancelZoomAnimation();
        event.preventDefault();

        const svg = this.mapSvg.nativeElement;
        const currentTransform = this.transformState();

        // Determine zoom direction (negative deltaY = zoom in, positive = zoom out)
        const zoomFactor = event.deltaY < 0 ? 1 + this.ZOOM_SENSITIVITY : 1 - this.ZOOM_SENSITIVITY;
        const newScale = Math.max(this.MIN_SCALE, Math.min(this.MAX_SCALE, currentTransform.scale * zoomFactor));

        // Get mouse position in SVG viewport coordinates
        const pt = svg.createSVGPoint();
        pt.x = event.clientX;
        pt.y = event.clientY;
        const svgP = pt.matrixTransform(svg.getScreenCTM()!.inverse());

        // Calculate zoom center point in content space
        const zoomPointX = (svgP.x - currentTransform.x) / currentTransform.scale;
        const zoomPointY = (svgP.y - currentTransform.y) / currentTransform.scale;

        // Calculate new transform to keep zoom point stationary
        const newX = svgP.x - zoomPointX * newScale;
        const newY = svgP.y - zoomPointY * newScale;

        this.transformState.set({
            scale: newScale,
            x: newX,
            y: newY,
        });
    }

    public onMouseDown(event: MouseEvent): void {
        this.cancelZoomAnimation();
        if (event.button === 0) {
            this.isDragging = false; // Start as not dragging
            this.lastMousePosition = { x: event.clientX, y: event.clientY };
        }
    }

    public onMouseMove(event: MouseEvent): void {
        if (this.lastMousePosition) {
            const dx = Math.abs(event.clientX - this.lastMousePosition.x);
            const dy = Math.abs(event.clientY - this.lastMousePosition.y);

            if (dx > 3 || dy > 3) {
                this.isDragging = true;
            }

            if (this.isDragging) {
                const deltaX = (event.clientX - this.lastMousePosition.x) * this.PAN_SENSITIVITY;
                const deltaY = (event.clientY - this.lastMousePosition.y) * this.PAN_SENSITIVITY;

                this.transformState.update((state) => ({
                    ...state,
                    x: state.x + deltaX,
                    y: state.y + deltaY,
                }));

                this.lastMousePosition = { x: event.clientX, y: event.clientY };
                event.preventDefault();
            }
        }
    }

    public onMouseUp(event: MouseEvent): void {
        if (event.button === 0) {
            const wasDragging = this.isDragging;
            this.isDragging = false;
            this.lastMousePosition = null;

            if (wasDragging) {
                event.preventDefault();
                event.stopPropagation();
            }
        }
    }

    public onClick(event: MouseEvent): void {
        try {
            const svgPoint = this.screenToSVGPoint(event.clientX, event.clientY);
            const clickedRoom = this.findRoomAtPoint(svgPoint.x, svgPoint.y);

            if (clickedRoom) {
                this.onRoomSelected(clickedRoom);
            } else {
                this.closeRoomDetails();
            }
        } catch {
            this.closeRoomDetails();
        }
    }

    public findRoomByNumber(roomNumber: string): RoomDto | null {
        const rooms = this.currentFloorRooms();
        return rooms.find((r) => r.number === roomNumber) ?? null;
    }

    private findNearestStaircase(fromPoint: Point, staircases: StaircaseDto[]): StaircaseDto | null {
        if (staircases.length === 0) {
            return null;
        }

        let nearestStaircase: StaircaseDto | null = null;
        let minDistance = Infinity;

        for (const staircase of staircases) {
            const distance = this.svgUtil.distance(fromPoint, { x: staircase.x, y: staircase.y });
            if (distance < minDistance) {
                minDistance = distance;
                nearestStaircase = staircase;
            }
        }

        return nearestStaircase;
    }

    public generateArrowPath(fromPoints: Point[], toPoints: Point[]): string {
        const fromCenter = {
            x: this.getCenterX(fromPoints),
            y: this.getCenterY(fromPoints),
        };
        const toCenter = {
            x: this.getCenterX(toPoints),
            y: this.getCenterY(toPoints),
        };

        const midX = (fromCenter.x + toCenter.x) / 2;
        const midY = (fromCenter.y + toCenter.y) / 2;
        const controlOffset = 30;

        return `M ${fromCenter.x},${fromCenter.y} Q ${midX},${midY - controlOffset} ${toCenter.x},${toCenter.y}`;
    }

    private generateNavigationSequenceFromTasks(tasks: CleaningTaskDto[]): NavigationSequence {
        const activeTasks = tasks.filter(
            (task) => task.status === TaskStatus.PENDING || task.status === TaskStatus.IN_PROGRESS,
        );

        const sortedTasks = activeTasks
            .filter((task) => task.scheduledStart)
            .sort((a, b) => {
                const timeA = new Date(a.scheduledStart).getTime();
                const timeB = new Date(b.scheduledStart).getTime();
                return timeA - timeB;
            });

        const roomNumbers = sortedTasks.map((task) => task.roomNumber);

        return {
            id: Date.now(),
            name: 'Daily Cleaning Route',
            roomNumbers,
            createdAt: new Date().toISOString(),
        };
    }

    private calculateNavigationSteps(fromRoomNumber: string, nextRoomNumber: string | null): NavigationStep[] {
        if (!nextRoomNumber || !fromRoomNumber) {
            return [];
        }

        const currentFloorRooms = this.currentFloorRooms();
        const currentRoom = currentFloorRooms.find((r) => r.number === fromRoomNumber);
        const nextRoom = currentFloorRooms.find((r) => r.number === nextRoomNumber);

        if (currentRoom && nextRoom) {
            return [
                {
                    fromRoomNumber: fromRoomNumber,
                    toRoomNumber: nextRoomNumber,
                    fromFloor: currentRoom.floor,
                    toFloor: nextRoom.floor,
                    isCrossFloor: false,
                    stepIndex: 0,
                },
            ];
        }

        if (currentRoom && !nextRoom) {
            const targetFloor = parseInt(nextRoomNumber.charAt(0));

            return [
                {
                    fromRoomNumber: fromRoomNumber,
                    toRoomNumber: nextRoomNumber,
                    fromFloor: currentRoom.floor,
                    toFloor: targetFloor,
                    isCrossFloor: true,
                    stepIndex: 0,
                },
            ];
        }

        return [];
    }

    private async refreshDailyTasks(): Promise<void> {
        const userProfile = this.authService.userProfile();
        if (!userProfile) {
            return;
        }

        const today = this.demoTimeService.getCurrentTime();
        await this.cleaningTaskService.getDailyTasksForEmployee(
            userProfile.id,
            today.getFullYear(),
            today.getMonth() + 1,
            today.getDate(),
        );
    }

    public async onStartCleaning(): Promise<void> {
        const room = this.selectedRoom();
        if (!room) {
            return;
        }

        if (!this.isSelectedRoomAssigned()) {
            this.toastr.error('This room is not assigned to you', 'Action Not Allowed', {
                timeOut: 3000,
                progressBar: true,
            });
            return;
        }

        // Check if user has an active task elsewhere
        const activeTask = this.userHasActiveCleaningTask();
        if (activeTask) {
            this.toastr.error(
                `You must finish cleaning Room ${activeTask.roomNumber} before starting a new cleaning task.`,
                'Cleaning In Progress',
                { timeOut: 5000, progressBar: true },
            );
            return;
        }

        const task = await this.cleaningTaskService.startCleaning(room.id);

        if (task) {
            await this.roomService.updateSingleRoomInFloorList(room.id);
            await this.cleaningTaskService.getAllInProgressTasks();
            await this.refreshDailyTasks();

            this.navigationSteps.set([]);
        }
    }

    public async onFinishCleaning(): Promise<void> {
        const room = this.selectedRoom();
        if (!room) {
            return;
        }

        if (!this.isSelectedRoomAssigned()) {
            this.toastr.error('This room is not assigned to you', 'Action Not Allowed', {
                timeOut: 3000,
                progressBar: true,
            });
            return;
        }

        const task = await this.cleaningTaskService.finishCleaning(room.id);

        if (task) {
            await this.roomService.updateSingleRoomInFloorList(room.id);
            await this.cleaningTaskService.getAllInProgressTasks();
            await this.refreshDailyTasks();

            this.closeRoomDetails();

            const nextRoom = this.nextRoomInSequence();
            const steps = this.calculateNavigationSteps(room.number, nextRoom);
            this.navigationSteps.set(steps);
        }
    }

    public isRoomBeingCleaned(roomId: number): boolean {
        return this.roomsBeingCleaned().has(roomId);
    }

    public getCleaningStatusText(): string {
        const task = this.currentCleaningTask();
        if (!task) {
            return '';
        }

        if (task.employeeName) {
            return `Being cleaned by ${task.employeeName}`;
        }

        return 'Cleaning in progress';
    }

    public async onSkipCleaning(): Promise<void> {
        const room = this.selectedRoom();
        if (!room) {
            return;
        }

        if (!this.isSelectedRoomAssigned()) {
            this.toastr.error('This room is not assigned to you', 'Action Not Allowed', {
                timeOut: 3000,
                progressBar: true,
            });
            return;
        }

        const task = await this.cleaningTaskService.skipCleaning(room.id, 'Do Not Disturb');

        if (task) {
            await this.roomService.updateSingleRoomInFloorList(room.id);
            await this.cleaningTaskService.getAllInProgressTasks();
            await this.refreshDailyTasks();

            this.closeRoomDetails();

            // Calculate navigation to next room (same as onFinishCleaning)
            const nextRoom = this.nextRoomInSequence();
            const steps = this.calculateNavigationSteps(room.number, nextRoom);
            this.navigationSteps.set(steps);
        }
    }

    public onReportIssue(): void {
        const room = this.selectedRoom();
        if (!room) {
            return;
        }

        const dialogRef = this.dialog.open(ReportDialogComponent, {
            data: {
                roomId: room.id,
                roomNumber: room.number,
            },
            maxWidth: '90vw',
            width: '480px',
            panelClass: 'mobile-dialog',
        });

        dialogRef.afterClosed().subscribe((result) => {
            if (result) {
                // Report submitted successfully - toast shown by dialog component
            }
        });
    }

    public onDisabledMinibarClick(): void {
        if (this.disabledMinibarClickTimer) {
            clearTimeout(this.disabledMinibarClickTimer);
        }

        this.disabledMinibarClickCount++;

        if (this.disabledMinibarClickCount >= 2) {
            this.toastr.info('No active or recent booking for this room', 'Minibar Unavailable', {
                timeOut: 3000,
                progressBar: true,
            });
            this.disabledMinibarClickCount = 0;
        } else {
            this.disabledMinibarClickTimer = setTimeout(() => {
                this.disabledMinibarClickCount = 0;
            }, this.DOUBLE_CLICK_THRESHOLD);
        }
    }

    public onReportMinibar(): void {
        const room = this.selectedRoom();
        if (!room) {
            return;
        }

        if (!this.canReportMinibarForRoom()) {
            this.toastr.error('No active or recent booking for this room', 'Action Not Allowed', {
                timeOut: 3000,
                progressBar: true,
            });
            return;
        }

        this.dialog.open(MinibarDialogComponent, {
            data: {
                roomId: room.id,
                roomNumber: room.number,
            },
            maxWidth: '90vw',
            width: '480px',
            panelClass: 'mobile-dialog',
        });
    }

    public ngOnDestroy(): void {
        this.cancelZoomAnimation();
        if (this.timeUpdateInterval) {
            clearInterval(this.timeUpdateInterval);
        }
    }

    private formatCurrentTime(): string {
        return this.demoTimeService.getCurrentTime().toLocaleTimeString('en-US', {
            hour: 'numeric',
            minute: '2-digit',
            hour12: true,
        });
    }
}
