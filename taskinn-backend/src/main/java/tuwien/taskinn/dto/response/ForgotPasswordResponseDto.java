package tuwien.taskinn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ForgotPasswordResponseDto {

    private String resetToken;
}
