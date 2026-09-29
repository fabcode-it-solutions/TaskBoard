import { Injectable, computed, signal, WritableSignal, Signal, OnDestroy } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DemoTimeService implements OnDestroy {
    private readonly demoTimeSignal: WritableSignal<Date | null> = signal(null);

    private readonly realTimeTickSignal = signal(0);
    private realTimeIntervalId: number | null = null;

    constructor() {
        this.realTimeIntervalId = window.setInterval(() => {
            if (!this.demoTimeSignal()) {
                this.realTimeTickSignal.update((n) => n + 1);
            }
        }, 60000);
    }

    /**
     * Parse and set demo time from query parameter string
     * @param timeString Time in HH:mm format (e.g., "14:30")
     */
    public setDemoTimeFromQuery(timeString: string | null): void {
        if (!timeString) {
            this.demoTimeSignal.set(null);
            return;
        }

        const parsed = this.parseTimeString(timeString);
        if (parsed) {
            this.demoTimeSignal.set(parsed);
        } else {
            console.warn(
                `Invalid currentTime format: "${timeString}". Expected HH:mm (e.g., "14:30"). Using real time.`,
            );
            this.demoTimeSignal.set(null);
        }
    }

    /**
     * Get current time - either demo time or real time
     * This method is reactive via signals
     */
    public getCurrentTime(): Date {
        this.realTimeTickSignal();
        const demoTime = this.demoTimeSignal();

        return demoTime ? new Date(demoTime) : new Date();
    }

    /**
     * Computed signal for current hour (0-23)
     */
    public readonly currentHour: Signal<number> = computed(() => {
        this.realTimeTickSignal(); // Ensure reactivity
        const demoTime = this.demoTimeSignal();
        const time = demoTime ?? new Date();
        return time.getHours();
    });

    /**
     * Computed signal for current minute (0-59)
     */
    public readonly currentMinute: Signal<number> = computed(() => {
        this.realTimeTickSignal(); // Ensure reactivity
        const demoTime = this.demoTimeSignal();
        const time = demoTime ?? new Date();
        return time.getMinutes();
    });

    /**
     * Parse time string in HH:mm format
     * @param timeString Time string to parse
     * @returns Date object with today's date and parsed time, or null if invalid
     */
    private parseTimeString(timeString: string): Date | null {
        const regex = /^([01]?[0-9]|2[0-3]):([0-5][0-9])$/;
        const match = timeString.trim().match(regex);

        if (!match) {
            return null;
        }

        const hours = parseInt(match[1], 10);
        const minutes = parseInt(match[2], 10);

        // Create date with today's date and parsed time
        const today = new Date();
        today.setHours(hours, minutes, 0, 0);

        return today;
    }

    /**
     * Clear demo time and return to real time
     */
    public clearDemoTime(): void {
        this.demoTimeSignal.set(null);
    }

    public ngOnDestroy(): void {
        if (this.realTimeIntervalId !== null) {
            clearInterval(this.realTimeIntervalId);
            this.realTimeIntervalId = null;
        }
    }
}
