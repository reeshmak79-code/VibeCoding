-- Run this in H2 Console to fix the constraint issue
-- Access: http://localhost:8080/h2-console

-- Drop the problematic constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS CONSTRAINT_4;

-- Verify the constraint is gone
SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME = 'USERS';
