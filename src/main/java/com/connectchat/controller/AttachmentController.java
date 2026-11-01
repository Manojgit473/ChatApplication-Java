package com.connectchat.controller;

import com.connectchat.dto.response.ApiResponse;
import com.connectchat.dto.response.AttachmentDto;
import com.connectchat.entity.Attachment;
import com.connectchat.security.SecurityUtils;
import com.connectchat.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for uploading and downloading file attachments.
 */
@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<AttachmentDto>> uploadAttachment(@RequestParam("file") MultipartFile file) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        AttachmentDto attachment = attachmentService.uploadAttachment(file, currentUserId);
        return new ResponseEntity<>(ApiResponse.success("File uploaded successfully", attachment), HttpStatus.CREATED);
    }

    @GetMapping("/{storedFileName}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String storedFileName) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Resource resource = attachmentService.loadAttachmentResource(storedFileName, currentUserId);
        Attachment metadata = attachmentService.getAttachmentEntity(storedFileName);

        String contentType = metadata.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.getFileName() + "\"")
                .body(resource);
    }
}
