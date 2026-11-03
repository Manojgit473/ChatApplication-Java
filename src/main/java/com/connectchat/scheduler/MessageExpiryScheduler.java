package com.connectchat.scheduler;

import com.connectchat.dto.response.MessageDto;
import com.connectchat.entity.ConversationMember;
import com.connectchat.entity.Message;
import com.connectchat.mapper.MessageMapper;
import com.connectchat.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Background Scheduler for Self-Destructing / Expiring Messages.
 * 
 * Demonstrates:
 * - Spring Task Execution & Scheduling (@Scheduled) running at a high-precision 1-second fixed interval.
 * - Idempotent, transaction-safe lifecycle management of ephemeral data.
 * - Real-time STOMP WebSocket notification to remove messages from all connected clients simultaneously.
 */
@Component
public class MessageExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(MessageExpiryScheduler.class);

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageExpiryScheduler(MessageRepository messageRepository,
                                  MessageMapper messageMapper,
                                  @Lazy SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processExpiredMessages() {
        Instant now = Instant.now();
        List<Message> pendingExpired = messageRepository.findPendingExpiredMessages(now);

        if (pendingExpired.isEmpty()) {
            return;
        }

        log.debug("[EXPIRY] Processing {} expired messages at {}", pendingExpired.size(), now);

        for (Message msg : pendingExpired) {
            msg.setExpired(true);
            msg.setContent("");
            if (msg.getAttachments() != null) {
                msg.getAttachments().clear();
            }
            messageRepository.save(msg);

            MessageDto expiredDto = messageMapper.toDto(msg, null);
            expiredDto.setExpired(true);
            expiredDto.setContent("");

            Long convId = msg.getConversation().getId();

            // 1. Broadcast to active conversation channel
            messagingTemplate.convertAndSend("/topic/conversations/" + convId, expiredDto);
            messagingTemplate.convertAndSend("/topic/conversations/" + convId + "/expiry", expiredDto);

            // 2. Broadcast to all conversation members' personal topics
            if (msg.getConversation().getMembers() != null) {
                for (ConversationMember member : msg.getConversation().getMembers()) {
                    messagingTemplate.convertAndSend("/topic/users/" + member.getUser().getId() + "/messages", expiredDto);
                }
            }

            log.info("[EXPIRY] Message {} in conversation {} has self-destructed and expired", msg.getId(), convId);
        }
    }
}
