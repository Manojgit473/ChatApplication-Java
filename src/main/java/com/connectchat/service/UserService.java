package com.connectchat.service;

import com.connectchat.dto.request.PasswordChangeRequest;
import com.connectchat.dto.request.ProfileUpdateRequest;
import com.connectchat.dto.response.UserProfileDto;
import com.connectchat.dto.response.UserSummaryDto;

import java.util.List;

/**
 * Service interface for User profile, discovery, and blocking management.
 */
public interface UserService {

    UserProfileDto getUserProfile(Long userId);

    UserProfileDto updateProfile(Long userId, ProfileUpdateRequest request);

    void changePassword(Long userId, PasswordChangeRequest request);

    List<UserSummaryDto> searchUsers(String query, Long currentUserId);

    List<UserSummaryDto> getAllContacts(Long currentUserId);

    void blockUser(Long currentUserId, Long targetUserId);

    void unblockUser(Long currentUserId, Long targetUserId);

    List<UserSummaryDto> getBlockedUsers(Long currentUserId);

    boolean isBlockedEitherWay(Long user1Id, Long user2Id);
}
