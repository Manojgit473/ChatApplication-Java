package com.connectchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO to update group details.
 */
public class UpdateGroupRequest {

    @NotBlank(message = "Group title cannot be blank")
    @Size(min = 2, max = 100, message = "Group title must be between 2 and 100 characters")
    private String title;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Size(max = 255, message = "Avatar URL cannot exceed 255 characters")
    private String groupAvatarUrl;

    public UpdateGroupRequest() {
    }

    public UpdateGroupRequest(String title, String description, String groupAvatarUrl) {
        this.title = title;
        this.description = description;
        this.groupAvatarUrl = groupAvatarUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroupAvatarUrl() {
        return groupAvatarUrl;
    }

    public void setGroupAvatarUrl(String groupAvatarUrl) {
        this.groupAvatarUrl = groupAvatarUrl;
    }
}

