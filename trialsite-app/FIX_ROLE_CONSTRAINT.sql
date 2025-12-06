-- ============================================
-- FIX ROLE_NAME CONSTRAINT ISSUE
-- Run in H2 Console: http://localhost:8080/h2-console
-- ============================================

-- The role_name column in document_permissions has a CHECK constraint
-- that only allows 'ADMIN' or 'USER', but users can have other roles (AUDITOR, DOCTOR, etc.)
-- We need to drop this constraint or update it

-- Step 1: Find the constraint name
SELECT 
    CONSTRAINT_NAME,
    CHECK_EXPRESSION
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc ON tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
WHERE tc.TABLE_NAME = 'DOCUMENT_PERMISSIONS'
AND tc.CONSTRAINT_TYPE = 'CHECK';

-- Step 2: Drop the constraint (replace CONSTRAINT_NAME with actual name from Step 1)
-- ALTER TABLE document_permissions DROP CONSTRAINT IF EXISTS CONSTRAINT_NAME;

-- Step 3: Recreate the view with CAST to avoid constraint issues
DROP VIEW IF EXISTS permissions_with_names;

CREATE OR REPLACE VIEW permissions_with_names AS
SELECT 
    dp.id,
    dp.permission_type,
    dp.granted_by,
    dp.granted_at,
    dp.user_id,
    u.full_name AS user_full_name,
    u.email AS user_email,
    -- Cast to VARCHAR to avoid CHECK constraint issues
    CAST(COALESCE(CAST(u.role AS VARCHAR), CAST(dp.role_name AS VARCHAR)) AS VARCHAR) AS role_name,
    dp.folder_id,
    f.folder_name,
    -- Document information (shows original file name)
    dp.document_id,
    d.original_file_name AS document_name
FROM document_permissions dp
LEFT JOIN users u ON dp.user_id = u.id
LEFT JOIN folders f ON dp.folder_id = f.id
LEFT JOIN documents d ON dp.document_id = d.id;

-- Step 4: Test the view
SELECT * FROM permissions_with_names;
