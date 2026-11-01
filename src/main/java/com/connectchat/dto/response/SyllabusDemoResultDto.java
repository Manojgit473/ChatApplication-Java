package com.connectchat.dto.response;

import java.time.Instant;
import java.util.Map;

/**
 * Educational DTO for showcasing college syllabus concepts
 * (Hibernate SessionFactory/Session/Transaction vs JPA EntityManager,
 * Reflection, Serialization, Generics & Streams).
 */
public class SyllabusDemoResultDto {

    private String topic;
    private String description;
    private long executionTimeNanos;
    private Instant executedAt;
    private Map<String, Object> details;

    public SyllabusDemoResultDto() {
        this.executedAt = Instant.now();
    }

    public SyllabusDemoResultDto(String topic, String description, long executionTimeNanos, Map<String, Object> details) {
        this.topic = topic;
        this.description = description;
        this.executionTimeNanos = executionTimeNanos;
        this.details = details;
        this.executedAt = Instant.now();
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public void setExecutionTimeNanos(long executionTimeNanos) {
        this.executionTimeNanos = executionTimeNanos;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
