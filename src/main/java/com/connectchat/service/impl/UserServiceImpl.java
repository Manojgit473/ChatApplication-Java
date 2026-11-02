package com.connectchat.service.impl;

import com.connectchat.dto.request.PasswordChangeRequest;
import com.connectchat.dto.request.ProfileUpdateRequest;
import com.connectchat.dto.response.UserProfileDto;
import com.connectchat.dto.response.UserSummaryDto;
import com.connectchat.entity.BlockedUser;
import com.connectchat.entity.User;
import com.connectchat.exception.BadRequestException;
import com.connectchat.exception.ResourceNotFoundException;
import com.connectchat.mapper.UserMapper;
import com.connectchat.repository.BlockedUserRepository;
import com.connectchat.repository.UserRepository;
import com.connectchat.service.UserService;
import com.connectchat.util.StringValidationUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of UserService.
 * 
 * Demonstrates:
 * - Declarative transactions with readOnly optimizations.
 * - Password verification using BCryptPasswordEncoder.
 * - Java Stream API for filtering and transforming collections.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository,
                           BlockedUserRepository blockedUserRepository,
                           PasswordEncoder passwordEncoder,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return userMapper.toProfileDto(user);
    }

    @Override
    @Transactional
    public UserProfileDto updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setDisplayName(request.getDisplayName().trim());
        if (request.getPhoneNumber() != null) {
            String normalizedPhone = com.connectchat.util.PhoneNumberUtils.normalizeIndianPhoneNumber(request.getPhoneNumber());
            user.setPhoneNumber(normalizedPhone);
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        User updated = userRepository.save(user);
        return userMapper.toProfileDto(updated);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password provided does not match");
        }

        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new BadRequestException("New password cannot be identical to current password");
        }

        String passwordError = StringValidationUtils.validatePasswordStrength(request.getNewPassword());
        if (passwordError != null) {
            throw new BadRequestException(passwordError);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDto> searchUsers(String query, Long currentUserId) {
        String cleanQuery = (query != null) ? query.trim() : "";
        List<User> found = userRepository.searchUsers(cleanQuery, currentUserId);

        return found.stream()
                .filter(u -> !blockedUserRepository.isBlockedEitherWay(currentUserId, u.getId()))
                .map(userMapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllContacts(Long currentUserId) {
        List<User> users = userRepository.findAllExceptCurrent(currentUserId);
        return users.stream()
                .filter(u -> !blockedUserRepository.isBlockedEitherWay(currentUserId, u.getId()))
                .map(userMapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void blockUser(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("You cannot block yourself");
        }

        if (blockedUserRepository.existsByBlockerIdAndBlockedId(currentUserId, targetUserId)) {
            return; // Already blocked
        }

        User blocker = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target User", "id", targetUserId));

        BlockedUser block = new BlockedUser(blocker, blocked);
        blockedUserRepository.save(block);
    }

    @Override
    @Transactional
    public void unblockUser(Long currentUserId, Long targetUserId) {
        blockedUserRepository.deleteByBlockerIdAndBlockedId(currentUserId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getBlockedUsers(Long currentUserId) {
        List<BlockedUser> list = blockedUserRepository.findByBlockerIdWithBlockedUser(currentUserId);
        return list.stream()
                .map(b -> userMapper.toSummaryDto(b.getBlocked()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedEitherWay(Long user1Id, Long user2Id) {
        return blockedUserRepository.isBlockedEitherWay(user1Id, user2Id);
    }
}

