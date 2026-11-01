package com.connectchat.repository;

import com.connectchat.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Attachment entity.
 */
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    Optional<Attachment> findByStoredFileName(String storedFileName);

    List<Attachment> findByMessageId(Long messageId);

    List<Attachment> findByMessageConversationIdOrderByCreatedAtDesc(Long conversationId);
}

