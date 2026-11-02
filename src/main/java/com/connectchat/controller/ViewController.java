package com.connectchat.controller;

import com.connectchat.dto.response.UserProfileDto;
import com.connectchat.security.CustomUserDetails;
import com.connectchat.service.AuthService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Spring MVC View Controller rendering Thymeleaf pages.
 * 
 * Demonstrates:
 * - Spring MVC Model-View-Controller architecture.
 * - Server-side page routing and Model data passing.
 */
@Controller
public class ViewController {

    private final AuthService authService;

    public ViewController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/chat";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/chat")
    public String chatPage(Model model) {
        if (isAuthenticated()) {
            try {
                UserProfileDto profile = authService.getCurrentUserProfile();
                model.addAttribute("currentUser", profile);
            } catch (Exception e) {
                model.addAttribute("currentUser", null);
            }
        } else {
            model.addAttribute("currentUser", null);
        }
        return "chat";
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}

