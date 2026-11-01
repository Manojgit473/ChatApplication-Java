package com.connectchat.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Spring AOP Aspect for business and security audit tracking.
 * 
 * Demonstrates:
 * - @AfterReturning advice for auditing successful state-changing operations.
 * - Non-sensitive audit trail generation (groups created, messages deleted, users blocked).
 */
@Aspect
@Component
public class AuditLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingAspect.class);

    @AfterReturning("execution(* com.connectchat.service.impl.AuthServiceImpl.register(..))")
    public void auditRegistration(JoinPoint joinPoint) {
        log.info("[AUDIT] New user registration successfully completed");
    }

    @AfterReturning(pointcut = "execution(* com.connectchat.service.impl.GroupServiceImpl.createGroup(..))", returning = "result")
    public void auditGroupCreation(JoinPoint joinPoint, Object result) {
        log.info("[AUDIT] New group created via GroupService");
    }

    @AfterReturning("execution(* com.connectchat.service.impl.GroupServiceImpl.deleteGroup(..))")
    public void auditGroupDeletion(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        log.info("[AUDIT] Group with id {} deleted by user {}", args.length > 0 ? args[0] : "?", args.length > 1 ? args[1] : "?");
    }

    @AfterReturning("execution(* com.connectchat.service.impl.MessageServiceImpl.deleteMessage(..))")
    public void auditMessageDeletion(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        log.info("[AUDIT] Message with id {} deleted by user {}", args.length > 0 ? args[0] : "?", args.length > 1 ? args[1] : "?");
    }

    @AfterReturning("execution(* com.connectchat.service.impl.UserServiceImpl.blockUser(..))")
    public void auditUserBlocked(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        log.info("[AUDIT] User block established: blocker={} blocked={}", args.length > 0 ? args[0] : "?", args.length > 1 ? args[1] : "?");
    }
}
