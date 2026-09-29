package tuwien.taskinn.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PtoDto {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
}
