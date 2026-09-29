package tuwien.taskinn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinibarStatisticsResponseDto {
    private Map<String, Double> salesByEarnings;

    private Map<String, Long> salesByQuantity;

    private Map<Long, Map<String, Long>> dailyItemSales;
}
