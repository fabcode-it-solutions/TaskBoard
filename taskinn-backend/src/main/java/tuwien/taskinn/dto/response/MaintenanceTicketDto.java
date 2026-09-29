package tuwien.taskinn.dto.response;

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
public class MaintenanceTicketDto {

    private Long id;

    private String title;

    private String description;

    private Priority priority;

    private TicketStatus status;

    // Necessary room details for context for the ticket
    private Long roomId;

    private String roomNumber;

    private Long reporterId;

    private String reporterName;
}
