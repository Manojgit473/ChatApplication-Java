package com.connectchat.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for AI-Powered Conversation Summary.
 */
public class ChatSummaryDto {

    private Long conversationId;
    private String conversationTitle;
    private int messageCountAnalyzed;
    private String overview;
    private List<String> topics = new ArrayList<>();
    private List<String> keyInformation = new ArrayList<>();
    private List<String> decisions = new ArrayList<>();
    private List<String> actionItems = new ArrayList<>();
    private List<String> deadlines = new ArrayList<>();
    private String rawSummary;
    private String aiModelUsed;
    private Instant generatedAt = Instant.now();

    public ChatSummaryDto() {
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getConversationTitle() {
        return conversationTitle;
    }

    public void setConversationTitle(String conversationTitle) {
        this.conversationTitle = conversationTitle;
    }

    public int getMessageCountAnalyzed() {
        return messageCountAnalyzed;
    }

    public void setMessageCountAnalyzed(int messageCountAnalyzed) {
        this.messageCountAnalyzed = messageCountAnalyzed;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics != null ? topics : new ArrayList<>();
    }

    public List<String> getKeyInformation() {
        return keyInformation;
    }

    public void setKeyInformation(List<String> keyInformation) {
        this.keyInformation = keyInformation != null ? keyInformation : new ArrayList<>();
    }

    public List<String> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<String> decisions) {
        this.decisions = decisions != null ? decisions : new ArrayList<>();
    }

    public List<String> getActionItems() {
        return actionItems;
    }

    public void setActionItems(List<String> actionItems) {
        this.actionItems = actionItems != null ? actionItems : new ArrayList<>();
    }

    public List<String> getDeadlines() {
        return deadlines;
    }

    public void setDeadlines(List<String> deadlines) {
        this.deadlines = deadlines != null ? deadlines : new ArrayList<>();
    }

    public String getRawSummary() {
        return rawSummary;
    }

    public void setRawSummary(String rawSummary) {
        this.rawSummary = rawSummary;
    }

    public String getAiModelUsed() {
        return aiModelUsed;
    }

    public void setAiModelUsed(String aiModelUsed) {
        this.aiModelUsed = aiModelUsed;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
