package com.connectchat.service;

import com.connectchat.dto.request.CreateConversationRequest;
import com.connectchat.dto.response.ConversationDetailDto;
import com.connectchat.dto.response.ConversationSummaryDto;

import java.util.List;

/**
 * Service interface for Direct and Group conversation operations.
 */
public interface ConversationService {

    List<ConversationSummaryDto> getUserConversations(Long userId);

    ConversationDetailDto getConversationDetail(Long conversationId, Long userId);

    ConversationDetailDto createDirectConversation(Long currentUserId, CreateConversationRequest request);

    void muteConversation(Long conversationId, Long userId, boolean muted);

    void archiveConversation(Long conversationId, Long userId, boolean archived);

    void markConversationAsRead(Long conversationId, Long userId);
}
