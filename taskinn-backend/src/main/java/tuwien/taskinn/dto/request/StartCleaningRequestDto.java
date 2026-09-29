package tuwien.taskinn.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartCleaningRequestDto {
    @NotNull(message = "Room ID is required")
    private Long roomId;
}
