package com.connectchat.repository;

import com.connectchat.entity.MessageReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for MessageReceipt entity.
 */
@Repository
public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, Long> {

    Optional<MessageReceipt> findByMessageIdAndUserId(Long messageId, Long userId);

    List<MessageReceipt> findByMessageId(Long messageId);
}

