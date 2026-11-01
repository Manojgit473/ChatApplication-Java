package com.connectchat.controller;

import com.connectchat.dto.request.PasswordChangeRequest;
import com.connectchat.dto.request.ProfileUpdateRequest;
import com.connectchat.dto.response.ApiResponse;
import com.connectchat.dto.response.UserProfileDto;
import com.connectchat.dto.response.UserSummaryDto;
import com.connectchat.security.SecurityUtils;
import com.connectchat.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for User profile management, search, and blocking.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> getMyProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileDto profile = userService.getUserProfile(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", profile));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateMyProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileDto updated = userService.updateProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PostMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userService.changePassword(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> searchUsers(@RequestParam(required = false, defaultValue = "") String q) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<UserSummaryDto> results = userService.searchUsers(q, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("User search results", results));
    }

    @GetMapping("/contacts")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getAllContacts() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<UserSummaryDto> contacts = userService.getAllContacts(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Contacts list", contacts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileDto>> getUserById(@PathVariable Long id) {
        UserProfileDto profile = userService.getUserProfile(id);
        return ResponseEntity.ok(ApiResponse.success("User profile", profile));
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userService.blockUser(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully"));
    }

    @DeleteMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userService.unblockUser(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully"));
    }

    @GetMapping("/me/blocked")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getBlockedUsers() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<UserSummaryDto> blocked = userService.getBlockedUsers(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Blocked users list", blocked));
    }
}
