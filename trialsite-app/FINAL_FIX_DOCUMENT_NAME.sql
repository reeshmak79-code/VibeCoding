-- ============================================
-- FINAL FIX: Show document names in permissions_with_names
-- Run in H2 Console: http://localhost:8080/h2-console
-- ============================================

-- This view will show:
-- 1. Document name when permission is directly on a document
-- 2. Documents inside folders when permission is on a folder (one row per document)
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
    -- Document: Show document_id and original_file_name
    -- If permission is on document, use that document
    -- If permission is on folder, show all documents in that folder
    COALESCE(dp.document_id, d_in_folder.id) AS document_id,
    COALESCE(d.original_file_name, d_in_folder.original_file_name) AS document_name
FROM document_permissions dp
LEFT JOIN users u ON dp.user_id = u.id
LEFT JOIN folders f ON dp.folder_id = f.id
LEFT JOIN documents d ON dp.document_id = d.id  -- Direct document permission
LEFT JOIN documents d_in_folder ON dp.folder_id IS NOT NULL 
    AND dp.document_id IS NULL 
    AND d_in_folder.folder_id = dp.folder_id;  -- Documents in folder

-- Test it - you should now see document names!
SELECT 
    id,
    permission_type,
    user_full_name,
    role_name,
    folder_name,
    document_id,
    document_name  -- This should NOT be null for folder permissions!
FROM permissions_with_names
ORDER BY id, document_name;
