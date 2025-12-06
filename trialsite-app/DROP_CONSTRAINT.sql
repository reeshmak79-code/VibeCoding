-- ============================================
-- FIX PASSWORD CONSTRAINT ERROR
-- ============================================
-- Run this in H2 Console: http://localhost:8080/h2-console
-- 
-- Steps:
-- 1. Open http://localhost:8080/h2-console
-- 2. JDBC URL: jdbc:h2:file:./data/trialsite
-- 3. Username: sa
-- 4. Password: (leave empty)
-- 5. Click Connect
-- 6. Copy and paste the SQL below
-- 7. Click Run
-- 8. Restart your backend
-- ============================================

-- Drop the problematic constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS CONSTRAINT_4;

-- Verify it's gone (should return 0 rows)
SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_NAME = 'USERS' AND CONSTRAINT_NAME = 'CONSTRAINT_4';

-- If the above doesn't work, try this (drops all check constraints on users table):
-- SELECT 'ALTER TABLE users DROP CONSTRAINT ' || CONSTRAINT_NAME || ';' 
-- FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
-- WHERE TABLE_NAME = 'USERS' AND CONSTRAINT_TYPE = 'CHECK';
