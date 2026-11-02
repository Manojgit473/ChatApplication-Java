package com.connectchat.websocket;

import com.connectchat.dto.request.SendMessageRequest;
import com.connectchat.dto.request.TypingNotificationRequest;
import com.connectchat.dto.response.TypingIndicatorDto;
import com.connectchat.dto.response.UserSummaryDto;
import com.connectchat.entity.User;
import com.connectchat.repository.UserRepository;
import com.connectchat.security.CustomUserDetails;
import com.connectchat.service.ConversationService;
import com.connectchat.service.MessageService;
import com.connectchat.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;

/**
 * Spring STOMP WebSocket Controller.
 * 
 * Demonstrates:
 * - @MessageMapping annotation for routing incoming STOMP frame payloads.
 * - Extracts authenticated identity from Principal rather than relying on payload client-side fields.
 * - Real-time typing status notification dispatching.
 */
@Controller
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageService messageService,
                                   ConversationService conversationService,
                                   UserService userService,
                                   UserRepository userRepository,
                                   SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send/{conversationId}")
    public void handleSendMessage(@DestinationVariable Long conversationId,
                                  @Payload SendMessageRequest request,
                                  Principal principal) {
        Long senderId = extractUserId(principal);
        if (senderId == null) {
            log.warn("[WS-SEND] Unauthenticated attempt to send message to conversation {}", conversationId);
            return;
        }

        messageService.sendMessage(conversationId, senderId, request);
    }

    @MessageMapping("/chat.typing/{conversationId}")
    public void handleTypingNotification(@DestinationVariable Long conversationId,
                                         @Payload TypingNotificationRequest request,
                                         Principal principal) {
        Long userId = extractUserId(principal);
        if (userId == null) {
            return;
        }

        CustomUserDetails userDetails = extractUserDetails(principal);
        UserSummaryDto userSummary;
        if (userDetails != null) {
            userSummary = new UserSummaryDto(
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getDisplayName(),
                    null,
                    null,
                    null
            );
        } else {
            User user = userRepository.findById(userId).orElse(null);
            userSummary = user != null ? new UserSummaryDto(user.getId(), user.getUsername(), user.getDisplayName(), null, null, null) : null;
        }

        TypingIndicatorDto typingDto = new TypingIndicatorDto(conversationId, userSummary, request.isTyping());
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/typing", typingDto);
    }

    @MessageMapping("/chat.read/{conversationId}")
    public void handleReadReceipt(@DestinationVariable Long conversationId, Principal principal) {
        Long userId = extractUserId(principal);
        if (userId != null) {
            conversationService.markConversationAsRead(conversationId, userId);
        }
    }

    private Long extractUserId(Principal principal) {
        CustomUserDetails userDetails = extractUserDetails(principal);
        if (userDetails != null) {
            return userDetails.getId();
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

    private CustomUserDetails extractUserDetails(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                return userDetails;
            }
        }
        return null;
    }
}
