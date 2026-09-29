package tuwien.taskinn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tuwien.taskinn.enums.Priority;
import tuwien.taskinn.enums.TicketStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceTicketCreationUpdateDto {

    // INFO: ID is only used for updates; can be null for creation
    private Long id;

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 100)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotNull(message = "Priority cannot be null")
    private Priority priority;

    private TicketStatus status;

    @NotNull(message = "Room ID cannot be null")
    private Long roomId;

    // INFO: Reporter is derived from the authenticated user, so no need to include it here
}
