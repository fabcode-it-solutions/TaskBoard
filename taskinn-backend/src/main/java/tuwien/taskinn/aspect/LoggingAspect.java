package tuwien.taskinn.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
@ConditionalOnProperty(name = "logging.aspect.enabled", havingValue = "true")
public class LoggingAspect {

    @Value("${logging.aspect.arg-string-length}")
    private int argStringLength;

    /**
     * Defines a pointcut that targets all public methods within any class in the `tuwien.taskinn.services` package (and
     * its subpackages) or any class annotated with `@Repository`. This effectively covers the entire business and data
     * access layers.
     */
    @Pointcut("within(tuwien.taskinn.services..*) || within(@org.springframework.stereotype.Repository *)")
    public void beanPointcut() {}

    /**
     * Defines a pointcut that targets all controller methods (classes annotated with @RestController or @Controller).
     * This is used to log incoming HTTP requests in non-production environments.
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) || within(@org.springframework.stereotype.Controller *)")
    public void controllerPointcut() {}

    /**
     * An "Around" advice that wraps the methods matched by the `beanPointcut`. It logs the method call details before
     * execution and logs the result and duration after execution.
     *
     * @param joinPoint The ProceedingJoinPoint, which represents the intercepted method call.
     * @return The result of the original method's execution.
     * @throws Throwable Propagates any exception thrown by the target method.
     */
    @Around("beanPointcut()")
    public Object logMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        log.debug("Before method: " + joinPoint.getSignature().getDeclaringType().getSimpleName() + "."
                + joinPoint.getSignature().getName() + " called with argument(s): " + getJoinPointArgs(joinPoint));

        Object result = joinPoint.proceed();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        log.debug("After method in " + duration + "ms: " + joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName() + " with result: "
                + limitString(result != null ? result.toString() : "null"));
        return result;
    }

    private String getJoinPointArgs(ProceedingJoinPoint joinPoint) {
        var params = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        var args = joinPoint.getArgs();

        var sb = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(params != null ? params[i] : "");
            sb.append("=");
            sb.append(limitString(args[i] != null ? args[i].toString() : "null"));
        }

        return sb.toString();
    }

    /**
     * An "Around" advice that wraps controller methods to log HTTP request details. Only active in non-production
     * environments (dev, test, etc.).
     *
     * @param joinPoint The ProceedingJoinPoint, which represents the intercepted controller method.
     * @return The result of the original controller method's execution.
     * @throws Throwable Propagates any exception thrown by the target method.
     */
    @Around("controllerPointcut() && !@annotation(org.springframework.context.annotation.Profile)")
    @Profile("!prod")
    public Object logEndpoint(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.info("HTTP Request: {} {}", request.getMethod(), request.getRequestURI());
        }

        return joinPoint.proceed();
    }

    private String limitString(String input) {
        if (input.length() <= argStringLength) {
            return input;
        } else {
            return input.substring(0, argStringLength) + "...";
        }
    }
}
