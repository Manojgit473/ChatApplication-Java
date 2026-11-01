package com.connectchat.service;

import com.connectchat.dto.request.LoginRequest;
import com.connectchat.dto.request.RegisterRequest;
import com.connectchat.dto.response.UserProfileDto;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for user registration, authentication, and session initialization.
 */
public interface AuthService {

    UserProfileDto register(RegisterRequest request);

    UserProfileDto login(LoginRequest request, HttpServletRequest httpRequest);

    UserProfileDto getCurrentUserProfile();
}
