package com.connectchat.service;

import com.connectchat.dto.request.RegisterRequest;
import com.connectchat.dto.response.UserProfileDto;
import com.connectchat.entity.User;
import com.connectchat.exception.BadRequestException;
import com.connectchat.exception.ConflictException;
import com.connectchat.mapper.UserMapper;
import com.connectchat.repository.UserRepository;
import com.connectchat.security.SessionTokenService;
import com.connectchat.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PresenceService presenceService;

    @Mock
    private SessionTokenService sessionTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("testuser", "test@example.com", "Password@123", "Test User", "9876543210");
    }

    @Test
    void register_WhenValidRequest_ShouldSaveAndReturnUser() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setUsername("testuser");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserProfileDto profileDto = new UserProfileDto();
        profileDto.setId(10L);
        profileDto.setUsername("testuser");
        when(userMapper.toProfileDto(any(User.class))).thenReturn(profileDto);

        UserProfileDto result = authService.register(registerRequest);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_WhenDuplicateUsername_ShouldThrowConflictException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_WhenDuplicateEmail_ShouldThrowConflictException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_WhenDuplicatePhone_ShouldThrowConflictException() {
        registerRequest.setPhoneNumber("9876543210");
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+91 9876543210")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_WhenInvalidEmail_ShouldThrowBadRequestException() {
        registerRequest.setEmail("invalid-email-format");
        assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
    }

    @Test
    void register_WhenInvalidIndianPhone_ShouldThrowBadRequestException() {
        registerRequest.setPhoneNumber("12345"); // invalid length & series
        assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
    }

    @Test
    void register_WhenWeakPassword_ShouldThrowBadRequestException() {
        registerRequest.setPassword("simple"); // no upper, no digits, no symbols, <8
        assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
    }
}

