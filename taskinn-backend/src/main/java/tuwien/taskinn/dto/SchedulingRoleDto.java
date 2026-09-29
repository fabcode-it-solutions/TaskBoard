package tuwien.taskinn.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SchedulingRoleDto {

    @NotNull
    private Long id;

    private String name;

    private List<SchedulableEmployeeDto> employees;

}
