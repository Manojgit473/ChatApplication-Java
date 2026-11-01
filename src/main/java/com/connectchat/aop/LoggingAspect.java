package com.connectchat.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Spring AOP Aspect for service performance measurement and execution logging.
 * 
 * Demonstrates:
 * - @Aspect and @Pointcut annotations.
 * - @Around advice for intercepting method execution.
 * - Separation of cross-cutting concerns (logging, timing) from core business logic.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(* com.connectchat.service.impl.*.*(..))")
    public void serviceLayerPointcut() {
    }

    @Around("serviceLayerPointcut()")
    public Object profileServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - start;

            if (elapsedTime > 50) {
                log.info("[PERF] {}.{}() executed in {} ms", className, methodName, elapsedTime);
            } else {
                log.debug("[EXEC] {}.{}() finished in {} ms", className, methodName, elapsedTime);
            }

            return result;
        } catch (Throwable ex) {
            long elapsedTime = System.currentTimeMillis() - start;
            log.warn("[EXCEPTION] {}.{}() threw {} after {} ms: {}",
                    className, methodName, ex.getClass().getSimpleName(), elapsedTime, ex.getMessage());
            throw ex;
        }
    }
}
