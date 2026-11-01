package com.connectchat.service;

import com.connectchat.dto.request.PasswordChangeRequest;
import com.connectchat.entity.User;
import com.connectchat.exception.BadRequestException;
import com.connectchat.mapper.UserMapper;
import com.connectchat.repository.BlockedUserRepository;
import com.connectchat.repository.UserRepository;
import com.connectchat.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void changePassword_WhenCurrentPasswordIncorrect_ShouldThrowBadRequest() {
        User user = new User();
        user.setId(1L);
        user.setPassword("correct_hash");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "correct_hash")).thenReturn(false);

        PasswordChangeRequest request = new PasswordChangeRequest("wrong_password", "new_secret_123");

        assertThrows(BadRequestException.class, () -> userService.changePassword(1L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void blockUser_WhenBlockingSelf_ShouldThrowBadRequest() {
        assertThrows(BadRequestException.class, () -> userService.blockUser(1L, 1L));
    }
}

