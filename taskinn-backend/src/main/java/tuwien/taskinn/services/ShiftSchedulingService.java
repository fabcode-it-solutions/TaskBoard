package tuwien.taskinn.services;

import java.util.List;
import tuwien.taskinn.dto.EmployeeMonthShiftDto;
import tuwien.taskinn.dto.SchedulableEmployeeDto;
import tuwien.taskinn.dto.SchedulingRoleDto;
import tuwien.taskinn.dto.ShiftScheduleTemplateDto;
import tuwien.taskinn.dto.request.ShiftScheduleSolveRequestDto;

public interface ShiftSchedulingService {

    /**
     * Returns a list of all employees that can participate in shift scheduling.
     *
     * @return A list of all employees that can participate in shift scheduling.
     */
    List<SchedulableEmployeeDto> getSchedulableEmployees();

    /**
     * Returns a list of all shift assignments within a given month.
     *
     * @param year Year to get shift assignments from
     * @param month Month to get shift assignments from (starts at 1)
     * @return A list of all shift assignments within a given month
     */
    List<EmployeeMonthShiftDto> getShiftsForMonth(Integer year, Integer month);

    /**
     * Tries to update the shift assignments to the given ones within a given month.
     *
     * @param year Year to update shift assignments in
     * @param month Month to update shift assignments in
     * @param shifts New shifts to update to
     */
    void putShiftSchedule(int year, int month, List<EmployeeMonthShiftDto> shifts);

    /**
     * Tries to find a solution to the given shift scheduling problem.
     *
     * @param request Description of the shift scheduling problem
     * @return A solution to request
     */
    List<EmployeeMonthShiftDto> solveShiftScheduling(ShiftScheduleSolveRequestDto request);

    /**
     * Returns a list of all known scheduling roles.
     *
     * @return A list of all known scheduling roles.
     */
    List<SchedulingRoleDto> getAllSchedulingRoles();

    /**
     * Deletes a given scheduling role by its id.
     *
     * @param id ID of the scheduling role to delete
     */
    void deleteSchedulingRole(Long id);

    /**
     * Creates a new scheduling role according to the given request.
     *
     * @param request Parameters for the new scheduling role
     */
    void createSchedulingRole(SchedulingRoleDto request);

    /**
     * Updates a scheduling role with the given ID to reflect the given parameters.
     *
     * @param id ID of the scheduling role to update
     * @param request Parameters to update the scheduling role by
     */
    void updateSchedulingRole(Long id, SchedulingRoleDto request);

    /**
     * Returns a list of all known shift scheduling templates.
     *
     * @return A list of all known shift scheduling templates.
     */
    List<ShiftScheduleTemplateDto> getAllShiftScheduleTemplates();

    /**
     * Deletes a shift scheduling template with the given ID.
     *
     * @param id ID of the shift scheduling template to delete
     */
    void deleteShiftScheduleTemplate(Long id);

    /**
     * Creates a new shift scheduling template according to the given request.
     *
     * @param request Parameters for the new shift scheduling role
     */
    void createShiftScheduleTemplate(ShiftScheduleTemplateDto request);

    /**
     * Update the shift scheduling template with the given ID to reflect the given request.
     *
     * @param id ID of the shift scheduling template to update
     * @param request Parameters to update the shift scheduling role by
     */
    void updateShiftScheduleTemplate(Long id, ShiftScheduleTemplateDto request);

    /**
     * Generates a PDF document containing the shift schedule for the specified month.
     *
     * @param year The year of the schedule
     * @param month The month of the schedule
     * @return A byte array containing the generated PDF
     */
    byte[] getShiftSchedulePdf(Integer year, Integer month);
}
