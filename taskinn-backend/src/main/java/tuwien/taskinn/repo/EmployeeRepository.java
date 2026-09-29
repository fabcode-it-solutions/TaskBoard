package tuwien.taskinn.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tuwien.taskinn.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    /**
     * Find an employee by their email
     *
     * @param email the email address
     * @return an Optional containing the Employee if found, otherwise empty
     */
    Optional<Employee> findByEmail(String email);

    /**
     * Check if an employee exists by their email
     *
     * @param email the email address
     * @return true if an employee with the given email exists, otherwise false
     */
    boolean existsByEmail(String email);

    @Query("""
                SELECT e FROM Employee e WHERE
                    (e.hourlyWage IS NOT NULL) AND
                    (e.hourlyOvertimeWage IS NOT NULL) AND
                    (e.workingHoursPerWeek IS NOT NULL) AND
                    (e.active = TRUE)
            """)
    List<Employee> findAllSchedulableEmployees();

    @Query("SELECT e FROM Employee e WHERE e.schedulingRole.id = :id AND e.active = TRUE "
            + "ORDER BY e.lastName ASC, e.firstName ASC")
    List<Employee> findAllEmployeesWithSchedulingRole(@Param("id") Long roleId);

}
