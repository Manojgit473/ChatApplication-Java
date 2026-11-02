package com.connectchat.service;

import com.connectchat.dto.request.CreateGroupRequest;
import com.connectchat.dto.response.ConversationDetailDto;
import com.connectchat.dto.response.MessageDto;
import com.connectchat.entity.Conversation;
import com.connectchat.entity.ConversationMember;
import com.connectchat.entity.Message;
import com.connectchat.entity.User;
import com.connectchat.enums.ConversationType;
import com.connectchat.enums.GroupRole;
import com.connectchat.mapper.ConversationMapper;
import com.connectchat.mapper.MessageMapper;
import com.connectchat.repository.ConversationMemberRepository;
import com.connectchat.repository.ConversationRepository;
import com.connectchat.repository.MessageRepository;
import com.connectchat.repository.UserRepository;
import com.connectchat.service.impl.GroupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMemberRepository conversationMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationMapper conversationMapper;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private GroupServiceImpl groupService;

    private User creator;
    private User member2;
    private User member3;

    @BeforeEach
    void setUp() {
        creator = new User("creator", "creator@test.com", "Password@123", "Creator User");
        creator.setId(1L);

        member2 = new User("member2", "m2@test.com", "Password@123", "Member Two");
        member2.setId(2L);

        member3 = new User("member3", "m3@test.com", "Password@123", "Member Three");
        member3.setId(3L);
    }

    @Test
    void createGroup_WithDynamicMembers_ShouldSucceedAndBroadcast() {
        Set<Long> memberIds = new HashSet<>(List.of(2L, 3L));
        CreateGroupRequest req = new CreateGroupRequest("Dev Team", "Developers Group", memberIds);

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(member3));

        Conversation savedConv = new Conversation();
        savedConv.setId(50L);
        savedConv.setType(ConversationType.GROUP);
        savedConv.setTitle("Dev Team");
        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConv);

        Message savedMsg = new Message();
        savedMsg.setId(101L);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);
        when(messageMapper.toDto(any(Message.class), eq(1L))).thenReturn(new MessageDto());
        when(conversationMapper.toDetailDto(any(), eq(1L), any())).thenReturn(new ConversationDetailDto());

        ConversationDetailDto result = groupService.createGroup(1L, req);

        assertNotNull(result);
        verify(conversationRepository, atLeastOnce()).save(any(Conversation.class));
        verify(conversationMemberRepository, times(3)).save(any(ConversationMember.class));
        verify(messageRepository).save(any(Message.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/50"), any(MessageDto.class));
    }

    @Test
    void createGroup_WithZeroInitialMembers_ShouldSucceed() {
        CreateGroupRequest req = new CreateGroupRequest("Solo Group", "", new HashSet<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        Conversation savedConv = new Conversation();
        savedConv.setId(51L);
        savedConv.setType(ConversationType.GROUP);
        savedConv.setTitle("Solo Group");
        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConv);

        Message savedMsg = new Message();
        savedMsg.setId(102L);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);
        when(messageMapper.toDto(any(Message.class), eq(1L))).thenReturn(new MessageDto());
        when(conversationMapper.toDetailDto(any(), eq(1L), any())).thenReturn(new ConversationDetailDto());

        ConversationDetailDto result = groupService.createGroup(1L, req);

        assertNotNull(result);
        verify(conversationMemberRepository, times(1)).save(any(ConversationMember.class));
    }

    @Test
    void addGroupMember_ShouldAddMemberAndBroadcast() {
        Conversation group = new Conversation();
        group.setId(60L);
        group.setType(ConversationType.GROUP);
        group.setTitle("Team Alpha");

        ConversationMember requesterMem = new ConversationMember(group, creator, GroupRole.OWNER);

        when(conversationRepository.findById(60L)).thenReturn(Optional.of(group));
        when(conversationMemberRepository.findByConversationIdAndUserId(60L, 1L)).thenReturn(Optional.of(requesterMem));
        when(conversationMemberRepository.existsByConversationIdAndUserId(60L, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member2));

        Message savedMsg = new Message();
        savedMsg.setId(103L);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);
        when(messageMapper.toDto(any(Message.class), eq(1L))).thenReturn(new MessageDto());
        when(conversationMemberRepository.findByConversationId(60L)).thenReturn(List.of(requesterMem));

        groupService.addGroupMember(60L, 1L, 2L);

        verify(conversationMemberRepository).save(any(ConversationMember.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/60"), any(MessageDto.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/users/2/messages"), any(MessageDto.class));
    }
}
