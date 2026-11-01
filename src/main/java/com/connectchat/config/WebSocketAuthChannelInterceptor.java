package com.connectchat.config;

import com.connectchat.repository.ConversationMemberRepository;
import com.connectchat.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocket STOMP Channel Interceptor for security authorization.
 * 
 * Demonstrates:
 * - STOMP destination authorization at connection and subscription time.
 * - Prevents users from subscribing to conversations they do not belong to.
 * - Enforces zero-trust: uses Principal from authenticated handshake, not client payload IDs.
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);
    private static final Pattern CONVERSATION_TOPIC_PATTERN = Pattern.compile("^/topic/conversations/(\\d+)(?:/.*)?$");

    private final ConversationMemberRepository conversationMemberRepository;

    public WebSocketAuthChannelInterceptor(ConversationMemberRepository conversationMemberRepository) {
        this.conversationMemberRepository = conversationMemberRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            Principal principal = accessor.getUser();

            if (principal == null) {
                log.warn("[WS-AUTH] Unauthorized subscription attempt to destination '{}' without principal", destination);
                throw new AccessDeniedException("Anonymous WebSocket subscriptions are disallowed");
            }

            Long userId = extractUserIdFromPrincipal(principal);
            if (userId != null && destination != null) {
                Matcher matcher = CONVERSATION_TOPIC_PATTERN.matcher(destination);
                if (matcher.matches()) {
                    Long conversationId = Long.parseLong(matcher.group(1));
                    boolean isMember = conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId);
                    if (!isMember) {
                        log.warn("[WS-AUTH] User {} attempted unauthorized subscription to conversation {}", userId, conversationId);
                        throw new AccessDeniedException("You are not a member of conversation " + conversationId);
                    }
                    log.debug("[WS-AUTH] User {} authorized for destination '{}'", userId, destination);
                }
            }
        }

        return message;
    }

    private Long extractUserIdFromPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                return userDetails.getId();
            }
        }
        return null;
    }
}
