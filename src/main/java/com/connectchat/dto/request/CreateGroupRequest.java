package com.connectchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO to create a new group conversation.
 */
public class CreateGroupRequest {

    @NotBlank(message = "Group title is required")
    @Size(min = 2, max = 100, message = "Group title must be between 2 and 100 characters")
    private String title;

    @Size(max = 255, message = "Group description cannot exceed 255 characters")
    private String description;

    private Set<Long> memberIds = new HashSet<>();

    public CreateGroupRequest() {
    }

    public CreateGroupRequest(String title, String description, Set<Long> memberIds) {
        this.title = title;
        this.description = description;
        this.memberIds = memberIds != null ? memberIds : new HashSet<>();
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

    public Set<Long> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(Set<Long> memberIds) {
        this.memberIds = memberIds;
    }
}

