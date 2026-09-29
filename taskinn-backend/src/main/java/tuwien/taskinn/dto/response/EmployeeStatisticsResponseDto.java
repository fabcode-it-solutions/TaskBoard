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
public class EmployeeStatisticsResponseDto {
    private Long employeeId;
    private Map<String, Map<String, Double>> avgDurationByTaskAndRoomType;
}
