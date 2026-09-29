package tuwien.taskinn.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinibarBatchConsumptionRequestDto {

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<MinibarConsumptionRequestDto> items;
}
