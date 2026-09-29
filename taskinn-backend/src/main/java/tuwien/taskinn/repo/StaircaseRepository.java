package tuwien.taskinn.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tuwien.taskinn.entity.Staircase;

public interface StaircaseRepository extends JpaRepository<Staircase, Long> {

    List<Staircase> findAllByFloorAndActiveTrue(Integer floor);

    List<Staircase> findAllByActiveTrue();
}
