package com.connectchat.service.impl;

import com.connectchat.dto.response.ChatSummaryDto;
import com.connectchat.entity.Conversation;
import com.connectchat.entity.Message;
import com.connectchat.enums.ConversationType;
import com.connectchat.enums.MessageType;
import com.connectchat.exception.ForbiddenException;
import com.connectchat.exception.ResourceNotFoundException;
import com.connectchat.repository.ConversationMemberRepository;
import com.connectchat.repository.ConversationRepository;
import com.connectchat.repository.MessageRepository;
import com.connectchat.service.ChatSummaryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of ChatSummaryService integrating Google Gemini API.
 * 
 * Demonstrates:
 * - Enterprise AI integration pattern: Configurable API key, prompt engineering, and structured JSON parsing.
 * - Robust error handling with graceful local heuristic fallback when AI endpoint is offline or unconfigured.
 * - Strict authorization checks and automated exclusion of deleted or self-destructed messages.
 */
@Service
public class ChatSummaryServiceImpl implements ChatSummaryService {

    private static final Logger log = LoggerFactory.getLogger(ChatSummaryServiceImpl.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${gemini.api.key:${AI_API_KEY:${GEMINI_API_KEY:}}}")
    private String apiKey;

    @Value("${gemini.model:${AI_MODEL:gemini-2.5-flash}}")
    private String modelName;

    @org.springframework.beans.factory.annotation.Autowired
    public ChatSummaryServiceImpl(ConversationRepository conversationRepository,
                                  ConversationMemberRepository conversationMemberRepository,
                                  MessageRepository messageRepository) {
        this(conversationRepository, conversationMemberRepository, messageRepository, new ObjectMapper());
    }

    public ChatSummaryServiceImpl(ConversationRepository conversationRepository,
                                  ConversationMemberRepository conversationMemberRepository,
                                  MessageRepository messageRepository,
                                  ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.messageRepository = messageRepository;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatSummaryDto summarizeConversation(Long conversationId, Long userId) {
        // 1. Authorization check: user must be an active member of this conversation
        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ForbiddenException("You are not authorized to access or summarize this conversation");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        String title = conversation.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = conversation.getType() == ConversationType.GROUP ? "Group Chat" : "Direct Conversation";
        }

        // 2. Retrieve only active, non-deleted, non-expired messages
        List<Message> rawMessages = messageRepository.findActiveMessagesForSummary(conversationId);
        Instant now = Instant.now();
        List<Message> messages = rawMessages.stream()
                .filter(m -> !m.isExpired() && (m.getExpiresAt() == null || m.getExpiresAt().isAfter(now)))
                .collect(Collectors.toList());

        ChatSummaryDto dto = new ChatSummaryDto();
        dto.setConversationId(conversationId);
        dto.setConversationTitle(title);
        dto.setMessageCountAnalyzed(messages.size());
        dto.setGeneratedAt(Instant.now());

        if (messages.isEmpty()) {
            dto.setOverview("No active messages found in this conversation to analyze. Self-destructed or deleted messages are safely excluded.");
            dto.setTopics(List.of("No Active Topics"));
            dto.setAiModelUsed("System Filter");
            return dto;
        }

        // 3. Format message transcript for AI processing
        StringBuilder transcript = new StringBuilder();
        for (Message msg : messages) {
            String timeStr = msg.getCreatedAt() != null ? TIME_FORMATTER.format(msg.getCreatedAt()) : "Recent";
            if (msg.getType() == MessageType.SYSTEM) {
                transcript.append(String.format("[%s] (SYSTEM): %s%n",
                        timeStr, msg.getContent()));
            } else {
                String senderName = msg.getSender() != null ? msg.getSender().getDisplayName() : "Unknown";
                String text = msg.getContent() != null ? msg.getContent() : "[Attachment]";
                transcript.append(String.format("[%s] %s: %s%n",
                        timeStr, senderName, text));
            }
        }

        // 4. Try AI generation via Google Gemini API if key is configured
        if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.contains("your_")) {
            try {
                ChatSummaryDto aiSummary = callGeminiApi(conversationId, title, messages.size(), transcript.toString());
                if (aiSummary != null) {
                    return aiSummary;
                }
            } catch (Exception e) {
                log.warn("[AI-SUMMARY] Gemini API call failed for conversation {}: {}", conversationId, e.getMessage());
            }
        }

        // 5. Intelligent Local Heuristic Analyzer (Fallback when Gemini key is pending or offline)
        log.info("[AI-SUMMARY] Using Intelligent Local Heuristic Analyzer for conversation {}", conversationId);
        return generateLocalSummary(conversationId, title, messages);
    }

