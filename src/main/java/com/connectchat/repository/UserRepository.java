package com.connectchat.repository;

import com.connectchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for User entity.
 * 
 * Demonstrates:
 * - Query derivation mechanism based on method names.
 * - Named parameter JPQL queries with :param binding for SQL injection prevention.
 * - Multi-criteria search by Name, Mobile Number, Email, and Username.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u WHERE u.phoneNumber = :phone " +
           "OR u.phoneNumber = CONCAT('+91 ', :phone) " +
           "OR u.phoneNumber = CONCAT('+91', :phone) " +
           "OR (u.phoneNumber IS NOT NULL AND u.phoneNumber LIKE CONCAT('%', :phone))")
    Optional<User> findByFlexiblePhoneNumber(@Param("phone") String phone);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u WHERE u.id != :currentUserId AND u.enabled = true AND (" +
           "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "(u.phoneNumber IS NOT NULL AND u.phoneNumber LIKE CONCAT('%', :query, '%'))) " +
           "ORDER BY u.displayName ASC")
    List<User> searchUsers(@Param("query") String query, @Param("currentUserId") Long currentUserId);

    @Query("SELECT u FROM User u WHERE u.id != :currentUserId AND u.enabled = true ORDER BY u.displayName ASC")
    List<User> findAllExceptCurrent(@Param("currentUserId") Long currentUserId);
}
