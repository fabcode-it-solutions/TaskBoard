import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    computed,
    ElementRef,
    inject,
    OnDestroy,
    OnInit,
    signal,
    ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { ToastrService } from 'ngx-toastr';
import { MobileHeaderComponent } from '../components/mobile-header/mobile-header';
import { CleaningTaskService } from '../cleaning-task.service';
import { AuthService } from '../../auth/auth.service';
import { DemoTimeService } from '../demo-time.service';
import { CleaningTaskDto, TaskStatus, TaskType } from '../cleaning-task';
import { MobileNavigationService } from '../mobile-navigation.service';
import { getTaskStatusColor } from '../../../shared/util/status-colors';
import { RoomStatus } from '../../room/room';
import { RoomStatusUtilService } from '../../../shared/services/room-status-util.service';
import { SvgUtilityService } from '../../../shared/services/svg-utility.service';

interface TaskWithPosition {
    task: CleaningTaskDto;
    top: number;
    height: number;
    startHour: number;
    startMinute: number;
}

interface TouchPoint {
    x: number;
    y: number;
}

interface ZoomGestureState {
    startHourHeight: number;
    startTouches: TouchPoint[];
    initialDistance: number | null;
    startScrollTop: number;
}

interface TaskOverlapGroup {
    tasks: TaskWithPosition[];
    startOffset: number; // Earliest start time in group (px from top)
    endOffset: number; // Latest end time in group (px from top)
    isExpanded: boolean;
}

