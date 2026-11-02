package com.connectchat.security;

import com.connectchat.entity.User;
import com.connectchat.repository.UserRepository;
import com.connectchat.util.PhoneNumberUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Custom UserDetailsService implementation allowing login with:
 * - 10-digit Indian Mobile Number (without requiring +91)
 * - Email Address
 * - Username
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new UsernameNotFoundException("Identifier cannot be empty");
        }

        String clean = identifier.trim();
        String tenDigits = PhoneNumberUtils.extract10Digit(clean);

        // 1. If 10-digit mobile number, resolve with and without +91 prefix
        if (tenDigits != null) {
            Optional<User> byPhone = userRepository.findByPhoneNumber("+91 " + tenDigits)
                    .or(() -> userRepository.findByPhoneNumber("+91" + tenDigits))
                    .or(() -> userRepository.findByPhoneNumber(tenDigits))
                    .or(() -> userRepository.findByFlexiblePhoneNumber(tenDigits));
            if (byPhone.isPresent()) {
                return new CustomUserDetails(byPhone.get());
            }
        }

        // 2. Email login
        Optional<User> byEmail = userRepository.findByEmail(clean.toLowerCase());
        if (byEmail.isPresent()) {
            return new CustomUserDetails(byEmail.get());
        }

        // 3. Exact phone number (international numbers, etc.)
        Optional<User> byExactPhone = userRepository.findByPhoneNumber(clean)
                .or(() -> userRepository.findByFlexiblePhoneNumber(clean));
        if (byExactPhone.isPresent()) {
            return new CustomUserDetails(byExactPhone.get());
        }

        // 4. Username login
        User user = userRepository.findByUsername(clean.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with mobile number, email, or username: " + identifier));

        return new CustomUserDetails(user);
    }
}
