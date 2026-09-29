package tuwien.taskinn.annotation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tuwien.taskinn.enums.Roles;

@Aspect
@Component
public class RoleGuardAspect {

    /**
     * An "Around" advice that wraps methods or classes annotated with {@link RequireRole} or {@link RequireRoles}. It
     * performs the authorization check before allowing the target method to execute. The pointcut expression targets
     * annotations at both the method level (`@annotation`) and the class level (`@within`).
     *
     * @param pjp The ProceedingJoinPoint, which represents the intercepted method call.
     * @return The result of the original method's execution if the security check passes.
     * @throws Throwable If the security check fails, it throws an {@link AccessDeniedException}. It also re-throws any
     *         exception thrown by the target method.
     */
    @Around("@annotation(tuwien.taskinn.annotation.RequireRole) ||"
            + " @within(tuwien.taskinn.annotation.RequireRole) ||"
            + " @annotation(tuwien.taskinn.annotation.RequireRoles) ||"
            + " @within(tuwien.taskinn.annotation.RequireRoles)")
    public Object checkRoles(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        RequireRole[] methodReqSingle = signature.getMethod().getAnnotationsByType(RequireRole.class);
        RequireRoles methodReq = signature.getMethod().getAnnotation(RequireRoles.class);

        Class<?> targetClass = pjp.getTarget().getClass();
        RequireRole[] classReqSingle = targetClass.getAnnotationsByType(RequireRole.class);
        RequireRoles classReq = targetClass.getAnnotation(RequireRoles.class);

        Roles[] roles;
        boolean requireAll = false;
        if ((methodReqSingle != null && methodReqSingle.length > 0) || methodReq != null) {
            roles = concat(methodReqSingle, methodReq);
            requireAll = methodReq != null && methodReq.requireAll();
        } else {
            roles = concat(classReqSingle, classReq);
            requireAll = classReq != null && classReq.requireAll();
        }

        if (roles == null || roles.length == 0) {
            return pjp.proceed();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated");
        }

        Set<String> have = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        Set<String> need =
                Arrays.stream(roles).map(r -> "ROLE_" + r.name()).collect(Collectors.toCollection(HashSet::new));

        boolean ok;
        if (requireAll) {
            ok = have.containsAll(need);
        } else {
            ok = need.stream().anyMatch(have::contains);
        }

        if (!ok) {
            throw new AccessDeniedException("Forbidden");
        }
        return pjp.proceed();
    }

    /**
     * A helper method to aggregate roles from both repeatable {@link RequireRole} annotations and a single
     * {@link RequireRoles} container annotation.
     *
     * @param singles The array of single @RequireRole annotations.
     * @param multi The container @RequireRoles annotation.
     * @return An array of unique {@link Roles} enums required for access.
     */
    private Roles[] concat(RequireRole[] singles, RequireRoles multi) {
        Set<Roles> out = new HashSet<>();
        if (singles != null) {
            for (RequireRole rr : singles) {
                out.add(rr.value());
            }
        }
        if (multi != null) {
            RequireRole[] multiValues = multi.value();
            for (RequireRole rr : multiValues) {
                out.add(rr.value());
            }
        }
        return out.toArray(new Roles[0]);
    }
}
