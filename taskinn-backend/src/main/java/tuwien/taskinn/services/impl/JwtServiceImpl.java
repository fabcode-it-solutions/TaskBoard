package tuwien.taskinn.services.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.security.JwtPrincipal;
import tuwien.taskinn.services.JwtService;

@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey signingKey;

    @Getter
    private final long accessTtlSeconds;

    public JwtServiceImpl(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-ttl-seconds:900}") long accessTtlSeconds) {
        this.signingKey = deriveKey(secret);
        this.accessTtlSeconds = accessTtlSeconds;
    }

    private SecretKey deriveKey(String secret) {
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(decoded);
        } catch (Exception ignore) {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String generateAccessToken(String subjectEmail, Set<Roles> roles) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtlSeconds);
        String jti = UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles.stream().map(Enum::name).collect(Collectors.toList()));
        claims.put("jti", jti);

        return Jwts.builder()
                .setSubject(subjectEmail)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(claims)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Optional<JwtPrincipal> parseAndValidate(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
            Claims claims = jws.getBody();
            String subject = claims.getSubject();
            String jti = claims.get("jti", String.class);
            Date issuedAt = claims.getIssuedAt();
            Date expiration = claims.getExpiration();
            List<String> roleNames = claims.get("roles", List.class);
            Set<Roles> roles =
                    roleNames == null ? Set.of() : roleNames.stream().map(Roles::valueOf).collect(Collectors.toSet());
            return Optional.of(new JwtPrincipal(subject, jti, issuedAt == null ? null : issuedAt.toInstant(),
                    expiration == null ? null : expiration.toInstant(), roles));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
