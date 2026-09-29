package tuwien.taskinn.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.repo.RevokedTokenRepository;
import tuwien.taskinn.services.JwtService;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtAuthenticationFilter(JwtService jwtService, RevokedTokenRepository revokedTokenRepository) {
        this.jwtService = jwtService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    /**
     * Filter to authenticate requests based on JWT tokens, we check if the token is present, valid and not revoked If
     * yes, we set the authentication in the security context
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException in case of servlet errors
     * @throws IOException in case of I/O errors
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtService.parseAndValidate(token).ifPresent(principal -> {
                if (!revokedTokenRepository.existsByJti(principal.jti())) {
                    Authentication authentication = buildAuthentication(principal);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            });
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Create authentication object from JWT principal
     *
     * @param principal the JWT principal
     * @return the authentication object
     */
    private Authentication buildAuthentication(JwtPrincipal principal) {
        Collection<GrantedAuthority> authorities = toAuthorities(principal.roles());
        return new UsernamePasswordAuthenticationToken(principal.subject(), null, authorities);
    }

    /**
     * Convert roles to granted authorities
     *
     * @param roles the roles
     * @return the granted authorities
     */
    private Collection<GrantedAuthority> toAuthorities(Set<Roles> roles) {
        return roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).collect(Collectors.toSet());
    }
}
