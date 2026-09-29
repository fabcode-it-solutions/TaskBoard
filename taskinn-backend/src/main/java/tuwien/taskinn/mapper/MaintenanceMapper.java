package tuwien.taskinn.mapper;

import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.response.MaintenanceTicketDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.MaintenanceTicket;

/**
 * Mapper for converting between MaintenanceTicket entities and DTOs.
 */
@Component
public class MaintenanceMapper {

    /**
     * Convert a MaintenanceTicket entity to a MaintenanceTicketDto.
     *
     * @param maintenanceTicket the MaintenanceTicket entity to convert
     * @return the corresponding MaintenanceTicketDto
     */
    public MaintenanceTicketDto toDto(MaintenanceTicket maintenanceTicket) {
        if (maintenanceTicket == null) {
            return null;
        }

        Employee reporter = maintenanceTicket.getReporter();

        return MaintenanceTicketDto.builder()
                .id(maintenanceTicket.getId())
                .title(maintenanceTicket.getTitle())
                .description(maintenanceTicket.getDescription())
                .priority(maintenanceTicket.getPriority())
                .status(maintenanceTicket.getStatus())
                .reporterId(reporter.getId())
                .reporterName(reporter.getFirstName() + " " + reporter.getLastName())
                .roomId(maintenanceTicket.getRoom().getId())
                .roomNumber(maintenanceTicket.getRoom().getRoomNumber())
                .build();
    }
}
