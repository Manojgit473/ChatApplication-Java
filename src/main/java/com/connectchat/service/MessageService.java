package com.connectchat.service;

import com.connectchat.dto.request.EditMessageRequest;
import com.connectchat.dto.request.SendMessageRequest;
import com.connectchat.dto.response.MessageDto;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service interface for persistent message creation, retrieval, updates, and indexing.
 */
public interface MessageService {

    Page<MessageDto> getMessages(Long conversationId, Long userId, int page, int size);

    MessageDto sendMessage(Long conversationId, Long senderId, SendMessageRequest request);

    MessageDto editMessage(Long messageId, Long userId, EditMessageRequest request);

    void deleteMessage(Long messageId, Long userId);

    void toggleStarMessage(Long messageId, Long userId);

    List<MessageDto> getStarredMessages(Long userId);

    List<MessageDto> searchMessages(Long conversationId, Long userId, String keyword);

    String exportConversationAsText(Long conversationId, Long userId);
}
