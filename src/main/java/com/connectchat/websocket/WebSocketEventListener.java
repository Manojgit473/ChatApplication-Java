package com.connectchat.websocket;

import com.connectchat.enums.OnlineStatus;
import com.connectchat.security.CustomUserDetails;
import com.connectchat.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Event Listener for WebSocket lifecycle events (connect, disconnect).
 * 
 * Demonstrates:
 * - Spring ApplicationEvent handling for tracking online/offline presence dynamically.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final PresenceService presenceService;

    public WebSocketEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        Long userId = extractUserId(principal);
        if (userId != null) {
            log.info("[WS-EVENT] User {} connected to WebSocket session", userId);
            presenceService.setUserOnlineStatus(userId, OnlineStatus.ONLINE);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        Long userId = extractUserId(principal);
        if (userId != null) {
            log.info("[WS-EVENT] User {} disconnected from WebSocket session", userId);
            presenceService.setUserOnlineStatus(userId, OnlineStatus.OFFLINE);
        }
    }

    private Long extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                return userDetails.getId();
            }
        }
        return null;
    }
}
