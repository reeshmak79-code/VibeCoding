-- ============================================
-- VIEW DOCUMENTS IN FOLDERS WITH PERMISSIONS
-- Run in H2 Console: http://localhost:8080/h2-console
-- ============================================

-- This shows documents that are INSIDE folders that have permissions
-- Even though permission is on folder, you can see which documents are in that folder

SELECT 
    dp.id AS permission_id,
    dp.permission_type,
    dp.granted_by,
    -- Permission target (folder)
    dp.folder_id,
    f.folder_name,
    -- Documents in this folder
    d.id AS document_id,
    d.original_file_name AS document_name,
    d.document_type,
    d.file_size,
    d.uploaded_by,
    d.uploaded_at,
    -- User who has permission
    u.full_name AS user_name,
    u.email AS user_email,
    u.role AS user_role,
    dp.granted_at
FROM document_permissions dp
LEFT JOIN folders f ON dp.folder_id = f.id
LEFT JOIN documents d ON d.folder_id = f.id  -- Documents IN the folder
LEFT JOIN users u ON dp.user_id = u.id
WHERE dp.folder_id IS NOT NULL  -- Only folder permissions
ORDER BY f.folder_name, d.original_file_name;

-- Alternative: Show permissions with documents listed separately
SELECT 
    dp.id,
    dp.permission_type,
    dp.granted_by,
    -- Folder info
    dp.folder_id,
    f.folder_name,
    -- User info
    u.full_name AS user_name,
    u.role AS user_role,
    -- Document count in folder
    (SELECT COUNT(*) FROM documents WHERE folder_id = dp.folder_id) AS document_count,
    -- List of documents (comma separated)
    (SELECT STRING_AGG(original_file_name, ', ') 
     FROM documents 
     WHERE folder_id = dp.folder_id) AS documents_in_folder,
    dp.granted_at
FROM document_permissions dp
LEFT JOIN folders f ON dp.folder_id = f.id
LEFT JOIN users u ON dp.user_id = u.id
WHERE dp.folder_id IS NOT NULL
ORDER BY f.folder_name;

-- If you want to see document permissions (when granted directly to documents):
SELECT 
    dp.id,
    dp.permission_type,
    dp.granted_by,
    dp.document_id,
    d.original_file_name AS document_name,  -- This will NOT be null!
    d.document_type,
    u.full_name AS user_name,
    u.role AS user_role,
    dp.granted_at
FROM document_permissions dp
LEFT JOIN documents d ON dp.document_id = d.id
LEFT JOIN users u ON dp.user_id = u.id
WHERE dp.document_id IS NOT NULL  -- Only document permissions
ORDER BY d.original_file_name;
