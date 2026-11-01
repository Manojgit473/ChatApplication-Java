package com.connectchat.service.impl;

import com.connectchat.dto.request.CreateGroupRequest;
import com.connectchat.dto.request.UpdateGroupRequest;
import com.connectchat.dto.response.ConversationDetailDto;
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
import com.connectchat.repository.ConversationMemberRepository;
import com.connectchat.repository.ConversationRepository;
import com.connectchat.repository.MessageRepository;
import com.connectchat.repository.UserRepository;
import com.connectchat.service.GroupService;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of GroupService.
 * 
 * Demonstrates:
 * - Role-Based Access Control (RBAC) at the business service layer (OWNER, ADMIN, MEMBER).
 * - System-generated audit messages persisted into group chat stream.
 */
@Service
public class GroupServiceImpl implements GroupService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public GroupServiceImpl(ConversationRepository conversationRepository,
                            ConversationMemberRepository conversationMemberRepository,
                            UserRepository userRepository,
                            MessageRepository messageRepository,
                            ConversationMapper conversationMapper,
                            @Lazy SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.conversationMapper = conversationMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public ConversationDetailDto createGroup(Long creatorId, CreateGroupRequest request) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorId));

        Conversation group = new Conversation();
        group.setType(ConversationType.GROUP);
        group.setTitle(request.getTitle().trim());
        group.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        group.setCreatedBy(creator);
        group.setLastMessageAt(Instant.now());

        Conversation savedGroup = conversationRepository.save(group);

        // Add creator as OWNER
        ConversationMember ownerMember = new ConversationMember(savedGroup, creator, GroupRole.OWNER);
        conversationMemberRepository.save(ownerMember);
        savedGroup.getMembers().add(ownerMember);

        // Add initial members
        if (request.getMemberIds() != null) {
            for (Long memberId : request.getMemberIds()) {
                if (!memberId.equals(creatorId)) {
                    userRepository.findById(memberId).ifPresent(user -> {
                        ConversationMember m = new ConversationMember(savedGroup, user, GroupRole.MEMBER);
                        conversationMemberRepository.save(m);
                        savedGroup.getMembers().add(m);
                    });
                }
            }
        }

        // Create initial system message
        Message sysMsg = new Message(savedGroup, creator, creator.getDisplayName() + " created group \"" + group.getTitle() + "\"", MessageType.SYSTEM);
        messageRepository.save(sysMsg);

        return conversationMapper.toDetailDto(savedGroup, creatorId, ownerMember);
    }

    @Override
    @Transactional
    public ConversationDetailDto updateGroup(Long groupId, Long userId, UpdateGroupRequest request) {
        Conversation group = conversationRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));

        if (group.getType() != ConversationType.GROUP) {
            throw new BadRequestException("Specified conversation is not a group");
        }

        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this group"));

        if (member.getRole() != GroupRole.OWNER && member.getRole() != GroupRole.ADMIN) {
            throw new ForbiddenException("Only group owners and admins can edit group settings");
        }

        group.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription().trim());
        }
        if (request.getGroupAvatarUrl() != null) {
            group.setGroupAvatarUrl(request.getGroupAvatarUrl().trim());
        }

        Conversation updated = conversationRepository.save(group);
        return conversationMapper.toDetailDto(updated, userId, member);
    }

    @Override
    @Transactional
    public void addGroupMember(Long groupId, Long requesterId, Long newUserId) {
        Conversation group = conversationRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));

        if (group.getType() != ConversationType.GROUP) {
            throw new BadRequestException("Specified conversation is not a group");
        }

        ConversationMember requesterMember = conversationMemberRepository.findByConversationIdAndUserId(groupId, requesterId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this group"));

        if (requesterMember.getRole() != GroupRole.OWNER && requesterMember.getRole() != GroupRole.ADMIN) {
            throw new ForbiddenException("Only group owners and admins can add members");
        }

        if (conversationMemberRepository.existsByConversationIdAndUserId(groupId, newUserId)) {
            throw new BadRequestException("User is already a member of this group");
        }

        User newUser = userRepository.findById(newUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", newUserId));

        ConversationMember newMember = new ConversationMember(group, newUser, GroupRole.MEMBER);
        conversationMemberRepository.save(newMember);

        // System message
        Message sysMsg = new Message(group, requesterMember.getUser(), newUser.getDisplayName() + " was added to the group", MessageType.SYSTEM);
        messageRepository.save(sysMsg);
    }

    @Override
    @Transactional
    public void removeGroupMember(Long groupId, Long requesterId, Long memberUserId) {
        Conversation group = conversationRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));

        ConversationMember requester = conversationMemberRepository.findByConversationIdAndUserId(groupId, requesterId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this group"));

        if (requester.getRole() != GroupRole.OWNER && requester.getRole() != GroupRole.ADMIN) {
            throw new ForbiddenException("Only group owners and admins can remove members");
        }

        ConversationMember target = conversationMemberRepository.findByConversationIdAndUserId(groupId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "userId", memberUserId));

        if (target.getRole() == GroupRole.OWNER) {
            throw new BadRequestException("Group owner cannot be removed");
        }

        conversationMemberRepository.delete(target);

        Message sysMsg = new Message(group, requester.getUser(), target.getUser().getDisplayName() + " was removed from the group", MessageType.SYSTEM);
        messageRepository.save(sysMsg);
    }

    @Override
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        Conversation group = conversationRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));

        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this group"));

        long remainingMembers = conversationMemberRepository.countByConversationId(groupId);
        if (member.getRole() == GroupRole.OWNER && remainingMembers > 1) {
            // Find another member and promote them to OWNER
            List<ConversationMember> others = conversationMemberRepository.findByConversationId(groupId);
            for (ConversationMember other : others) {
                if (!other.getUser().getId().equals(userId)) {
                    other.setRole(GroupRole.OWNER);
                    conversationMemberRepository.save(other);
                    break;
                }
            }
        }

        conversationMemberRepository.delete(member);

        if (remainingMembers <= 1) {
            // If last member left, clean up group
            conversationRepository.delete(group);
        } else {
            Message sysMsg = new Message(group, member.getUser(), member.getUser().getDisplayName() + " left the group", MessageType.SYSTEM);
            messageRepository.save(sysMsg);
        }
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        Conversation group = conversationRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));

        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this group"));

        if (member.getRole() != GroupRole.OWNER) {
            throw new ForbiddenException("Only the group owner can delete this group");
        }

        conversationRepository.delete(group);
    }
}
