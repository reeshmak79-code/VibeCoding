-- ============================================
-- UPDATE PERMISSIONS_WITH_NAMES VIEW
-- Run in H2 Console: http://localhost:8080/h2-console
-- ============================================

-- Drop and recreate the view to show document original file names
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
    -- Role information: Show user's role from users table, or permission's role if granted to role
    -- Cast to VARCHAR to avoid CHECK constraint issues
    CAST(COALESCE(CAST(u.role AS VARCHAR), CAST(dp.role_name AS VARCHAR)) AS VARCHAR) AS role_name,
    -- Folder information
    dp.folder_id,
    f.folder_name,
    f.description AS folder_description,
    -- Document information: 
    -- If permission is on document, show that document's original_file_name
    -- If permission is on folder, show documents IN that folder (original_file_name)
    COALESCE(dp.document_id, d_in_folder.id) AS document_id,
    COALESCE(d.original_file_name, d_in_folder.original_file_name) AS document_name
FROM document_permissions dp
LEFT JOIN users u ON dp.user_id = u.id
LEFT JOIN folders f ON dp.folder_id = f.id
LEFT JOIN documents d ON dp.document_id = d.id  -- Document permission (direct)
LEFT JOIN documents d_in_folder ON dp.folder_id IS NOT NULL AND dp.document_id IS NULL AND d_in_folder.folder_id = dp.folder_id;  -- Documents in folder

-- Test the view
SELECT * FROM permissions_with_names;

-- If you want to see documents INSIDE folders (when permission is on folder):
-- This creates multiple rows (one per document in folder)
SELECT 
    dp.id AS permission_id,
    dp.permission_type,
    dp.granted_by,
    f.folder_name,
    d.original_file_name AS document_name,  -- Documents in the folder
    u.full_name AS user_name,
    CAST(COALESCE(CAST(u.role AS VARCHAR), CAST(dp.role_name AS VARCHAR)) AS VARCHAR) AS role_name
FROM document_permissions dp
LEFT JOIN folders f ON dp.folder_id = f.id
LEFT JOIN documents d ON d.folder_id = f.id  -- Documents IN the folder
LEFT JOIN users u ON dp.user_id = u.id
WHERE dp.folder_id IS NOT NULL
ORDER BY f.folder_name, d.original_file_name;
