package com.connectchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for editing an existing message.
 */
public class EditMessageRequest {

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 5000, message = "Message content cannot exceed 5000 characters")
    private String content;

    public EditMessageRequest() {
    }

    public EditMessageRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

