package tuwien.taskinn.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tuwien.taskinn.entity.MobileAuthToken;

public interface MobileAuthTokenRepository extends JpaRepository<MobileAuthToken, Long> {

    /**
     * Find a MobileAuthToken by its token string
     *
     * @param token the token string
     * @return an Optional containing the MobileAuthToken if found, otherwise empty
     */
    Optional<MobileAuthToken> findByToken(String token);

    /**
     * Find a MobileAuthToken by its session ID
     *
     * @param sessionId the session ID
     * @return an Optional containing the MobileAuthToken if found, otherwise empty
     */
    Optional<MobileAuthToken> findBySessionId(String sessionId);
}
