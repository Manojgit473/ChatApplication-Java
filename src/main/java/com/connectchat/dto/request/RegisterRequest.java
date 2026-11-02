package com.connectchat.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for new user registration requests.
 * 
 * Supports simplified registration: Name, Mobile Number, Email, Password.
 */
public class RegisterRequest {

    // Username is optional; if omitted, backend generates it from email or mobile number
    @Size(max = 50, message = "Username cannot exceed 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a well-formed email address")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Please enter a valid email address (e.g. name@domain.com)")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^(?:\\+91[\\-\\s]?)?[6-9]\\d{9}$", message = "Mobile number must be a valid 10-digit Indian number starting with 6, 7, 8, or 9")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`]).{8,100}$",
             message = "Password must be at least 8 characters and contain at least one uppercase letter (A-Z), one lowercase letter (a-z), one number (0-9), and one special character")
    private String password;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String displayName;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String email, String password, String displayName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    public RegisterRequest(String username, String email, String password, String displayName, String phoneNumber) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // Alias for mobileNumber in JSON
    public String getMobileNumber() {
        return phoneNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.phoneNumber = mobileNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    // Alias for name in JSON
    public String getName() {
        return displayName;
    }

    public void setName(String name) {
        this.displayName = name;
    }
}
