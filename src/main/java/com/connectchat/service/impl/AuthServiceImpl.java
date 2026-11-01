package com.connectchat.service.impl;

import com.connectchat.dto.request.LoginRequest;
import com.connectchat.dto.request.RegisterRequest;
import com.connectchat.dto.response.UserProfileDto;
import com.connectchat.entity.User;
import com.connectchat.enums.OnlineStatus;
import com.connectchat.exception.BadRequestException;
import com.connectchat.exception.ConflictException;
import com.connectchat.exception.ResourceNotFoundException;
import com.connectchat.mapper.UserMapper;
import com.connectchat.repository.UserRepository;
import com.connectchat.security.CustomUserDetails;
import com.connectchat.security.SecurityUtils;
import com.connectchat.service.AuthService;
import com.connectchat.service.PresenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AuthService.
 * 
 * Demonstrates:
 * - Spring Dependency Injection via constructor.
 * - Declarative transactions (@Transactional).
 * - BCrypt password hashing before persistence.
 * - Programmatic Spring Security Authentication and session fixation protection.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final PresenceService presenceService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           UserMapper userMapper,
                           PresenceService presenceService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
        this.presenceService = presenceService;
    }

    @Override
    @Transactional
    public UserProfileDto register(RegisterRequest request) {
        String trimmedUsername = request.getUsername().trim().toLowerCase();
        String trimmedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new ConflictException("Username '" + trimmedUsername + "' is already taken");
        }

        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new ConflictException("Email '" + trimmedEmail + "' is already registered");
        }

        User user = new User();
        user.setUsername(trimmedUsername);
        user.setEmail(trimmedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName().trim());
        user.setBio("Hey there! I am using Connect Chat.");
        user.setOnlineStatus(OnlineStatus.ONLINE);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        return userMapper.toProfileDto(savedUser);
    }

    @Override
    @Transactional
    public UserProfileDto login(LoginRequest request, HttpServletRequest httpRequest) {
        String identifier = request.getUsernameOrEmail().trim();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, request.getPassword())
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Persist SecurityContext into HTTP Session for stateful web flow
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        presenceService.setUserOnlineStatus(userDetails.getId(), OnlineStatus.ONLINE);

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        return userMapper.toProfileDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));
        return userMapper.toProfileDto(user);
    }
}
