-- ========================================================
-- Connect Chat: Flyway V2 Migration
-- Add Self-Destructing / Expiring Message Columns
-- ========================================================

DROP PROCEDURE IF EXISTS AddSelfDestructColumns;
DELIMITER //
CREATE PROCEDURE AddSelfDestructColumns()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'messages' 
        AND COLUMN_NAME = 'expires_at'
    ) THEN
        ALTER TABLE messages ADD COLUMN expires_at TIMESTAMP(6) NULL;
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'messages' 
        AND COLUMN_NAME = 'is_expired'
    ) THEN
        ALTER TABLE messages ADD COLUMN is_expired BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'messages' 
        AND COLUMN_NAME = 'expiry_duration'
    ) THEN
        ALTER TABLE messages ADD COLUMN expiry_duration VARCHAR(30) NULL;
    END IF;
END//
DELIMITER ;
CALL AddSelfDestructColumns();
DROP PROCEDURE IF EXISTS AddSelfDestructColumns;
