-- ============================================
-- CREATE VIEW FOR PERMISSIONS WITH NAMES
-- Run in H2 Console: http://localhost:8080/h2-console
-- ============================================

-- Create a VIEW that shows permissions with user names and folder/document names
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
    -- Document information (shows original file name when permission is on document)
    dp.document_id,
    d.original_file_name AS document_name
FROM document_permissions dp
LEFT JOIN users u ON dp.user_id = u.id
LEFT JOIN folders f ON dp.folder_id = f.id
LEFT JOIN documents d ON dp.document_id = d.id;

-- Now you can query the view easily:
-- SELECT * FROM permissions_with_names;

-- Or get a complete view with all details:
SELECT 
    id,
    permission_type,
    -- User details
    user_id,
    user_full_name,
    user_email,
    -- Role: Shows user's role from users table, or permission role if granted to role
    role_name,
    -- Folder details
    folder_id,
    folder_name,
    -- Document details (always shown, even if null)
    document_id,
    document_name,
    -- Summary
    CASE 
        WHEN user_id IS NOT NULL THEN user_full_name || ' (' || role_name || ')'
        WHEN role_name IS NOT NULL THEN 'Role: ' || role_name
        ELSE 'Unknown'
    END AS granted_to,
    CASE 
        WHEN folder_id IS NOT NULL THEN 'Folder: ' || folder_name
        WHEN document_id IS NOT NULL THEN 'Document: ' || document_name
        ELSE 'N/A'
    END AS target,
    granted_by,
    granted_at
FROM permissions_with_names
ORDER BY granted_at DESC;
