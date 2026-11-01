package com.connectchat.service.impl;

import com.connectchat.dto.request.CreateConversationRequest;
import com.connectchat.dto.request.SendMessageRequest;
import com.connectchat.dto.response.ConversationDetailDto;
import com.connectchat.dto.response.ConversationSummaryDto;
import com.connectchat.dto.response.MessageDto;
import com.connectchat.dto.response.ReadReceiptDto;
import com.connectchat.entity.Conversation;
import com.connectchat.entity.ConversationMember;
import com.connectchat.entity.Message;
import com.connectchat.entity.User;
import com.connectchat.enums.ConversationType;
import com.connectchat.enums.GroupRole;
import com.connectchat.enums.MessageType;
import com.connectchat.exception.BadRequestException;
import com.connectchat.exception.ForbiddenException;
import com.connectchat.exception.ResourceNotFoundException;
import com.connectchat.mapper.ConversationMapper;
import com.connectchat.mapper.MessageMapper;
import com.connectchat.repository.BlockedUserRepository;
import com.connectchat.repository.ConversationMemberRepository;
import com.connectchat.repository.ConversationRepository;
import com.connectchat.repository.MessageRepository;
import com.connectchat.repository.UserRepository;
import com.connectchat.service.ConversationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ConversationService.
 * 
 * Demonstrates:
 * - Complex entity relationship management across Conversation and ConversationMember.
 * - Prevention of duplicate direct conversations using database lookups.
 * - Real-time STOMP read receipt broadcasts.
 */
@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   ConversationMemberRepository conversationMemberRepository,
                                   MessageRepository messageRepository,
                                   UserRepository userRepository,
                                   BlockedUserRepository blockedUserRepository,
                                   ConversationMapper conversationMapper,
                                   MessageMapper messageMapper,
                                   @Lazy SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getUserConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findAllByUserIdOrderByLastMessageAtDesc(userId);
        List<ConversationSummaryDto> summaries = new ArrayList<>();

        for (Conversation conv : conversations) {
            ConversationMember member = conversationMemberRepository
                    .findByConversationIdAndUserId(conv.getId(), userId)
                    .orElse(null);

            Optional<Message> latestMsgOpt = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conv.getId());
            MessageDto latestMsgDto = latestMsgOpt.map(m -> messageMapper.toDto(m, userId)).orElse(null);

            Long lastReadId = (member != null) ? member.getLastReadMessageId() : null;
            long unreadCount = messageRepository.countUnreadMessages(conv.getId(), lastReadId, userId);

            summaries.add(conversationMapper.toSummaryDto(conv, userId, latestMsgDto, unreadCount, member));
        }

        return summaries;
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailDto getConversationDetail(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        ConversationMember member = conversationMemberRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a participant in this conversation"));

        return conversationMapper.toDetailDto(conversation, userId, member);
    }

    @Override
    @Transactional
    public ConversationDetailDto createDirectConversation(Long currentUserId, CreateConversationRequest request) {
        Long recipientId = request.getRecipientId();
        if (currentUserId.equals(recipientId)) {
            throw new BadRequestException("You cannot start a direct conversation with yourself");
        }

        if (blockedUserRepository.isBlockedEitherWay(currentUserId, recipientId)) {
            throw new BadRequestException("Cannot create conversation: User is blocked");
        }

        // Check if a direct conversation already exists between these two users
        Optional<Conversation> existing = conversationRepository.findDirectConversationBetweenUsers(currentUserId, recipientId);
        if (existing.isPresent()) {
            Conversation conv = existing.get();
            ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conv.getId(), currentUserId).orElse(null);
            return conversationMapper.toDetailDto(conv, currentUserId, member);
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient", "id", recipientId));

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.DIRECT);
        conversation.setCreatedBy(currentUser);
        conversation.setLastMessageAt(Instant.now());

        Conversation savedConversation = conversationRepository.save(conversation);

        ConversationMember member1 = new ConversationMember(savedConversation, currentUser, GroupRole.MEMBER);
        ConversationMember member2 = new ConversationMember(savedConversation, recipient, GroupRole.MEMBER);

        conversationMemberRepository.save(member1);
        conversationMemberRepository.save(member2);

        savedConversation.getMembers().add(member1);
        savedConversation.getMembers().add(member2);

        // If an initial message is supplied, persist it
        if (request.getInitialMessage() != null && !request.getInitialMessage().trim().isEmpty()) {
            Message initial = new Message(savedConversation, currentUser, request.getInitialMessage().trim(), MessageType.TEXT);
            messageRepository.save(initial);
            savedConversation.setLastMessageAt(initial.getCreatedAt());
            conversationRepository.save(savedConversation);
        }

        return conversationMapper.toDetailDto(savedConversation, currentUserId, member1);
    }

    @Override
    @Transactional
    public void muteConversation(Long conversationId, Long userId, boolean muted) {
        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this conversation"));
        member.setMuted(muted);
        conversationMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void archiveConversation(Long conversationId, Long userId, boolean archived) {
        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this conversation"));
        member.setArchived(archived);
        conversationMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void markConversationAsRead(Long conversationId, Long userId) {
        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this conversation"));

        Optional<Message> latestMsg = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId);
        if (latestMsg.isPresent()) {
            Long latestId = latestMsg.get().getId();
            member.setLastReadMessageId(latestId);
            conversationMemberRepository.save(member);

            // Broadcast read receipt via STOMP
            ReadReceiptDto receipt = new ReadReceiptDto(conversationId, latestId, userId, Instant.now());
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/read", receipt);
        }
    }
}
