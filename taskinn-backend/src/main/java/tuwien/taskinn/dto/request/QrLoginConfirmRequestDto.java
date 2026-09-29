package tuwien.taskinn.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrLoginConfirmRequestDto {

    @NotBlank
    private String sessionId;

    private boolean confirmed;
}
