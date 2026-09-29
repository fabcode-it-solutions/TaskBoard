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
public class CleaningStatisticsResponseDto {
    // Average cleaning duration (minutes) per task type + room type
    private Map<String, Map<String, Double>> avgDurationByTaskAndRoomType;

    // Total tracked hours by month (MM format, full history or filtered)
    private Map<String, Double> avgHoursByMonth;

    // Percentage of blocked tasks (over full history or filtered period)
    private Double blockedTasksPercentage;
}
