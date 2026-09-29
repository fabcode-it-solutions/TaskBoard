export interface BookingStatisticsResponse {
    allBookingsByDay: Record<string, number>; // "2026-01-11": 5
    monthBookingsByDay: Record<string, number>;
    bookingsByMonth: Record<string, number>; // "01": 42, "02": 0, ..., "12": 3
    bookingsByRoomType: Record<string, number>; // "Single": 42
}
