package com.connectchat.service.impl;

import com.connectchat.dto.response.PresenceDto;
import com.connectchat.entity.User;
import com.connectchat.enums.OnlineStatus;
import com.connectchat.exception.ResourceNotFoundException;
import com.connectchat.repository.UserRepository;
import com.connectchat.service.PresenceService;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Implementation of PresenceService for real-time presence management.
 */
@Service
public class PresenceServiceImpl implements PresenceService {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceServiceImpl(UserRepository userRepository, @Lazy SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public void setUserOnlineStatus(Long userId, OnlineStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setOnlineStatus(status);
        user.setLastSeenAt(Instant.now());
        userRepository.save(user);

        PresenceDto presenceDto = new PresenceDto(user.getId(), user.getUsername(), status, user.getLastSeenAt());
        messagingTemplate.convertAndSend("/topic/presence", presenceDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PresenceDto getUserPresence(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return new PresenceDto(user.getId(), user.getUsername(), user.getOnlineStatus(), user.getLastSeenAt());
    }
}

