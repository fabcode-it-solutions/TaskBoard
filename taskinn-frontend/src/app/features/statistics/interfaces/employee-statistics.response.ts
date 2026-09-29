export interface EmployeeStatisticsResponse {
    employeeId: number;
    // TaskType -> RoomType -> Duration
    avgDurationByTaskAndRoomType: Record<string, Record<string, number>>;
}
