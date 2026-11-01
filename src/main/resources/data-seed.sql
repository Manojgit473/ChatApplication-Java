-- ========================================================
-- Connect Chat: Demo Seed Data
-- Note: Password for all seeded users is: Password@123
-- BCrypt Hash: $2a$10$3zHS2W0E4e4mOqT/d44B9uNl8/1w3qC3y1C5wU5r.64Gv7LhWdOma
-- ========================================================

-- Seed Sample Users
INSERT INTO users (id, created_at, updated_at, username, email, password, display_name, avatar_url, bio, online_status, enabled)
VALUES 
(1, NOW(6), NOW(6), 'alex', 'alex@example.com', '$2a$10$595r2E0Q4M2UfE5qP10gceFv8zT/c181h7J6KzL4yLwVlB1pC98eG', 'Alex Morgan', NULL, 'Full Stack Engineer & Open Source enthusiast', 'ONLINE', TRUE),
(2, NOW(6), NOW(6), 'samira', 'samira@example.com', '$2a$10$595r2E0Q4M2UfE5qP10gceFv8zT/c181h7J6KzL4yLwVlB1pC98eG', 'Samira Khan', NULL, 'UI/UX Designer | Clean interfaces & soft palettes', 'AWAY', TRUE),
(3, NOW(6), NOW(6), 'jordan', 'jordan@example.com', '$2a$10$595r2E0Q4M2UfE5qP10gceFv8zT/c181h7J6KzL4yLwVlB1pC98eG', 'Jordan Lee', NULL, 'Backend Developer & Distributed Systems researcher', 'OFFLINE', TRUE),
(4, NOW(6), NOW(6), 'elena', 'elena@example.com', '$2a$10$595r2E0Q4M2UfE5qP10gceFv8zT/c181h7J6KzL4yLwVlB1pC98eG', 'Elena Rostova', NULL, 'DevOps & Cloud architecture apprentice', 'OFFLINE', TRUE)
ON DUPLICATE KEY UPDATE updated_at = NOW(6);

-- Seed Sample Direct Conversation between Alex and Samira
INSERT INTO conversations (id, created_at, updated_at, type, title, created_by_id, last_message_at)
VALUES 
(1, NOW(6), NOW(6), 'DIRECT', NULL, 1, NOW(6))
ON DUPLICATE KEY UPDATE updated_at = NOW(6);

INSERT INTO conversation_members (id, created_at, updated_at, conversation_id, user_id, role, joined_at)
VALUES 
(1, NOW(6), NOW(6), 1, 1, 'MEMBER', NOW(6)),
(2, NOW(6), NOW(6), 1, 2, 'MEMBER', NOW(6))
ON DUPLICATE KEY UPDATE updated_at = NOW(6);

-- Seed Sample Group Conversation: "CS 2026 Project Squad"
INSERT INTO conversations (id, created_at, updated_at, type, title, description, created_by_id, last_message_at)
VALUES 
(2, NOW(6), NOW(6), 'GROUP', 'CS 2026 Project Squad', 'Collaboration channel for the final web programming viva', 1, NOW(6))
ON DUPLICATE KEY UPDATE updated_at = NOW(6);

INSERT INTO conversation_members (id, created_at, updated_at, conversation_id, user_id, role, joined_at)
VALUES 
(3, NOW(6), NOW(6), 2, 1, 'OWNER', NOW(6)),
(4, NOW(6), NOW(6), 2, 2, 'ADMIN', NOW(6)),
(5, NOW(6), NOW(6), 2, 3, 'MEMBER', NOW(6))
ON DUPLICATE KEY UPDATE updated_at = NOW(6);

-- Seed Initial Messages
INSERT INTO messages (id, created_at, updated_at, conversation_id, sender_id, content, type, status)
VALUES 
(1, DATE_SUB(NOW(6), INTERVAL 30 MINUTE), DATE_SUB(NOW(6), INTERVAL 30 MINUTE), 1, 1, 'Hi Samira, welcome to Connect Chat! How does the new soft teal interface look?', 'TEXT', 'READ'),
(2, DATE_SUB(NOW(6), INTERVAL 25 MINUTE), DATE_SUB(NOW(6), INTERVAL 25 MINUTE), 1, 2, 'Hey Alex! It looks wonderfully crisp and modern. The typography and responsive spacing are spot on.', 'TEXT', 'READ'),
(3, DATE_SUB(NOW(6), INTERVAL 15 MINUTE), DATE_SUB(NOW(6), INTERVAL 15 MINUTE), 2, 1, 'Team, our Spring Boot + Hibernate real-time chat application architecture is ready for demonstration.', 'TEXT', 'DELIVERED'),
(4, DATE_SUB(NOW(6), INTERVAL 10 MINUTE), DATE_SUB(NOW(6), INTERVAL 10 MINUTE), 2, 2, 'Awesome! I verified the WebSocket STOMP real-time pub-sub channels and presence indicators.', 'TEXT', 'DELIVERED')
ON DUPLICATE KEY UPDATE updated_at = NOW(6);
