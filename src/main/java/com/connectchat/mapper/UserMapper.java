package com.connectchat.mapper;

import com.connectchat.dto.response.UserProfileDto;
import com.connectchat.dto.response.UserSummaryDto;
import com.connectchat.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting User entities to safe external DTOs.
 */
@Component
public class UserMapper {

    public UserSummaryDto toSummaryDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getOnlineStatus(),
                user.getLastSeenAt()
        );
    }

    public UserProfileDto toProfileDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getOnlineStatus(),
                user.getLastSeenAt(),
                user.getCreatedAt()
        );
    }
}
