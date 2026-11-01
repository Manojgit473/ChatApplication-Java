package com.connectchat.service.impl;

import com.connectchat.dto.request.EditMessageRequest;
import com.connectchat.dto.request.SendMessageRequest;
import com.connectchat.dto.response.MessageDto;
import com.connectchat.entity.Conversation;
import com.connectchat.entity.ConversationMember;
import com.connectchat.entity.Message;
import com.connectchat.entity.User;
import com.connectchat.enums.ConversationType;
import com.connectchat.enums.GroupRole;
import com.connectchat.enums.MessageStatus;
import com.connectchat.enums.MessageType;
import com.connectchat.exception.BadRequestException;
import com.connectchat.exception.ForbiddenException;
import com.connectchat.exception.ResourceNotFoundException;
import com.connectchat.mapper.MessageMapper;
import com.connectchat.repository.BlockedUserRepository;
import com.connectchat.repository.ConversationMemberRepository;
import com.connectchat.repository.ConversationRepository;
import com.connectchat.repository.MessageRepository;
import com.connectchat.repository.UserRepository;
import com.connectchat.service.MessageService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of MessageService.
 * 
 * Demonstrates:
 * - Real-time STOMP message broadcasting over WebSockets.
 * - Idempotency deduplication for network retry resilience.
 * - Server-side authorization checks on edit and delete operations.
 * - Core Java String and Date/Time handling for chat export generation.
 */
@Service
public class MessageServiceImpl implements MessageService {

    private static final Duration EDIT_TIME_LIMIT = Duration.ofMinutes(15);
    private static final DateTimeFormatter EXPORT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("UTC"));

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final MessageMapper messageMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageServiceImpl(MessageRepository messageRepository,
                              ConversationRepository conversationRepository,
                              ConversationMemberRepository conversationMemberRepository,
                              UserRepository userRepository,
                              BlockedUserRepository blockedUserRepository,
                              MessageMapper messageMapper,
                              @Lazy SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.userRepository = userRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.messageMapper = messageMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageDto> getMessages(Long conversationId, Long userId, int page, int size) {
        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ForbiddenException("You are not authorized to view messages in this conversation");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        return messagePage.map(m -> messageMapper.toDto(m, userId));
    }

    @Override
    @Transactional
    public MessageDto sendMessage(Long conversationId, Long senderId, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, senderId)) {
            throw new ForbiddenException("You are not a member of this conversation");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));

        // Check blocking policy for direct conversation
        if (conversation.getType() == ConversationType.DIRECT) {
            for (ConversationMember member : conversation.getMembers()) {
                if (!member.getUser().getId().equals(senderId)) {
                    if (blockedUserRepository.isBlockedEitherWay(senderId, member.getUser().getId())) {
                        throw new BadRequestException("Cannot send message: Communication is blocked with this user");
                    }
                }
            }
        }

        // Idempotency check: if client sent idempotencyKey, prevent duplicate inserts
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
            Optional<Message> existing = messageRepository.findByConversationIdAndIdempotencyKey(conversationId, request.getIdempotencyKey());
            if (existing.isPresent()) {
                return messageMapper.toDto(existing.get(), senderId);
            }
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.getContent() != null ? request.getContent().trim() : "");
        message.setType(request.getType() != null ? request.getType() : MessageType.TEXT);
        message.setStatus(MessageStatus.SENT);
        message.setIdempotencyKey(request.getIdempotencyKey());

        // Handle Threaded Reply
        if (request.getReplyToId() != null) {
            messageRepository.findById(request.getReplyToId()).ifPresent(replyTo -> {
                if (replyTo.getConversation().getId().equals(conversationId)) {
                    message.setReplyTo(replyTo);
                }
            });
        }

        Message savedMessage = messageRepository.save(message);

        // Update conversation lastMessageAt
        conversation.setLastMessageAt(savedMessage.getCreatedAt());
        conversationRepository.save(conversation);

        MessageDto messageDto = messageMapper.toDto(savedMessage, senderId);

        // Real-time broadcast to conversation topic
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, messageDto);

        return messageDto;
    }

    @Override
    @Transactional
    public MessageDto editMessage(Long messageId, Long userId, EditMessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        if (!message.getSender().getId().equals(userId)) {
            throw new ForbiddenException("You can only edit messages you sent");
        }

        if (message.isDeleted()) {
            throw new BadRequestException("Cannot edit a deleted message");
        }

        Duration age = Duration.between(message.getCreatedAt(), Instant.now());
        if (age.compareTo(EDIT_TIME_LIMIT) > 0) {
            throw new BadRequestException("Messages can only be edited within 15 minutes of sending");
        }

        message.setContent(request.getContent().trim());
        message.setEdited(true);
        message.setEditedAt(Instant.now());

        Message updated = messageRepository.save(message);
        MessageDto dto = messageMapper.toDto(updated, userId);

        // Real-time broadcast updated message
        messagingTemplate.convertAndSend("/topic/conversations/" + message.getConversation().getId(), dto);

        return dto;
    }

    @Override
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        Long conversationId = message.getConversation().getId();
        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this conversation"));

        boolean isSender = message.getSender().getId().equals(userId);
        boolean isOwnerOrAdmin = member.getRole() == GroupRole.OWNER || member.getRole() == GroupRole.ADMIN;

        if (!isSender && !isOwnerOrAdmin) {
            throw new ForbiddenException("You do not have permission to delete this message");
        }

        message.setDeleted(true);
        message.setContent("This message was deleted");
        messageRepository.save(message);

        MessageDto dto = messageMapper.toDto(message, userId);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, dto);
    }

    @Override
    @Transactional
    public void toggleStarMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        if (!conversationMemberRepository.existsByConversationIdAndUserId(message.getConversation().getId(), userId)) {
            throw new ForbiddenException("You are not a member of this conversation");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (message.getStarredByUsers().contains(user)) {
            message.getStarredByUsers().remove(user);
        } else {
            message.getStarredByUsers().add(user);
        }

        messageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageDto> getStarredMessages(Long userId) {
        List<Message> starred = messageRepository.findStarredMessagesByUser(userId);
        return starred.stream()
                .map(m -> messageMapper.toDto(m, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageDto> searchMessages(Long conversationId, Long userId, String keyword) {
        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ForbiddenException("You are not authorized to search this conversation");
        }

        List<Message> results = messageRepository.searchMessages(conversationId, keyword.trim());
        return results.stream()
                .map(m -> messageMapper.toDto(m, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public String exportConversationAsText(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ForbiddenException("You are not authorized to export this conversation");
        }

        // Fetch messages chronologically
        Page<Message> page = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, 1000));
        List<Message> messages = page.getContent();

        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================\n");
        sb.append(" CONNECT CHAT - EXPORTED CONVERSATION TRANSCRIPT\n");
        sb.append(" Title: ").append(conversation.getTitle() != null ? conversation.getTitle() : "Direct Chat").append("\n");
        sb.append(" Exported at: ").append(EXPORT_FORMATTER.format(Instant.now())).append(" UTC\n");
        sb.append(" Total Messages: ").append(messages.size()).append("\n");
        sb.append("=================================================================\n\n");

        // Iterate in chronological order
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message m = messages.get(i);
            String formattedTime = EXPORT_FORMATTER.format(m.getCreatedAt());
            String sender = m.getSender().getDisplayName() + " (@" + m.getSender().getUsername() + ")";
            String content = m.isDeleted() ? "[Message Deleted]" : m.getContent();

            sb.append(String.format("[%s] %s: %s\n", formattedTime, sender, content));
        }

        return sb.toString();
    }
}

