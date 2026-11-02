package com.connectchat.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage isolated authentication session tokens.
 * Enables concurrent multi-account testing across tabs and windows
 * without browser session-cookie collision on localhost.
 */
@Service
public class SessionTokenService {

    private final Map<String, Long> tokenToUserIdMap = new ConcurrentHashMap<>();

    public String generateToken(Long userId) {
        if (userId == null) {
            return null;
        }
        String token = "cctok_" + UUID.randomUUID().toString().replace("-", "");
        tokenToUserIdMap.put(token, userId);
        return token;
    }

    public Long getUserIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        String cleanToken = token.trim();
        if (cleanToken.startsWith("Bearer ")) {
            cleanToken = cleanToken.substring(7).trim();
        }
        return tokenToUserIdMap.get(cleanToken);
    }

    public void revokeToken(String token) {
        if (token != null) {
            String cleanToken = token.trim();
            if (cleanToken.startsWith("Bearer ")) {
                cleanToken = cleanToken.substring(7).trim();
            }
            tokenToUserIdMap.remove(cleanToken);
        }
    }
}
