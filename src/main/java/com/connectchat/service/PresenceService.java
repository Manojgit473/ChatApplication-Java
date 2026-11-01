package com.connectchat.service;

import com.connectchat.dto.response.PresenceDto;
import com.connectchat.enums.OnlineStatus;

/**
 * Service interface for tracking user presence across sessions.
 */
public interface PresenceService {

    void setUserOnlineStatus(Long userId, OnlineStatus status);

    PresenceDto getUserPresence(Long userId);
}
