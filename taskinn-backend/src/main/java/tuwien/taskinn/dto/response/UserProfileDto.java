package tuwien.taskinn.dto.response;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class UserProfileDto {

    private String email;

    private String firstName;

    private String lastName;

    private Long id;

    private Set<String> roles;
}
