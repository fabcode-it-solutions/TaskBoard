package tuwien.taskinn.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tuwien.taskinn.entity.SchedulingRole;

import java.util.Optional;

public interface SchedulingRoleRepository extends JpaRepository<SchedulingRole, Long> {

    /**
     * Find a scheduling role by its role name.
     */
    Optional<SchedulingRole> findByRoleName(String roleName);
}
