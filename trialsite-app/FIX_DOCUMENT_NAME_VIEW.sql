-- ============================================
-- FIX DOCUMENT_NAME IN PERMISSIONS_WITH_NAMES VIEW
-- Run in H2 Console: http://localhost:8080/h2-console
-- ============================================

-- This view shows documents when permission is on document OR shows documents in folders when permission is on folder
DROP VIEW IF EXISTS permissions_with_names;

CREATE OR REPLACE VIEW permissions_with_names AS
SELECT 
    dp.id,
    dp.permission_type,
    dp.granted_by,
    dp.granted_at,
    -- User information
    dp.user_id,
    u.full_name AS user_full_name,
    u.email AS user_email,
    -- Role information
    CAST(COALESCE(CAST(u.role AS VARCHAR), CAST(dp.role_name AS VARCHAR)) AS VARCHAR) AS role_name,
    -- Folder information
    dp.folder_id,
    f.folder_name,
    -- Document information: 
    -- If permission is on document, show that document
    -- If permission is on folder, show documents IN that folder
    COALESCE(dp.document_id, d_in_folder.id) AS document_id,
    COALESCE(d.original_file_name, d_in_folder.original_file_name) AS document_name
FROM document_permissions dp
LEFT JOIN users u ON dp.user_id = u.id
LEFT JOIN folders f ON dp.folder_id = f.id
LEFT JOIN documents d ON dp.document_id = d.id  -- Document permission
LEFT JOIN documents d_in_folder ON dp.folder_id IS NOT NULL AND dp.document_id IS NULL AND d_in_folder.folder_id = dp.folder_id  -- Documents in folder
ORDER BY dp.id, document_name;

-- Test it
SELECT * FROM permissions_with_names;

-- Alternative: If you want ONE row per permission with comma-separated document names
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
    CAST(COALESCE(CAST(u.role AS VARCHAR), CAST(dp.role_name AS VARCHAR)) AS VARCHAR) AS role_name,
    dp.folder_id,
    f.folder_name,
    dp.document_id,
    -- Show document name: direct permission OR first document in folder OR comma-separated list
    CASE 
        WHEN dp.document_id IS NOT NULL THEN d.original_file_name
        WHEN dp.folder_id IS NOT NULL THEN (
            SELECT STRING_AGG(original_file_name, ', ')
            FROM documents
            WHERE folder_id = dp.folder_id
        )
        ELSE NULL
    END AS document_name
FROM document_permissions dp
LEFT JOIN users u ON dp.user_id = u.id
LEFT JOIN folders f ON dp.folder_id = f.id
LEFT JOIN documents d ON dp.document_id = d.id;

-- Test it
SELECT * FROM permissions_with_names;
