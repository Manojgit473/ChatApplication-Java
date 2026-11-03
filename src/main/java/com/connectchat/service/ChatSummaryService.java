package com.connectchat.service;

import com.connectchat.dto.response.ChatSummaryDto;

/**
 * Service interface for AI-Powered Conversation Summarization.
 * 
 * Separates AI provider concerns from business and presentation logic,
 * allowing modular substitution of LLM backends (e.g., Google Gemini, OpenAI, Anthropic, or local SLMs).
 */
public interface ChatSummaryService {

    /**
     * Analyzes conversation message stream and produces an actionable structured summary.
     *
     * @param conversationId The ID of the conversation to summarize.
     * @param userId The ID of the requesting user (must be an authorized member).
     * @return Structured summary containing topics, decisions, action items, and deadlines.
     */
    ChatSummaryDto summarizeConversation(Long conversationId, Long userId);
}
