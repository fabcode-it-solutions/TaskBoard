package tuwien.taskinn.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tuwien.taskinn.entity.RoomType;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    /**
     * Check if a room type exists by its name
     *
     * @param name the name
     * @return true if a room type with the given name exists, otherwise false
     */
    boolean existsByName(String name);
}
