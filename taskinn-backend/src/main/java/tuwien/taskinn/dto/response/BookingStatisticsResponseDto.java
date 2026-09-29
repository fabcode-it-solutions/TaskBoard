package tuwien.taskinn.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatisticsResponseDto {
    // Always full history: date -> count
    private Map<String, Long> allBookingsByDay;

    // Current month line (or specified month)
    private Map<String, Long> monthBookingsByDay;

    // Bar: bookings by month (filtered by period)
    private Map<String, Long> bookingsByMonth;

    // Bar: bookings by room type (filtered by period)
    private Map<String, Long> bookingsByRoomType;
}
