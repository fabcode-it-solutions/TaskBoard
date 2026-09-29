export interface CleaningStatisticsResponse {
    avgDurationByTaskAndRoomType: Record<string, Record<string, number>>; // "CHECKOUT_CLEAN": {"Single": 45.2, "Double": 62.1}
    avgHoursByMonth: Record<string, number>; // "01": 125.5, "02": 98.2
    blockedTasksPercentage: number; // 12.34 (%)
}
