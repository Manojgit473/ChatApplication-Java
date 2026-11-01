package com.connectchat.service;

import com.connectchat.dto.request.CreateConversationRequest;
import com.connectchat.dto.response.ConversationDetailDto;
import com.connectchat.entity.Conversation;
import com.connectchat.entity.ConversationMember;
import com.connectchat.entity.User;
import com.connectchat.enums.ConversationType;
import com.connectchat.exception.BadRequestException;
import com.connectchat.mapper.ConversationMapper;
import com.connectchat.mapper.MessageMapper;
import com.connectchat.repository.*;
import com.connectchat.service.impl.ConversationServiceImpl;
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
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMemberRepository conversationMemberRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BlockedUserRepository blockedUserRepository;
    @Mock
    private ConversationMapper conversationMapper;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    @Test
    void createDirectConversation_WithSelf_ShouldThrowBadRequest() {
        CreateConversationRequest req = new CreateConversationRequest(1L, "Hello");
        assertThrows(BadRequestException.class, () -> conversationService.createDirectConversation(1L, req));
    }

    @Test
    void createDirectConversation_WhenBlocked_ShouldThrowBadRequest() {
        CreateConversationRequest req = new CreateConversationRequest(2L, "Hello");
        when(blockedUserRepository.isBlockedEitherWay(1L, 2L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> conversationService.createDirectConversation(1L, req));
    }

    @Test
    void createDirectConversation_WhenNew_ShouldPersistConversationAndMembers() {
        CreateConversationRequest req = new CreateConversationRequest(2L, null);
        when(blockedUserRepository.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(conversationRepository.findDirectConversationBetweenUsers(1L, 2L)).thenReturn(Optional.empty());

        User user1 = new User("u1", "u1@test.com", "pass", "User One");
        user1.setId(1L);
        User user2 = new User("u2", "u2@test.com", "pass", "User Two");
        user2.setId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        Conversation savedConv = new Conversation(ConversationType.DIRECT, null, user1);
        savedConv.setId(100L);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConv);

        ConversationDetailDto detailDto = new ConversationDetailDto();
        detailDto.setId(100L);
        when(conversationMapper.toDetailDto(any(), any(), any())).thenReturn(detailDto);

        ConversationDetailDto result = conversationService.createDirectConversation(1L, req);

        assertNotNull(result);
        verify(conversationRepository, times(1)).save(any(Conversation.class));
        verify(conversationMemberRepository, times(2)).save(any(ConversationMember.class));
    }
}

