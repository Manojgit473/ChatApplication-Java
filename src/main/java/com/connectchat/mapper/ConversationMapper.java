package com.connectchat.mapper;

import com.connectchat.dto.response.ConversationDetailDto;
import com.connectchat.dto.response.ConversationMemberDto;
import com.connectchat.dto.response.ConversationSummaryDto;
import com.connectchat.dto.response.MessageDto;
import com.connectchat.dto.response.UserSummaryDto;
import com.connectchat.entity.Conversation;
import com.connectchat.entity.ConversationMember;
import com.connectchat.entity.User;
import com.connectchat.enums.ConversationType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting Conversation entities into summary and detail DTOs.
 */
@Component
public class ConversationMapper {

    private final UserMapper userMapper;

    public ConversationMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public ConversationSummaryDto toSummaryDto(Conversation conversation, Long currentUserId, MessageDto lastMessage, long unreadCount, ConversationMember currentMember) {
        if (conversation == null) {
            return null;
        }

        ConversationSummaryDto dto = new ConversationSummaryDto();
        dto.setId(conversation.getId());
        dto.setType(conversation.getType());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setLastMessage(lastMessage);
        dto.setUnreadCount(unreadCount);

        if (currentMember != null) {
            dto.setMuted(currentMember.isMuted());
            dto.setArchived(currentMember.isArchived());
        }

        if (conversation.getType() == ConversationType.DIRECT) {
            User otherUser = findOtherParticipant(conversation, currentUserId);
            if (otherUser != null) {
                dto.setTitle(otherUser.getDisplayName());
                dto.setGroupAvatarUrl(otherUser.getAvatarUrl());
                dto.setDirectParticipant(userMapper.toSummaryDto(otherUser));
            } else {
                dto.setTitle("Direct Conversation");
            }
            dto.setMemberCount(2);
        } else {
            dto.setTitle(conversation.getTitle());
            dto.setGroupAvatarUrl(conversation.getGroupAvatarUrl());
            dto.setMemberCount(conversation.getMembers() != null ? conversation.getMembers().size() : 0);
        }

        return dto;
    }

    public ConversationDetailDto toDetailDto(Conversation conversation, Long currentUserId, ConversationMember currentMember) {
        if (conversation == null) {
            return null;
        }

        ConversationDetailDto dto = new ConversationDetailDto();
        dto.setId(conversation.getId());
        dto.setType(conversation.getType());
        dto.setDescription(conversation.getDescription());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setCreatedBy(userMapper.toSummaryDto(conversation.getCreatedBy()));

        if (currentMember != null) {
            dto.setMuted(currentMember.isMuted());
            dto.setArchived(currentMember.isArchived());
        }

        if (conversation.getType() == ConversationType.DIRECT) {
            User otherUser = findOtherParticipant(conversation, currentUserId);
            if (otherUser != null) {
                dto.setTitle(otherUser.getDisplayName());
                dto.setGroupAvatarUrl(otherUser.getAvatarUrl());
                dto.setDirectParticipant(userMapper.toSummaryDto(otherUser));
            } else {
                dto.setTitle("Direct Conversation");
            }
        } else {
            dto.setTitle(conversation.getTitle());
            dto.setGroupAvatarUrl(conversation.getGroupAvatarUrl());
        }

        if (conversation.getMembers() != null) {
            List<ConversationMemberDto> memberDtos = new ArrayList<>();
            for (ConversationMember m : conversation.getMembers()) {
                UserSummaryDto userSummary = userMapper.toSummaryDto(m.getUser());
                memberDtos.add(new ConversationMemberDto(userSummary, m.getRole(), m.getJoinedAt()));
            }
            dto.setMembers(memberDtos);
        }

        return dto;
    }

    private User findOtherParticipant(Conversation conversation, Long currentUserId) {
        if (conversation.getMembers() == null) {
            return null;
        }
        for (ConversationMember member : conversation.getMembers()) {
            if (member.getUser() != null && !member.getUser().getId().equals(currentUserId)) {
                return member.getUser();
            }
        }
        return null;
    }
}