    private ChatSummaryDto callGeminiApi(Long conversationId, String title, int messageCount, String transcript) throws Exception {
        String prompt = "You are an expert AI Smart Chat Summarizer for Connect Chat.\n" +
                "Analyze the following conversation transcript from \"" + title + "\" and produce an accurate, structured JSON summary.\n\n" +
                "Required JSON fields:\n" +
                "{\n" +
                "  \"overview\": \"A concise 2-3 sentence overview capturing the core discussion and sentiment.\",\n" +
                "  \"topics\": [\"Topic 1\", \"Topic 2\", ...],\n" +
                "  \"keyInformation\": [\"Key fact or detail discussed 1\", ...],\n" +
                "  \"decisions\": [\"Decision made or agreed upon 1\", ...],\n" +
                "  \"actionItems\": [\"Task assigned or actionable next step 1\", ...],\n" +
                "  \"deadlines\": [\"Any specific date, milestone, or deadline mentioned 1\", ...]\n" +
                "}\n\n" +
                "Transcript to analyze:\n" + transcript + "\n\n" +
                "Respond strictly with valid JSON only.";

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> contentObj = Map.of("parts", List.of(part));
        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.2,
                "responseMimeType", "application/json"
        );
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(contentObj),
                "generationConfig", generationConfig
        );

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        String endpoint = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                modelName.trim(), apiKey.trim());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("[AI-SUMMARY] Gemini API returned status {}: {}", response.statusCode(), response.body());
            return null;
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        JsonNode candidates = rootNode.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return null;
        }

        String rawText = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
        if (rawText == null || rawText.trim().isEmpty()) {
            return null;
        }

        // Clean code block fences if returned
        String cleanedJson = rawText.trim();
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7);
        } else if (cleanedJson.startsWith("```")) {
            cleanedJson = cleanedJson.substring(3);
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
        }
        cleanedJson = cleanedJson.trim();

        JsonNode summaryNode = objectMapper.readTree(cleanedJson);

        ChatSummaryDto dto = new ChatSummaryDto();
        dto.setConversationId(conversationId);
        dto.setConversationTitle(title);
        dto.setMessageCountAnalyzed(messageCount);
        dto.setGeneratedAt(Instant.now());
        dto.setAiModelUsed("Google Gemini (" + modelName + ")");
        dto.setRawSummary(cleanedJson);

        dto.setOverview(summaryNode.path("overview").asText("Analysis completed."));

        dto.setTopics(extractJsonStringList(summaryNode.path("topics")));
        dto.setKeyInformation(extractJsonStringList(summaryNode.path("keyInformation")));
        dto.setDecisions(extractJsonStringList(summaryNode.path("decisions")));
        dto.setActionItems(extractJsonStringList(summaryNode.path("actionItems")));
        dto.setDeadlines(extractJsonStringList(summaryNode.path("deadlines")));

        return dto;
    }

    private List<String> extractJsonStringList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                String str = item.asText();
                if (str != null && !str.trim().isEmpty()) {
                    list.add(str.trim());
                }
            }
        }
        return list;
    }

    /**
     * High-quality local NLP analyzer to provide instant, meaningful summaries from actual message content
     * when the external LLM key is pending configuration or network is constrained.
     */
    private ChatSummaryDto generateLocalSummary(Long conversationId, String title, List<Message> messages) {
        ChatSummaryDto dto = new ChatSummaryDto();
        dto.setConversationId(conversationId);
        dto.setConversationTitle(title);
        dto.setMessageCountAnalyzed(messages.size());
        dto.setGeneratedAt(Instant.now());
        dto.setAiModelUsed("Smart Heuristic NLP Engine (Configure GEMINI_API_KEY for Gemini AI)");

        Set<String> participants = messages.stream()
                .map(m -> m.getSender() != null ? m.getSender().getDisplayName() : "Unknown")
                .collect(Collectors.toSet());

        List<String> nonSystemTexts = messages.stream()
                .filter(m -> m.getType() != MessageType.SYSTEM && m.getContent() != null && !m.getContent().trim().isEmpty())
                .map(Message::getContent)
                .collect(Collectors.toList());

        // Extract decisions, action items, deadlines via keyword heuristics
        List<String> decisions = new ArrayList<>();
        List<String> actionItems = new ArrayList<>();
        List<String> deadlines = new ArrayList<>();
        List<String> keyInfo = new ArrayList<>();
        List<String> topics = new ArrayList<>();

        Pattern decisionPattern = Pattern.compile("(?i)(agreed|decided|approved|confirmed|let's go with|finalize|settled)");
        Pattern actionPattern = Pattern.compile("(?i)(i will|i'll|can you|please|we should|assigned|todo|task|action item|let us)");
        Pattern deadlinePattern = Pattern.compile("(?i)(by|before|deadline|due|tomorrow|today|friday|monday|tuesday|wednesday|thursday|saturday|sunday|next week|at \\d{1,2}|on \\d{1,2})");

        for (Message m : messages) {
            String text = m.getContent();
            if (text == null || text.trim().isEmpty()) continue;
            String sender = m.getSender() != null ? m.getSender().getDisplayName() : "User";

            if (decisionPattern.matcher(text).find()) {
                decisions.add(sender + ": \"" + text.trim() + "\"");
            }
            if (actionPattern.matcher(text).find()) {
                actionItems.add(sender + " committed: \"" + text.trim() + "\"");
            }
            if (deadlinePattern.matcher(text).find()) {
                deadlines.add("Mentioned by " + sender + ": \"" + text.trim() + "\"");
            }
            if (text.length() > 25 && keyInfo.size() < 5) {
                keyInfo.add(sender + " shared: " + text.trim());
            }
        }

        // Deduce general topics from participants and message themes
        topics.add("Discussion in " + title);
        if (participants.size() > 1) {
            topics.add("Collaboration between " + String.join(", ", participants));
        }
        if (!decisions.isEmpty()) {
            topics.add("Project Decisions & Alignment");
        }
        if (!actionItems.isEmpty()) {
            topics.add("Task Assignments & Next Steps");
        }

        if (decisions.isEmpty()) {
            decisions.add("Participants reached informal consensus on message threads.");
        }
        if (actionItems.isEmpty()) {
            actionItems.add("Continue active messaging in " + title + ".");
        }
        if (deadlines.isEmpty()) {
            deadlines.add("No explicit dates or deadlines detected in recent messages.");
        }
        if (keyInfo.isEmpty()) {
            keyInfo.add("Analyzed " + messages.size() + " messages exchanged across " + participants.size() + " participant(s).");
        }

        dto.setOverview(String.format("Conversation \"%s\" contains %d active messages between %s. %s",
                title, messages.size(), String.join(", ", participants),
                decisions.size() > 1 ? "Key agreements and action steps were identified." : "Ongoing discussion and communication."));

        dto.setTopics(topics);
        dto.setKeyInformation(keyInfo);
        dto.setDecisions(decisions);
        dto.setActionItems(actionItems);
        dto.setDeadlines(deadlines);

        return dto;
    }
}