@Component({
    selector: 'tinn-mobile-task-list-page',
    imports: [CommonModule, MatIcon, MobileHeaderComponent],
    templateUrl: './mobile-task-list-page.html',
    styleUrl: './mobile-task-list-page.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MobileTaskListPageComponent implements OnInit, AfterViewInit, OnDestroy {
    @ViewChild('timelineContainer') private timelineContainer?: ElementRef;

    private readonly authService = inject(AuthService);
    private readonly cleaningTaskService = inject(CleaningTaskService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly toastr = inject(ToastrService);
    private readonly navigationService = inject(MobileNavigationService);
    public readonly demoTimeService = inject(DemoTimeService);
    private readonly roomStatusUtil = inject(RoomStatusUtilService);
    private readonly svgUtil = inject(SvgUtilityService);

    // Timeline constants
    public readonly HOUR_HEIGHT = signal(180);
    public readonly HOURS = Array.from({ length: 24 }, (_, i) => i);
    private readonly DEFAULT_HOUR_HEIGHT = 180;
    private readonly MIN_HOUR_HEIGHT = 40;
    private readonly MAX_HOUR_HEIGHT = 240;
    private readonly ZOOM_ANIMATION_DURATION = 400;
    private readonly MIN_TASK_GAP = 8;

    // Core signals
    public readonly loading = this.cleaningTaskService.loading;
    public readonly error = this.cleaningTaskService.error;
    public readonly dailyTasks = this.cleaningTaskService.dailyTasks;
    public readonly selectedTask = signal<CleaningTaskDto | null>(null);

    private scrollPositionBeforeUpdate: number | null = null;

    // Zoom gesture state
    private readonly zoomGestureState = signal<ZoomGestureState | null>(null);
    private animationFrameId: number | null = null;

    // Overlap group state
    public readonly expandedGroupIndices = signal<Set<number>>(new Set());

    // Expose RoomStatus enum for template
    public readonly RoomStatus = RoomStatus;

    // Expose enums for template
    public readonly TaskStatus = TaskStatus;
    public readonly TaskType = TaskType;

    public readonly currentHour = this.demoTimeService.currentHour;
    public readonly currentMinute = this.demoTimeService.currentMinute;
    public readonly currentTimeOffset = computed(() => {
        return this.currentHour() * this.HOUR_HEIGHT() + (this.currentMinute() / 60) * this.HOUR_HEIGHT();
    });

    public readonly show30MinMarks = computed(() => this.HOUR_HEIGHT() >= 60);
    public readonly show10MinMarks = computed(() => this.HOUR_HEIGHT() >= 120);

    public readonly tasksWithPositions = computed<TaskWithPosition[]>(() => {
        return this.dailyTasks().map((task) => ({
            task,
            top: this.calculateTaskTop(task),
            height: this.calculateTaskHeight(task),
            startHour: new Date(task.scheduledStart).getHours(),
            startMinute: new Date(task.scheduledStart).getMinutes(),
        }));
    });

    public readonly totalTasks = computed(() => this.dailyTasks().length);
    public readonly completedCount = computed(
        () =>
            this.dailyTasks().filter((t) => t.status === TaskStatus.COMPLETED || t.status === TaskStatus.BLOCKED)
                .length,
    );
    public readonly progressPercentage = computed(() => {
        const total = this.totalTasks();
        if (total === 0) {
            return 0;
        }
        return Math.round((this.completedCount() / total) * 100);
    });

    public readonly taskOverlapGroups = computed<TaskOverlapGroup[]>(() => {
        const tasks = this.tasksWithPositions();
        const expandedIndices = this.expandedGroupIndices();
        return this.detectOverlaps(tasks, expandedIndices);
    });

    public userActiveTask = computed(() => {
        const userProfile = this.authService.userProfile();
        if (!userProfile) {
            return null;
        }
        return this.cleaningTaskService.getActiveTaskForEmployee(userProfile.id);
    });

    public async ngOnInit(): Promise<void> {
        const currentTimeParam = this.route.snapshot.queryParamMap.get('currentTime');
        this.demoTimeService.setDemoTimeFromQuery(currentTimeParam);

        await this.loadDailyTasks();
    }

    private async loadDailyTasks(): Promise<void> {
        const userProfile = this.authService.userProfile();

        if (!userProfile) {
            this.toastr.error('Please log in to view your tasks');
            this.router.navigate(['/auth/login'], {
                queryParams: { returnUrl: '/mobile/tasks' },
            });
            return;
        }

        const employeeId = userProfile.id;
        const today = this.demoTimeService.getCurrentTime();

        await this.cleaningTaskService.getDailyTasksForEmployee(
            employeeId,
            today.getFullYear(),
            today.getMonth() + 1,
            today.getDate(),
        );
    }

    public ngAfterViewInit(): void {
        // Scroll to current time after view is initialized
        setTimeout(() => this.scrollToCurrentTime(), 100);
    }

    public onBackClick(): void {
        this.navigationService.back('/mobile');
    }

    public onTaskClick(task: CleaningTaskDto): void {
        this.selectedTask.set(task);
    }

    public closeTaskModal(): void {
        this.selectedTask.set(null);
    }

    public navigateToMap(task: CleaningTaskDto): void {
        const floor = parseInt(task.roomNumber.charAt(0));

        this.navigationService.navigateWithPreservedParams(['/mobile/map'], {
            queryParams: {
                floor,
                room: task.roomNumber,
            },
        });

        this.closeTaskModal();
    }

    public async startCleaning(task: CleaningTaskDto): Promise<void> {
        // Check if user has an active task elsewhere
        const activeTask = this.userActiveTask();
        if (activeTask && activeTask.id !== task.id) {
            this.toastr.error(
                `You must finish cleaning Room ${activeTask.roomNumber} before starting a new cleaning task.`,
                'Cleaning In Progress',
                { timeOut: 5000, progressBar: true },
            );
            return;
        }

        this.captureScrollPosition();

        try {
            const result = await this.cleaningTaskService.startCleaning(task.roomId);
            if (result && !this.cleaningTaskService.error()) {
                this.toastr.success(`Started cleaning Room ${task.roomNumber}`);

                await this.cleaningTaskService.getAllInProgressTasks();
                await this.loadDailyTasks();

                this.updateSelectedTaskState();
                this.restoreScrollPosition();
            } else {
                const error = this.cleaningTaskService.error();
                if (error) {
                    this.toastr.error(error.message, 'Cannot Start Cleaning');
                    this.cleaningTaskService.clearError();
                }
                this.restoreScrollPosition();
            }
        } catch {
            this.restoreScrollPosition();
        }
    }

    public async finishCleaning(task: CleaningTaskDto): Promise<void> {
        this.captureScrollPosition();

        try {
            const result = await this.cleaningTaskService.finishCleaning(task.roomId);
            if (result && !this.cleaningTaskService.error()) {
                this.toastr.success(`Completed cleaning Room ${task.roomNumber}`);

                // Close card when finished (desired behavior)
                this.closeTaskModal();

                await this.cleaningTaskService.getAllInProgressTasks();
                await this.loadDailyTasks();
                this.restoreScrollPosition();
            } else {
                const error = this.cleaningTaskService.error();
                if (error) {
                    this.toastr.error(error.message, 'Cannot Finish Cleaning');
                    this.cleaningTaskService.clearError();
                }
                this.restoreScrollPosition();
            }
        } catch {
            this.restoreScrollPosition();
        }
    }

    public async skipCleaning(task: CleaningTaskDto): Promise<void> {
        const reason = prompt('Reason for skipping this task:', 'Do Not Disturb');
        if (reason === null || reason.trim().length === 0) {
            return;
        }

        this.captureScrollPosition();

        try {
            const result = await this.cleaningTaskService.skipCleaning(task.roomId, reason);
            if (result && !this.cleaningTaskService.error()) {
                this.toastr.info(`Skipped cleaning Room ${task.roomNumber}`);

                // Close card when skipped (desired behavior)
                this.closeTaskModal();

                await this.cleaningTaskService.getAllInProgressTasks();
                await this.loadDailyTasks();
                this.restoreScrollPosition();
            } else {
                const error = this.cleaningTaskService.error();
                if (error) {
                    this.toastr.error(error.message, 'Cannot Skip Cleaning');
                    this.cleaningTaskService.clearError();
                }
                this.restoreScrollPosition();
            }
        } catch {
            this.restoreScrollPosition();
        }
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

    public formatHourLabel(hour: number): string {
        return `${hour.toString().padStart(2, '0')}:00`;
    }

    public formatCurrentTime(): string {
        const hour = this.currentHour().toString().padStart(2, '0');
        const minute = this.currentMinute().toString().padStart(2, '0');
        return `${hour}:${minute}`;
    }

    public getStatusLabel(status: TaskStatus): string {
        return status.replace(/_/g, ' ');
    }

    public getTaskTypeLabel(type: TaskType): string {
        switch (type) {
            case TaskType.CHECKOUT_CLEAN:
                return 'Checkout Clean';
            case TaskType.STAYOVER_CLEAN:
                return 'Stayover Clean';
            case TaskType.DEEP_CLEAN:
                return 'Deep Clean';
            default:
                return 'Clean';
        }
    }

    public getTaskTypeIcon(type: TaskType): string {
        switch (type) {
            case TaskType.CHECKOUT_CLEAN:
                return 'exit_to_app';
            case TaskType.STAYOVER_CLEAN:
                return 'bed';
            case TaskType.DEEP_CLEAN:
                return 'cleaning_services';
            default:
                return 'cleaning_services';
        }
    }

    public getStatusColor(status: TaskStatus): string {
        return getTaskStatusColor(status);
    }

    public getRoomStatusBadgeClass(status: RoomStatus): string {
        return this.roomStatusUtil.getRoomBgClass(status) + ' text-white';
    }

    public getRoomStatusLabel(status: RoomStatus): string {
        return this.roomStatusUtil.getStatusLabel(status);
    }

    private calculateTaskTop(task: CleaningTaskDto): number {
        const startTime = new Date(task.scheduledStart);
        const hours = startTime.getHours();
        const minutes = startTime.getMinutes();
        return hours * this.HOUR_HEIGHT() + (minutes / 60) * this.HOUR_HEIGHT();
    }

    private calculateTaskHeight(task: CleaningTaskDto): number {
        const start = new Date(task.scheduledStart).getTime();
        const end = new Date(task.scheduledEnd).getTime();
        const durationMs = end - start;
        const durationHours = durationMs / (1000 * 60 * 60);
        return Math.max(durationHours * this.HOUR_HEIGHT(), 40); // Minimum 40px height
    }

    private scrollToCurrentTime(): void {
        if (!this.timelineContainer) {
            return;
        }

        const container = this.timelineContainer.nativeElement as HTMLElement;

        const scrollTo = this.currentTimeOffset() - 200;
        container.scrollTop = Math.max(0, scrollTo);
    }

    private captureScrollPosition(): void {
        if (!this.timelineContainer) {
            return;
        }
        const container = this.timelineContainer.nativeElement as HTMLElement;
        this.scrollPositionBeforeUpdate = container.scrollTop;
    }

    private restoreScrollPosition(): void {
        if (this.scrollPositionBeforeUpdate === null) {
            return;
        }

        const savedPosition = this.scrollPositionBeforeUpdate;

        setTimeout(() => {
            if (!this.timelineContainer) {
                return;
            }

            const container = this.timelineContainer.nativeElement as HTMLElement;
            container.scrollTop = savedPosition;
            this.scrollPositionBeforeUpdate = null;
        }, 100);
    }

    private updateSelectedTaskState(): void {
        const currentlySelected = this.selectedTask();
        if (!currentlySelected) {
            return;
        }

        const updatedTask = this.dailyTasks().find((t) => t.id === currentlySelected.id);
        if (updatedTask) {
            this.selectedTask.set(updatedTask);
        } else {
            this.selectedTask.set(null);
        }
    }

    // Overlap detection and group management
    private detectOverlaps(tasks: TaskWithPosition[], expandedIndices: Set<number>): TaskOverlapGroup[] {
        if (tasks.length === 0) {
            return [];
        }

        // Sort tasks by start time (top position)
        const sortedTasks = [...tasks].sort((a, b) => a.top - b.top);
        const groups: TaskOverlapGroup[] = [];
        const processed = new Set<number>();

        sortedTasks.forEach((task, index) => {
            if (processed.has(index)) {
                return;
            }

            // Find all tasks that overlap with this task
            const overlappingTasks: TaskWithPosition[] = [task];
            processed.add(index);

            for (let j = index + 1; j < sortedTasks.length; j++) {
                if (processed.has(j)) {
                    continue;
                }

                const otherTask = sortedTasks[j];

                // Check if any task in the current group overlaps with otherTask
                const overlapsWithGroup = overlappingTasks.some((groupTask) => this.tasksOverlap(groupTask, otherTask));

                if (overlapsWithGroup) {
                    overlappingTasks.push(otherTask);
                    processed.add(j);
                }
            }

            // Create group
            const startOffset = Math.min(...overlappingTasks.map((t) => t.top));
            const endOffset = Math.max(...overlappingTasks.map((t) => t.top + t.height));
            const groupIndex = groups.length;

            groups.push({
                tasks: overlappingTasks,
                startOffset,
                endOffset,
                isExpanded: expandedIndices.has(groupIndex),
            });
        });

        return groups;
    }

    private tasksOverlap(task1: TaskWithPosition, task2: TaskWithPosition): boolean {
        const task1End = task1.top + task1.height;
        const task2End = task2.top + task2.height;

        /*
         * Two tasks overlap if their time ranges intersect or are too close (within MIN_TASK_GAP)
         * This ensures visual separation between adjacent tasks
         */
        return task1.top < task2End + this.MIN_TASK_GAP && task2.top < task1End + this.MIN_TASK_GAP;
    }

    public toggleGroupExpansion(groupIndex: number): void {
        this.expandedGroupIndices.update((indices) => {
            const newSet = new Set(indices);
            if (newSet.has(groupIndex)) {
                newSet.delete(groupIndex);
            } else {
                newSet.add(groupIndex);
            }
            return newSet;
        });
    }

    // Touch event handlers for zoom
    public onTimelineContainerTouchStart(event: TouchEvent): void {
        this.cancelZoomAnimation();

        const touches = Array.from(event.touches).map((t) => ({
            x: t.clientX,
            y: t.clientY,
        }));

        let initialDistance = null;
        if (touches.length === 2) {
            initialDistance = this.calculateDistance(touches[0], touches[1]);
            event.preventDefault(); // Prevent browser zoom on pinch
        }

        const container = this.timelineContainer?.nativeElement as HTMLElement;
        this.zoomGestureState.set({
            startHourHeight: this.HOUR_HEIGHT(),
            startTouches: touches,
            initialDistance,
            startScrollTop: container?.scrollTop || 0,
        });
    }

    public onTimelineContainerTouchMove(event: TouchEvent): void {
        const gestureState = this.zoomGestureState();
        if (!gestureState) {
            return;
        }

        const touches = Array.from(event.touches).map((t) => ({
            x: t.clientX,
            y: t.clientY,
        }));

        // Only handle pinch-to-zoom (two fingers)
        if (touches.length === 2 && gestureState.initialDistance !== null) {
            event.preventDefault(); // Prevent browser zoom

            const currentDistance = this.calculateDistance(touches[0], touches[1]);
            const scaleFactor = currentDistance / gestureState.initialDistance;

            const newHeight = Math.max(
                this.MIN_HOUR_HEIGHT,
                Math.min(this.MAX_HOUR_HEIGHT, gestureState.startHourHeight * scaleFactor),
            );

            this.HOUR_HEIGHT.set(newHeight);
            this.maintainScrollPosition(gestureState.startHourHeight, newHeight, gestureState.startScrollTop);
        }
        // Single finger scrolling is handled natively by the browser
    }

    public onTimelineContainerTouchEnd(event: TouchEvent): void {
        if (event.touches.length === 0) {
            this.zoomGestureState.set(null);
        } else {
            // Update gesture state for remaining touches
            const touches = Array.from(event.touches).map((t) => ({
                x: t.clientX,
                y: t.clientY,
            }));
            const currentGesture = this.zoomGestureState();
            if (currentGesture) {
                const container = this.timelineContainer?.nativeElement as HTMLElement;
                this.zoomGestureState.update((state) => ({
                    ...state!,
                    startTouches: touches,
                    startHourHeight: this.HOUR_HEIGHT(),
                    initialDistance: touches.length === 2 ? this.calculateDistance(touches[0], touches[1]) : null,
                    startScrollTop: container?.scrollTop || 0,
                }));
            }
        }
    }

    // Zoom control methods
    public zoomIn(): void {
        const newHeight = Math.min(this.MAX_HOUR_HEIGHT, this.HOUR_HEIGHT() + 20);
        this.animateHourHeightChange(newHeight);
    }

    public zoomOut(): void {
        const newHeight = Math.max(this.MIN_HOUR_HEIGHT, this.HOUR_HEIGHT() - 20);
        this.animateHourHeightChange(newHeight);
    }

    public resetZoom(): void {
        this.animateHourHeightChange(this.DEFAULT_HOUR_HEIGHT);
    }

    // Helper methods
    private calculateDistance(p1: TouchPoint, p2: TouchPoint): number {
        return this.svgUtil.distance(p1, p2);
    }

    private maintainScrollPosition(oldHeight: number, newHeight: number, originalScrollTop: number): void {
        if (!this.timelineContainer) {
            return;
        }

        const container = this.timelineContainer.nativeElement as HTMLElement;

        // Calculate what hour was at the top of viewport
        const hourAtTop = originalScrollTop / oldHeight;

        // Maintain that hour at the top
        container.scrollTop = hourAtTop * newHeight;
    }

    private animateHourHeightChange(targetHeight: number): void {
        this.cancelZoomAnimation();

        const startHeight = this.HOUR_HEIGHT();
        if (startHeight === targetHeight) {
            return;
        }

        const startTime = performance.now();
        const container = this.timelineContainer?.nativeElement as HTMLElement;
        const startScrollTop = container?.scrollTop || 0;

        const animate = (currentTime: number): void => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / this.ZOOM_ANIMATION_DURATION, 1);
            const eased = 1 - Math.pow(1 - progress, 3); // Cubic ease-out

            const height = startHeight + (targetHeight - startHeight) * eased;
            this.HOUR_HEIGHT.set(height);

            // Maintain center of viewport
            this.maintainScrollPosition(startHeight, height, startScrollTop);

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

    public ngOnDestroy(): void {
        this.cancelZoomAnimation();
    }
}
