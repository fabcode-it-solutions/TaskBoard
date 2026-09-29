package tuwien.taskinn.services;

import java.util.*;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.security.JwtPrincipal;

public interface JwtService {

    /**
     * Creates a new JWT access token for a given user. The token will be digitally signed and will contain essential
     * user claims such as their email (subject), roles, and a unique token identifier (JTI).
     *
     * @param subjectEmail The user's unique identifier, typically their email address.
     * @param roles A set of the user's roles, which will be embedded in the token for authorization purposes.
     * @return A compact, URL-safe JWT string in the format header.payload.signature.
     */
    String generateAccessToken(String subjectEmail, Set<Roles> roles);

    /**
     * Parses a JWT string, validates its signature and expiration, and extracts the user principal. This method is the
     * primary mechanism for verifying a token's authenticity and integrity.
     *
     * @param token The compact JWT string received from a client (without the 'Bearer ' prefix).
     * @return An {@link Optional} containing the {@link JwtPrincipal} if the token is valid in every respect
     *         (signature, not expired, well-formed). Returns an empty {@link Optional} otherwise.
     */
    Optional<JwtPrincipal> parseAndValidate(String token);

    /**
     * Retrieves the configured time-to-live (TTL) for access tokens. This value determines how long a generated token
     * will be considered valid.
     *
     * @return The token lifetime in seconds.
     */
    long getAccessTtlSeconds();
}
