package com.connectchat.mapper;

import com.connectchat.dto.response.AttachmentDto;
import com.connectchat.dto.response.MessageDto;
import com.connectchat.entity.Attachment;
import com.connectchat.entity.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting Message and Attachment entities to DTOs.
 */
@Component
public class MessageMapper {

    private final UserMapper userMapper;

    public MessageMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public MessageDto toDto(Message message, Long currentUserId) {
        if (message == null) {
            return null;
        }

        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSender(userMapper.toSummaryDto(message.getSender()));
        dto.setType(message.getType());
        dto.setStatus(message.getStatus());
        dto.setIdempotencyKey(message.getIdempotencyKey());
        dto.setEdited(message.isEdited());
        dto.setEditedAt(message.getEditedAt());
        dto.setDeleted(message.isDeleted());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setExpiresAt(message.getExpiresAt());
        dto.setExpiryDuration(message.getExpiryDuration());

        boolean isExp = message.isExpired() || (message.getExpiresAt() != null && message.getExpiresAt().isBefore(java.time.Instant.now()));
        dto.setExpired(isExp);

        if (message.isDeleted()) {
            dto.setContent("This message was deleted");
            dto.setAttachments(new ArrayList<>());
        } else if (isExp) {
            dto.setContent("This message has self-destructed");
            dto.setAttachments(new ArrayList<>());
        } else {
            dto.setContent(message.getContent());
            if (message.getAttachments() != null) {
                List<AttachmentDto> attachmentDtos = new ArrayList<>();
                for (Attachment att : message.getAttachments()) {
                    attachmentDtos.add(toAttachmentDto(att));
                }
                dto.setAttachments(attachmentDtos);
            }
        }

        // Starred indicator check for current user
        if (currentUserId != null && message.getStarredByUsers() != null) {
            boolean isStarred = message.getStarredByUsers().stream()
                    .anyMatch(u -> u.getId().equals(currentUserId));
            dto.setStarred(isStarred);
        }

        // Map replyTo message (shallow mapping to prevent recursion)
        if (message.getReplyTo() != null) {
            Message reply = message.getReplyTo();
            MessageDto replyDto = new MessageDto();
            replyDto.setId(reply.getId());
            replyDto.setSender(userMapper.toSummaryDto(reply.getSender()));
            replyDto.setContent(reply.isDeleted() ? "This message was deleted" : reply.getContent());
            replyDto.setType(reply.getType());
            replyDto.setCreatedAt(reply.getCreatedAt());
            dto.setReplyTo(replyDto);
        }

        return dto;
    }

    public AttachmentDto toAttachmentDto(Attachment attachment) {
        if (attachment == null) {
            return null;
        }
        return new AttachmentDto(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getStoredFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                "/api/attachments/" + attachment.getStoredFileName(),
                attachment.getCreatedAt()
        );
    }
}

