package tuwien.taskinn.repo;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tuwien.taskinn.entity.Shift;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    @Query("""
                SELECT s FROM Shift s WHERE
                    (s.date >= :start) AND
                    (s.date < :end) AND
                    (s.employee.active = TRUE)
            """)
    List<Shift> findAllShiftsInTimeframe(@Param("start") LocalDateTime startDate,
            @Param("end") LocalDateTime endDateExclusive);

    @Modifying
    @Query("""
                DELETE FROM Shift s WHERE
                    (s.date >= :start) AND
                    (s.date < :end) AND
                    (s.employee.id IN :ids)
            """)
    void deleteAllShiftsOfEmployeesInTimeframe(@Param("ids") List<Long> employeeIds,
            @Param("start") LocalDateTime startDate, @Param("end") LocalDateTime endDateExclusive);

    @Query("""
                SELECT s FROM Shift s WHERE
                    s.employee.email = :email AND
                    s.date >= :start AND
                    s.date < :end
            """)
    List<Shift> findAllShiftsByEmployeeEmailInTimeframe(@Param("email") String email,
            @Param("start") LocalDateTime startDate, @Param("end") LocalDateTime endDateExclusive);

}
