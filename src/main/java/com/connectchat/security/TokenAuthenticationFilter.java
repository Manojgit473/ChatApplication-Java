package com.connectchat.security;

import com.connectchat.entity.User;
import com.connectchat.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter that inspects HTTP headers (Authorization Bearer or X-Auth-Token)
 * and sets up request-scoped SecurityContext for that specific user.
 * 
 * Enables multi-account tab concurrency on localhost without cookie collision.
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final SessionTokenService sessionTokenService;
    private final UserRepository userRepository;

    public TokenAuthenticationFilter(SessionTokenService sessionTokenService, UserRepository userRepository) {
        this.sessionTokenService = sessionTokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String tokenHeader = request.getHeader("X-Auth-Token");

        String rawToken = (authHeader != null && authHeader.startsWith("Bearer ")) 
                ? authHeader.substring(7).trim() 
                : (tokenHeader != null ? tokenHeader.trim() : null);

        if (rawToken != null && !rawToken.isEmpty()) {
            Long userId = sessionTokenService.getUserIdFromToken(rawToken);
            if (userId != null) {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    CustomUserDetails userDetails = new CustomUserDetails(userOpt.get());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
