package com.connectchat.service.impl;

import com.connectchat.dto.request.LoginRequest;
import com.connectchat.dto.request.RegisterRequest;
import com.connectchat.dto.response.UserProfileDto;
import com.connectchat.entity.User;
import com.connectchat.enums.OnlineStatus;
import com.connectchat.exception.BadRequestException;
import com.connectchat.exception.ConflictException;
import com.connectchat.exception.UnauthorizedException;
import com.connectchat.mapper.UserMapper;
import com.connectchat.repository.UserRepository;
import com.connectchat.security.CustomUserDetails;
import com.connectchat.security.SecurityUtils;
import com.connectchat.security.SessionTokenService;
import com.connectchat.service.AuthService;
import com.connectchat.service.PresenceService;
import com.connectchat.util.PhoneNumberUtils;
import com.connectchat.util.StringValidationUtils;
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
 * - Simplified registration with Name, Mobile Number, Email, Password.
 * - Login using Email, Mobile Number, or Username.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final PresenceService presenceService;
    private final SessionTokenService sessionTokenService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           UserMapper userMapper,
                           PresenceService presenceService,
                           SessionTokenService sessionTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
        this.presenceService = presenceService;
        this.sessionTokenService = sessionTokenService;
    }

    @Override
    @Transactional
    public UserProfileDto register(RegisterRequest request) {
        String trimmedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (trimmedEmail.isEmpty() || !StringValidationUtils.isValidEmail(trimmedEmail)) {
            throw new BadRequestException("Please enter a valid email address (e.g. name@domain.com)");
        }

        String rawPhone = request.getPhoneNumber();
        String tenDigits = PhoneNumberUtils.extract10Digit(rawPhone);
        if (tenDigits == null || !tenDigits.matches("^[6-9]\\d{9}$")) {
            throw new BadRequestException("Mobile number must be a valid 10-digit Indian number starting with 6, 7, 8, or 9");
        }

        String passwordError = StringValidationUtils.validatePasswordStrength(request.getPassword());
        if (passwordError != null) {
            throw new BadRequestException(passwordError);
        }

        String normalizedPhone = PhoneNumberUtils.normalizeIndianPhoneNumber(rawPhone);
        String explicitUsername = (request.getUsername() != null && !request.getUsername().trim().isEmpty())
                ? request.getUsername().trim().toLowerCase()
                : null;

        String finalUsername;
        if (explicitUsername != null) {
            if (userRepository.existsByUsername(explicitUsername)) {
                throw new ConflictException("Username '" + explicitUsername + "' is already taken");
            }
            finalUsername = explicitUsername;
        } else {
            String base = (tenDigits != null && !tenDigits.isEmpty())
                    ? tenDigits
                    : trimmedEmail.split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "");
            if (base.isEmpty()) {
                base = "user";
            }
            finalUsername = base;
            int counter = 1;
            while (userRepository.existsByUsername(finalUsername)) {
                finalUsername = base + counter++;
            }
        }

        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new ConflictException("Email '" + trimmedEmail + "' is already registered");
        }

        if (normalizedPhone != null && !normalizedPhone.isEmpty()) {
            boolean phoneExists = userRepository.existsByPhoneNumber(normalizedPhone)
                    || (tenDigits != null && (userRepository.existsByPhoneNumber(tenDigits)
                                           || userRepository.existsByPhoneNumber("+91 " + tenDigits)
                                           || userRepository.existsByPhoneNumber("+91" + tenDigits)));
            if (phoneExists) {
                throw new ConflictException("Mobile number '" + (tenDigits != null ? tenDigits : normalizedPhone) + "' is already registered");
            }
        }

        User user = new User();
        user.setUsername(finalUsername);
        user.setEmail(trimmedEmail);
        user.setPhoneNumber(normalizedPhone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName().trim() : finalUsername);
        user.setBio("Hey there! I am using Connect Chat.");
        user.setOnlineStatus(OnlineStatus.ONLINE);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        UserProfileDto profile = userMapper.toProfileDto(savedUser);
        profile.setToken(sessionTokenService.generateToken(savedUser.getId()));
        return profile;
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
                .orElseThrow(() -> new UnauthorizedException("User session is invalid"));

        UserProfileDto profile = userMapper.toProfileDto(user);
        profile.setToken(sessionTokenService.generateToken(user.getId()));
        return profile;
    }

    @Override
    public void logout(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        String tokenHeader = httpRequest.getHeader("X-Auth-Token");
        String rawToken = (authHeader != null && authHeader.startsWith("Bearer ")) 
                ? authHeader.substring(7).trim() 
                : (tokenHeader != null ? tokenHeader.trim() : null);
        if (rawToken != null) {
            sessionTokenService.revokeToken(rawToken);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            presenceService.setUserOnlineStatus(userDetails.getId(), OnlineStatus.OFFLINE);
        }

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("No active authenticated session");
        }
        if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new UnauthorizedException("User profile not found"));
            UserProfileDto profile = userMapper.toProfileDto(user);
            profile.setToken(sessionTokenService.generateToken(user.getId()));
            return profile;
        }
        throw new UnauthorizedException("Authentication principal is invalid");
    }
}
