package com.connectchat.config;

import com.connectchat.entity.User;
import com.connectchat.repository.ConversationMemberRepository;
import com.connectchat.repository.UserRepository;
import com.connectchat.security.CustomUserDetails;
import com.connectchat.security.SessionTokenService;
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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocket STOMP Channel Interceptor for security authorization.
 * 
 * Demonstrates:
 * - STOMP destination authorization at connection and subscription time.
 * - Prevents users from subscribing to conversations or personal topics they do not belong to.
 * - Enforces zero-trust: uses Principal from authenticated handshake.
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);
    private static final Pattern CONVERSATION_TOPIC_PATTERN = Pattern.compile("^/topic/conversations/(\\d+)(?:/.*)?$");
    private static final Pattern USER_TOPIC_PATTERN = Pattern.compile("^/topic/users/(\\d+)(?:/.*)?$");

    private final ConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;
    private final SessionTokenService sessionTokenService;

    public WebSocketAuthChannelInterceptor(ConversationMemberRepository conversationMemberRepository,
                                           UserRepository userRepository,
                                           SessionTokenService sessionTokenService) {
        this.conversationMemberRepository = conversationMemberRepository;
        this.userRepository = userRepository;
        this.sessionTokenService = sessionTokenService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authToken = accessor.getFirstNativeHeader("X-Auth-Token");
            if (authToken == null || authToken.trim().isEmpty()) {
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authToken = authHeader.substring(7).trim();
                }
            }
            if (authToken != null && !authToken.trim().isEmpty()) {
                Long userId = sessionTokenService.getUserIdFromToken(authToken);
                if (userId != null) {
                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent()) {
                        CustomUserDetails userDetails = new CustomUserDetails(userOpt.get());
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        accessor.setUser(auth);
                        log.info("[WS-AUTH] Authenticated WebSocket connection for user ID {} ({}) via session token", userId, userDetails.getUsername());
                    }
                }
            }
        }

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            Principal principal = accessor.getUser();

            if (principal == null) {
                log.warn("[WS-AUTH] Unauthorized subscription attempt to destination '{}' without principal", destination);
                throw new AccessDeniedException("Anonymous WebSocket subscriptions are disallowed");
            }

            Long userId = extractUserIdFromPrincipal(principal);
            if (userId != null && destination != null) {
                // Check Conversation topic
                Matcher convMatcher = CONVERSATION_TOPIC_PATTERN.matcher(destination);
                if (convMatcher.matches()) {
                    Long conversationId = Long.parseLong(convMatcher.group(1));
                    boolean isMember = conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId);
                    if (!isMember) {
                        log.warn("[WS-AUTH] User {} attempted unauthorized subscription to conversation {}", userId, conversationId);
                        throw new AccessDeniedException("You are not a member of conversation " + conversationId);
                    }
                    log.debug("[WS-AUTH] User {} authorized for conversation '{}'", userId, destination);
                }

                // Check Personal User topic
                Matcher userMatcher = USER_TOPIC_PATTERN.matcher(destination);
                if (userMatcher.matches()) {
                    Long targetUserId = Long.parseLong(userMatcher.group(1));
                    if (!targetUserId.equals(userId)) {
                        log.warn("[WS-AUTH] User {} attempted unauthorized subscription to user topic {}", userId, targetUserId);
                        throw new AccessDeniedException("Cannot subscribe to another user's personal queue");
                    }
                    log.debug("[WS-AUTH] User {} authorized for personal topic '{}'", userId, destination);
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
        if (principal != null && principal.getName() != null) {
            String name = principal.getName().trim();
            String tenDigits = com.connectchat.util.PhoneNumberUtils.extract10Digit(name);
            if (tenDigits != null) {
                Optional<User> byPhone = userRepository.findByPhoneNumber("+91 " + tenDigits)
                        .or(() -> userRepository.findByPhoneNumber("+91" + tenDigits))
                        .or(() -> userRepository.findByPhoneNumber(tenDigits))
                        .or(() -> userRepository.findByFlexiblePhoneNumber(tenDigits));
                if (byPhone.isPresent()) {
                    return byPhone.get().getId();
                }
            }
            return userRepository.findByUsername(name)
                    .or(() -> userRepository.findByEmail(name))
                    .or(() -> userRepository.findByPhoneNumber(name))
                    .or(() -> userRepository.findByFlexiblePhoneNumber(name))
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }
}
