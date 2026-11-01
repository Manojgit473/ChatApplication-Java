package com.connectchat.repository;

import com.connectchat.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for ConversationMember entity.
 * 
 * Demonstrates:
 * - JOIN FETCH in JPQL to solve the N+1 select problem when eagerly retrieving members and user profiles.
 */
@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);

    @Query("SELECT m FROM ConversationMember m JOIN FETCH m.user WHERE m.conversation.id = :conversationId")
    List<ConversationMember> findByConversationIdWithUser(@Param("conversationId") Long conversationId);

    List<ConversationMember> findByConversationId(Long conversationId);

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    long countByConversationId(Long conversationId);

    void deleteByConversationIdAndUserId(Long conversationId, Long userId);
}
