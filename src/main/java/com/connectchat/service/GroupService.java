package com.connectchat.service;

import com.connectchat.dto.request.CreateGroupRequest;
import com.connectchat.dto.request.UpdateGroupRequest;
import com.connectchat.dto.response.ConversationDetailDto;

/**
 * Service interface for Group conversation lifecycle and member access controls.
 */
public interface GroupService {

    ConversationDetailDto createGroup(Long creatorId, CreateGroupRequest request);

    ConversationDetailDto updateGroup(Long groupId, Long userId, UpdateGroupRequest request);

    void addGroupMember(Long groupId, Long requesterId, Long newUserId);

    void removeGroupMember(Long groupId, Long requesterId, Long memberUserId);

    void leaveGroup(Long groupId, Long userId);

    void deleteGroup(Long groupId, Long userId);
}

