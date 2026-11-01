package com.connectchat.service;

import com.connectchat.dto.request.SendMessageRequest;
import com.connectchat.dto.response.MessageDto;
import com.connectchat.entity.Conversation;
import com.connectchat.entity.Message;
import com.connectchat.entity.User;
import com.connectchat.enums.ConversationType;
import com.connectchat.enums.MessageType;
import com.connectchat.exception.ForbiddenException;
import com.connectchat.mapper.MessageMapper;
import com.connectchat.repository.BlockedUserRepository;
import com.connectchat.repository.ConversationMemberRepository;
import com.connectchat.repository.ConversationRepository;
import com.connectchat.repository.MessageRepository;
import com.connectchat.repository.UserRepository;
import com.connectchat.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMemberRepository conversationMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BlockedUserRepository blockedUserRepository;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void sendMessage_WhenSenderNotMember_ShouldThrowForbiddenException() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(new Conversation()));
        when(conversationMemberRepository.existsByConversationIdAndUserId(10L, 5L)).thenReturn(false);

        SendMessageRequest request = new SendMessageRequest("Hello", MessageType.TEXT, null, null);

        assertThrows(ForbiddenException.class, () -> messageService.sendMessage(10L, 5L, request));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendMessage_WhenValid_ShouldSaveAndBroadcast() {
        Conversation conv = new Conversation();
        conv.setId(10L);
        conv.setType(ConversationType.GROUP);

        User sender = new User("sender", "s@test.com", "pass", "Sender User");
        sender.setId(1L);

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conv));
        when(conversationMemberRepository.existsByConversationIdAndUserId(10L, 1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));

        Message saved = new Message(conv, sender, "Test Message", MessageType.TEXT);
        saved.setId(50L);
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        MessageDto dto = new MessageDto();
        dto.setId(50L);
        dto.setContent("Test Message");
        when(messageMapper.toDto(any(), any())).thenReturn(dto);

        SendMessageRequest req = new SendMessageRequest("Test Message", MessageType.TEXT, null, "key123");
        MessageDto result = messageService.sendMessage(10L, 1L, req);

        assertNotNull(result);
        verify(messageRepository, times(1)).save(any(Message.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/conversations/10"), eq(dto));
    }
}
