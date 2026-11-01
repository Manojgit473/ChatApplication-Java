package com.connectchat.security;

import com.connectchat.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility for accessing currently authenticated user context.
 */
@Component
public class SecurityUtils {

    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return (CustomUserDetails) principal;
        }
        throw new UnauthorizedException("Invalid authentication principal");
    }

    public static Long getCurrentUserId() {
        return getCurrentUserDetails().getId();
    }

    public static String getCurrentUsername() {
        return getCurrentUserDetails().getUsername();
    }
}
