package tuwien.taskinn.services;

import tuwien.taskinn.dto.request.MaintenanceTicketCreationUpdateDto;
import tuwien.taskinn.dto.response.MaintenanceTicketDto;

import java.util.List;

public interface MaintenanceService {

    /**
     * Create a new maintenance ticket based on the provided data.
     *
     * @param requestDto DTO containing the data required to create the ticket
     * @param reporterEmail the identifier of the employee reporting the issue
     * @return the created MaintenanceTicket as a DTO, including a generated id
     */
    MaintenanceTicketDto createMaintenanceTicket(MaintenanceTicketCreationUpdateDto requestDto, String reporterEmail);

    /**
     * Retrieve all existing maintenance tickets.
     *
     * @return a list of MaintenanceTicket DTOs; may be empty if no tickets exist
     */
    List<MaintenanceTicketDto> getAllMaintenanceTickets();

    /**
     * Retrieve a maintenance ticket by its identifier.
     *
     * @param id the identifier of the ticket to retrieve
     * @return the MaintenanceTicket DTO corresponding to the given id
     */
    MaintenanceTicketDto getMaintenanceTicketById(Long id);

    /**
     * Update an existing maintenance ticket.
     *
     * @param id the identifier of the ticket to update
     * @param requestDto DTO containing the fields to update
     * @return the updated MaintenanceTicket as a DTO
     */
    MaintenanceTicketDto updateMaintenanceTicket(Long id, MaintenanceTicketCreationUpdateDto requestDto);

    /**
     * Delete a maintenance ticket by its identifier.
     *
     * @param id the identifier of the ticket to delete
     */
    void deleteMaintenanceTicket(Long id);

    /**
     * Retrieve all open maintenance tickets.
     *
     * @return a list of MaintenanceTicket DTOs with status OPEN; may be empty if no open tickets exist
     */
    List<MaintenanceTicketDto> getAllOpenMaintenanceTickets();

    /**
     * Count high priority tickets that need attention. Counts tickets with priority HIGH or URGENT and status OPEN or
     * IN_PROGRESS.
     *
     * @return count of high priority tickets needing attention
     */
    Long countHighPriorityTicketsNeedingAttention();
}
