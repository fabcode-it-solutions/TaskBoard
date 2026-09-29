package tuwien.taskinn.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tuwien.taskinn.entity.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find a PasswordResetToken by its token string
     *
     * @param token the token string
     * @return an Optional containing the PasswordResetToken if found, otherwise empty
     */
    Optional<PasswordResetToken> findByToken(String token);
}
