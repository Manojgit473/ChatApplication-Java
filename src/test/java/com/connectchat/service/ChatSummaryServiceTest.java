package com.connectchat.service;

import com.connectchat.dto.response.ChatSummaryDto;
import com.connectchat.entity.Conversation;
import com.connectchat.entity.Message;
import com.connectchat.entity.User;
import com.connectchat.enums.ConversationType;
import com.connectchat.enums.MessageType;
import com.connectchat.exception.ForbiddenException;
import com.connectchat.repository.ConversationMemberRepository;
import com.connectchat.repository.ConversationRepository;
import com.connectchat.repository.MessageRepository;
import com.connectchat.service.impl.ChatSummaryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatSummaryServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository memberRepository;

    @Mock
    private MessageRepository messageRepository;

    private ChatSummaryServiceImpl chatSummaryService;

    private Conversation conversation;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        chatSummaryService = new ChatSummaryServiceImpl(conversationRepository, memberRepository, messageRepository, new ObjectMapper());
        ReflectionTestUtils.setField(chatSummaryService, "apiKey", ""); // Test offline / heuristic fallback
        ReflectionTestUtils.setField(chatSummaryService, "modelName", "gemini-2.5-flash");

        conversation = new Conversation();
        conversation.setId(200L);
        conversation.setTitle("Project AI Team");
        conversation.setType(ConversationType.GROUP);

        alice = new User("alice", "alice@test.com", "pass", "Alice Smith");
        alice.setId(1L);

        bob = new User("bob", "bob@test.com", "pass", "Bob Jones");
        bob.setId(2L);
    }

    @Test
    @DisplayName("summarizeConversation should throw ForbiddenException when user is not a conversation member")
    void testSummarizeConversation_WhenUserNotMember_ShouldThrowForbidden() {
        when(memberRepository.existsByConversationIdAndUserId(200L, 999L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> chatSummaryService.summarizeConversation(200L, 999L));
        verify(messageRepository, never()).findActiveMessagesForSummary(anyLong());
    }

    @Test
    @DisplayName("summarizeConversation should return empty summary when no active messages exist")
    void testSummarizeConversation_WhenNoMessages_ShouldReturnEmptyNotice() {
        when(conversationRepository.findById(200L)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationIdAndUserId(200L, 1L)).thenReturn(true);
        when(messageRepository.findActiveMessagesForSummary(200L)).thenReturn(Collections.emptyList());

        ChatSummaryDto result = chatSummaryService.summarizeConversation(200L, 1L);

        assertNotNull(result);
        assertEquals(0, result.getMessageCountAnalyzed());
        assertTrue(result.getOverview().contains("No active messages"));
    }

    @Test
    @DisplayName("summarizeConversation should generate structured summary with topics, decisions, action items, and deadlines")
    void testSummarizeConversation_WithMessages_ShouldExtractStructuredSummary() {
        when(conversationRepository.findById(200L)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationIdAndUserId(200L, 1L)).thenReturn(true);

        Message m1 = new Message(conversation, alice, "We decided to submit the project proposal tomorrow by 5 PM.", MessageType.TEXT);
        m1.setId(10L);
        m1.setCreatedAt(java.time.Instant.now());

        Message m2 = new Message(conversation, bob, "I will complete the database schema and upload the report by Friday.", MessageType.TEXT);
        m2.setId(11L);
        m2.setCreatedAt(java.time.Instant.now());

        Message m3 = new Message(conversation, alice, "Great! Meeting scheduled for Monday at 10 AM to finalize the presentation.", MessageType.TEXT);
        m3.setId(12L);
        m3.setCreatedAt(java.time.Instant.now());

        when(messageRepository.findActiveMessagesForSummary(200L)).thenReturn(List.of(m1, m2, m3));

        ChatSummaryDto result = chatSummaryService.summarizeConversation(200L, 1L);

        assertNotNull(result);
        assertEquals(3, result.getMessageCountAnalyzed());
        assertNotNull(result.getOverview());
        assertFalse(result.getOverview().isEmpty());

        // Verify Topics
        assertNotNull(result.getTopics());
        assertFalse(result.getTopics().isEmpty());

        // Verify Decisions
        assertNotNull(result.getDecisions());
        assertFalse(result.getDecisions().isEmpty());

        // Verify Action Items
        assertNotNull(result.getActionItems());
        assertFalse(result.getActionItems().isEmpty());

        // Verify Deadlines
        assertNotNull(result.getDeadlines());
        assertFalse(result.getDeadlines().isEmpty());
    }
}
