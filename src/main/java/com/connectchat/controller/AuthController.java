package com.connectchat.controller;

import com.connectchat.dto.request.LoginRequest;
import com.connectchat.dto.request.RegisterRequest;
import com.connectchat.dto.response.ApiResponse;
import com.connectchat.dto.response.UserProfileDto;
import com.connectchat.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for User Authentication operations.
 * 
 * Demonstrates:
 * - Spring @RestController returning JSON responses.
 * - @Valid annotation enforcing Jakarta Bean Validation rules.
 * - HTTP status codes (201 Created, 200 OK).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfileDto>> register(@Valid @RequestBody RegisterRequest request) {
        UserProfileDto profile = authService.register(request);
        return new ResponseEntity<>(ApiResponse.success("User registered successfully", profile), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserProfileDto>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        UserProfileDto profile = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", profile));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUser() {
        UserProfileDto profile = authService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved", profile));
    }
}

