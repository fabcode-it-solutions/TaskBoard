package tuwien.taskinn.services;

import java.util.List;

import tuwien.taskinn.dto.EmployeeDto;
import tuwien.taskinn.dto.PtoDto;
import tuwien.taskinn.dto.request.CreatePtoRequest;

public interface EmployeeService {

    /**
     * Retrieves all employees who do not have the ADMIN role.
     *
     * @return A list of {@link EmployeeDto} containing non-admin employees' information.
     */
    List<EmployeeDto> getAllStaffMembers();

    /**
     * Retrieves a single employee by their ID.
     *
     * @param id The ID of the employee to retrieve.
     * @return An {@link EmployeeDto} containing the employee's information.
     * @throws IllegalArgumentException if no employee exists with the given ID.
     */
    EmployeeDto getEmployeeById(Long id);

    /**
     * Updates an existing employee's information.
     *
     * @param id The ID of the employee to update.
     * @param updateEmployeeRequestDto The DTO containing the updated employee information.
     * @return An {@link EmployeeDto} containing the updated employee's information.
     * @throws IllegalArgumentException if no employee exists with the given ID or if the email is already in use by
     *         another employee.
     */
    EmployeeDto updateEmployee(Long id, EmployeeDto updateEmployeeRequestDto);

    /**
     * Retrieves the schedule preferences of an employee.
     *
     * @param email The email of the employee to retrieve the schedule preferences for.
     * @return A list of {@link PtoDto} containing the schedule preferences of the employee.
     */
    List<PtoDto> getPtosByEmployeeEmail(String email);

    /**
     * Creates a PTO entry for the employee identified by email.
     *
     * @param email the employee email
     * @param request the create PTO request payload
     * @return created PTO as dto
     */
    PtoDto createPtoForEmployee(String email, CreatePtoRequest request);

    /**
     * Deletes a PTO entry for the employee identified by email.
     *
     * @param email the employee email
     * @param ptoId the id of the PTO to delete
     */
    void deletePtoForEmployee(String email, Long ptoId);

    /**
     * Retrieves the work shifts for a given employee in a specified month.
     *
     * @param email the employee email
     * @param year the year
     * @param month the month (1-12)
     * @return a list of day indices (0-based) representing scheduled work days
     */
    List<Integer> getShiftsByEmployeeEmail(String email, int year, int month);
}
