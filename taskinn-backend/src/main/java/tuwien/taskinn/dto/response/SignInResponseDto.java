package tuwien.taskinn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignInResponseDto {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
}
