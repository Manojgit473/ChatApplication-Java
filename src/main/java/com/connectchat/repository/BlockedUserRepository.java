package com.connectchat.repository;

import com.connectchat.entity.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for BlockedUser entity.
 * 
 * Demonstrates:
 * - Multi-condition JPQL boolean evaluation to enforce bidirectional block security policies.
 */
@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BlockedUser b " +
           "WHERE (b.blocker.id = :user1Id AND b.blocked.id = :user2Id) " +
           "   OR (b.blocker.id = :user2Id AND b.blocked.id = :user1Id)")
    boolean isBlockedEitherWay(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT b FROM BlockedUser b JOIN FETCH b.blocked WHERE b.blocker.id = :blockerId")
    List<BlockedUser> findByBlockerIdWithBlockedUser(@Param("blockerId") Long blockerId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}

