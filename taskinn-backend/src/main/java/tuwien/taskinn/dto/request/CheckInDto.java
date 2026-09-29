package tuwien.taskinn.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInDto {
    @NotNull
    private Long bookingId;
    @NotNull
    private List<CheckInGuestDto> guests;
}
