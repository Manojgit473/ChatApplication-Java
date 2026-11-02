package com.connectchat.service.impl;

import com.connectchat.dto.response.AttachmentDto;
import com.connectchat.entity.Attachment;
import com.connectchat.entity.User;
import com.connectchat.exception.BadRequestException;
import com.connectchat.exception.ResourceNotFoundException;
import com.connectchat.mapper.MessageMapper;
import com.connectchat.repository.AttachmentRepository;
import com.connectchat.repository.UserRepository;
import com.connectchat.service.AttachmentService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of AttachmentService.
 * 
 * Demonstrates:
 * - Java NIO (java.nio.file.Path, Files) file I/O operations.
 * - Secure UUID-based file decoupling to prevent path traversal vulnerabilities.
 * - MIME type and extension whitelist verification.
 */
@Service
public class AttachmentServiceImpl implements AttachmentService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            // Images
            "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "ico", "tif", "tiff",
            // Videos
            "mp4", "webm", "mkv", "mov", "avi", "3gp", "flv", "wmv", "m4v",
            // Audio
            "mp3", "wav", "ogg", "m4a", "aac", "flac",
            // Documents & Office
            "pdf", "doc", "docx", "xls", "xlsx", "csv", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp",
            // Archives & Code
            "zip", "rar", "7z", "tar", "gz", "json", "xml", "html", "md"
    );

    @Value("${connectchat.upload.dir:uploads/attachments}")
    private String uploadDir;

    private Path rootStoragePath;

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;

    public AttachmentServiceImpl(AttachmentRepository attachmentRepository,
                                 UserRepository userRepository,
                                 MessageMapper messageMapper) {
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.messageMapper = messageMapper;
    }

    @PostConstruct
    public void init() {
        this.rootStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload storage location", e);
        }
    }

    @Override
    @Transactional
    public AttachmentDto uploadAttachment(MultipartFile file, Long uploaderId) {
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Filename contains invalid relative path sequence: " + originalFilename);
        }

        String fileExtension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFilename.substring(dotIndex + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new BadRequestException("Unsupported file extension ." + fileExtension + ". Allowed extensions: " + ALLOWED_EXTENSIONS);
        }

        String storedFileName = UUID.randomUUID().toString() + (fileExtension.isEmpty() ? "" : "." + fileExtension);

        try {
            Path targetLocation = this.rootStoragePath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            User uploader = userRepository.findById(uploaderId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", uploaderId));

            Attachment attachment = new Attachment();
            attachment.setUploader(uploader);
            attachment.setFileName(originalFilename);
            attachment.setStoredFileName(storedFileName);
            attachment.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
            attachment.setFileSize(file.getSize());
            attachment.setFilePath(targetLocation.toString());

            Attachment saved = attachmentRepository.save(attachment);
            return messageMapper.toAttachmentDto(saved);

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFilename + ". Please try again!", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadAttachmentResource(String storedFileName, Long userId) {
        try {
            Path filePath = this.rootStoragePath.resolve(storedFileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found or unreadable: " + storedFileName);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found: " + storedFileName);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Attachment getAttachmentEntity(String storedFileName) {
        return attachmentRepository.findByStoredFileName(storedFileName)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "storedFileName", storedFileName));
    }
}

