package com.connectchat.repository;

import com.connectchat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Message entity.
 * 
 * Demonstrates:
 * - Pagination via org.springframework.data.domain.Pageable for scalable chat history loading.
 * - JPQL projection, aggregation, and keyword searching.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m " +
           "LEFT JOIN FETCH m.sender " +
           "LEFT JOIN FETCH m.replyTo " +
           "WHERE m.conversation.id = :conversationId " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findByConversationIdOrderByCreatedAtDesc(@Param("conversationId") Long conversationId, Pageable pageable);

    Optional<Message> findByConversationIdAndIdempotencyKey(Long conversationId, String idempotencyKey);

    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);

    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND (:lastReadId IS NULL OR m.id > :lastReadId) " +
           "AND m.sender.id != :userId " +
           "AND m.deleted = false")
    long countUnreadMessages(@Param("conversationId") Long conversationId, 
                             @Param("lastReadId") Long lastReadId, 
                             @Param("userId") Long userId);

    @Query("SELECT m FROM Message m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.deleted = false " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY m.createdAt DESC")
    List<Message> searchMessages(@Param("conversationId") Long conversationId, @Param("keyword") String keyword);

    @Query("SELECT m FROM Message m " +
           "JOIN m.starredByUsers u " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE u.id = :userId " +
           "ORDER BY m.createdAt DESC")
    List<Message> findStarredMessagesByUser(@Param("userId") Long userId);
}

