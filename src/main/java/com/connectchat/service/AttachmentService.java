package com.connectchat.service;

import com.connectchat.dto.response.AttachmentDto;
import com.connectchat.entity.Attachment;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for secure file attachment uploads and downloads.
 */
public interface AttachmentService {

    AttachmentDto uploadAttachment(MultipartFile file, Long uploaderId);

    Resource loadAttachmentResource(String storedFileName, Long userId);

    Attachment getAttachmentEntity(String storedFileName);
}

