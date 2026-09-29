package tuwien.taskinn.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tuwien.taskinn.entity.ShiftScheduleTemplate;

public interface ShiftScheduleTemplateRepository extends JpaRepository<ShiftScheduleTemplate, Long> {
}
