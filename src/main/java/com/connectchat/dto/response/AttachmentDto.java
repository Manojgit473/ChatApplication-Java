package com.connectchat.dto.response;

import java.time.Instant;

/**
 * DTO for file attachments.
 */
public class AttachmentDto {

    private Long id;
    private String fileName;
    private String storedFileName;
    private String contentType;
    private Long fileSize;
    private String downloadUrl;
    private Instant createdAt;

    public AttachmentDto() {
    }

    public AttachmentDto(Long id, String fileName, String storedFileName, String contentType, Long fileSize, String downloadUrl, Instant createdAt) {
        this.id = id;
        this.fileName = fileName;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.downloadUrl = downloadUrl;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
