package tuwien.taskinn.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tuwien.taskinn.entity.RevokedToken;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    /**
     * Find a RevokedToken by its jti
     *
     * @param jti the JWT ID
     * @return an Optional containing the RevokedToken if found, otherwise empty
     */
    Optional<RevokedToken> findByJti(String jti);

    /**
     * Check if a RevokedToken exists by its jti
     *
     * @param jti the JWT ID
     * @return true if a RevokedToken with the given jti exists, otherwise false
     */
    boolean existsByJti(String jti);
}
