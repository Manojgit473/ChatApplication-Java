package com.connectchat.scheduler;

import com.connectchat.dto.response.MessageDto;
import com.connectchat.entity.Conversation;
import com.connectchat.entity.ConversationMember;
import com.connectchat.entity.Message;
import com.connectchat.entity.User;
import com.connectchat.enums.ConversationType;
import com.connectchat.enums.MessageType;
import com.connectchat.mapper.MessageMapper;
import com.connectchat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageExpirySchedulerTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageExpiryScheduler scheduler;

    private Conversation conversation;
    private User user;
    private Message expiredMessage;

    @BeforeEach
    void setUp() {
        conversation = new Conversation();
        conversation.setId(100L);
        conversation.setType(ConversationType.DIRECT);

        user = new User("alice", "alice@test.com", "pass", "Alice Smith");
        user.setId(1L);

        ConversationMember member = new ConversationMember(conversation, user, com.connectchat.enums.GroupRole.MEMBER);
        conversation.setMembers(List.of(member));

        expiredMessage = new Message(conversation, user, "Confidential Secret Code 1234", MessageType.TEXT);
        expiredMessage.setId(555L);
        expiredMessage.setExpiresAt(Instant.now().minusSeconds(1));
        expiredMessage.setExpired(false);
        expiredMessage.setExpiryDuration("10s");
    }

    @Test
    @DisplayName("processExpiredMessages should mark message as expired, clear content, save to DB, and broadcast via STOMP")
    void testProcessExpiredMessages_WhenPendingExpiredExists() {
        when(messageRepository.findPendingExpiredMessages(any(Instant.class)))
                .thenReturn(List.of(expiredMessage));

        MessageDto dto = new MessageDto();
        dto.setId(555L);
        dto.setConversationId(100L);
        dto.setExpired(true);
        dto.setContent("");

        when(messageMapper.toDto(eq(expiredMessage), any())).thenReturn(dto);

        // Execute scheduled process
        scheduler.processExpiredMessages();

        // Verify message state updated
        assertTrue(expiredMessage.isExpired());
        assertEquals("", expiredMessage.getContent());

        // Verify saved to repository
        verify(messageRepository, times(1)).save(expiredMessage);

        // Verify broadcast to conversation topic and user topic
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/100"), eq(dto));
        verify(messagingTemplate).convertAndSend(eq("/topic/users/1/messages"), eq(dto));
    }

    @Test
    @DisplayName("processExpiredMessages should do nothing when no messages have expired")
    void testProcessExpiredMessages_WhenNoPendingMessages() {
        when(messageRepository.findPendingExpiredMessages(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        scheduler.processExpiredMessages();

        verify(messageRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
