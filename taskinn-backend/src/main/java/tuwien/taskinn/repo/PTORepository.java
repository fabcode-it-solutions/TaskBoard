package tuwien.taskinn.repo;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tuwien.taskinn.entity.PTO;

public interface PTORepository extends JpaRepository<PTO, Long> {

    List<PTO> findByEmployeeEmail(String employeeEmail);

    @Query("SELECT p FROM PTO p WHERE (p.startDate < :end) AND (p.endDate >= :start) AND (p.employee.active = TRUE)")
    List<PTO> findAllPTOsInTimeframe(@Param("start") LocalDate startDate, @Param("end") LocalDate endDateExclusive);

    @Query("SELECT p FROM PTO p WHERE (p.startDate < :end) AND (p.endDate >= :start) AND (p.employee.id IN :ids)")
    List<PTO> findEmployeePTOsInTimeframe(@Param("ids") List<Long> employeeIds, @Param("start") LocalDate startDate,
            @Param("end") LocalDate endDateExclusive);

}
