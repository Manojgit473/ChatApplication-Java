package com.connectchat.controller;

import com.connectchat.dto.request.EditMessageRequest;
import com.connectchat.dto.response.ApiResponse;
import com.connectchat.dto.response.MessageDto;
import com.connectchat.security.SecurityUtils;
import com.connectchat.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for individual message manipulation (edit, delete, star, search).
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageDto>> editMessage(
            @PathVariable Long id,
            @Valid @RequestBody EditMessageRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        MessageDto updated = messageService.editMessage(id, currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Message edited successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        messageService.deleteMessage(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Message deleted successfully"));
    }

    @PostMapping("/{id}/star")
    public ResponseEntity<ApiResponse<Void>> toggleStarMessage(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        messageService.toggleStarMessage(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Message star toggled"));
    }

    @GetMapping("/starred")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getStarredMessages() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<MessageDto> list = messageService.getStarredMessages(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Starred messages", list));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MessageDto>>> searchMessages(
            @RequestParam Long conversationId,
            @RequestParam String keyword) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<MessageDto> results = messageService.searchMessages(conversationId, currentUserId, keyword);
        return ResponseEntity.ok(ApiResponse.success("Search results", results));
    }
}

