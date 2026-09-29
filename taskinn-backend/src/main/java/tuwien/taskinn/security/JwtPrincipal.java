package tuwien.taskinn.security;

import java.time.Instant;
import java.util.Set;
import tuwien.taskinn.enums.Roles;

/**
 * JWT Principal record
 *
 * @param subject the subject (usually user ID)
 * @param jti the JWT ID
 * @param issuedAt the issued at timestamp
 * @param expiresAt the expiration timestamp
 * @param roles the set of roles assigned to the principal
 */
public record JwtPrincipal(String subject, String jti, Instant issuedAt, Instant expiresAt, Set<Roles> roles) {
}
