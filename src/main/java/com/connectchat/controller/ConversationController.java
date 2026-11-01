package com.connectchat.controller;

import com.connectchat.dto.request.CreateConversationRequest;
import com.connectchat.dto.request.SendMessageRequest;
import com.connectchat.dto.response.ApiResponse;
import com.connectchat.dto.response.ConversationDetailDto;
import com.connectchat.dto.response.ConversationSummaryDto;
import com.connectchat.dto.response.MessageDto;
import com.connectchat.security.SecurityUtils;
import com.connectchat.service.ConversationService;
import com.connectchat.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * REST Controller for Conversations and Message History.
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    public ConversationController(ConversationService conversationService, MessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationSummaryDto>>> getMyConversations() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<ConversationSummaryDto> list = conversationService.getUserConversations(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved", list));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationDetailDto>> createDirectConversation(@Valid @RequestBody CreateConversationRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ConversationDetailDto detail = conversationService.createDirectConversation(currentUserId, request);
        return new ResponseEntity<>(ApiResponse.success("Direct conversation ready", detail), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationDetailDto>> getConversationDetail(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ConversationDetailDto detail = conversationService.getConversationDetail(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Conversation detail", detail));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<Page<MessageDto>>> getConversationMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<MessageDto> messages = messageService.getMessages(id, currentUserId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved", messages));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        MessageDto message = messageService.sendMessage(id, currentUserId, request);
        return new ResponseEntity<>(ApiResponse.success("Message sent", message), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        conversationService.markConversationAsRead(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Conversation marked as read"));
    }

    @PostMapping("/{id}/mute")
    public ResponseEntity<ApiResponse<Void>> muteConversation(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean muted) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        conversationService.muteConversation(id, currentUserId, muted);
        return ResponseEntity.ok(ApiResponse.success("Mute status updated"));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveConversation(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean archived) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        conversationService.archiveConversation(id, currentUserId, archived);
        return ResponseEntity.ok(ApiResponse.success("Archive status updated"));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportConversationTranscript(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String transcript = messageService.exportConversationAsText(id, currentUserId);

        byte[] output = transcript.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "connectchat_conversation_" + id + ".txt");
        headers.setContentLength(output.length);

        return new ResponseEntity<>(output, headers, HttpStatus.OK);
    }
}
