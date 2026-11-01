package com.connectchat.controller;

import com.connectchat.dto.request.AddGroupMemberRequest;
import com.connectchat.dto.request.CreateGroupRequest;
import com.connectchat.dto.request.UpdateGroupRequest;
import com.connectchat.dto.response.ApiResponse;
import com.connectchat.dto.response.ConversationDetailDto;
import com.connectchat.security.SecurityUtils;
import com.connectchat.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Group Chat lifecycle and role-based membership administration.
 */
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationDetailDto>> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ConversationDetailDto group = groupService.createGroup(currentUserId, request);
        return new ResponseEntity<>(ApiResponse.success("Group created successfully", group), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationDetailDto>> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGroupRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ConversationDetailDto updated = groupService.updateGroup(id, currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Group updated successfully", updated));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddGroupMemberRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        groupService.addGroupMember(id, currentUserId, request.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Member added to group"));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        groupService.removeGroupMember(id, currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("Member removed from group"));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        groupService.leaveGroup(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Left group successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        groupService.deleteGroup(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Group deleted successfully"));
    }
}
