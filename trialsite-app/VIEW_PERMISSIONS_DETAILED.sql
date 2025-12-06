-- ============================================
-- VIEW PERMISSIONS WITH FULL DETAILS
-- Run in H2 Console: http://localhost:8080/h2-console
-- ============================================

-- OPTION 1: DETAILED VIEW (shows everything)
SELECT 
    dp.id,
    dp.permission_type AS "Permission Type",
    dp.granted_by AS "Granted By",
    CASE 
        WHEN dp.user_id IS NOT NULL THEN u.full_name || ' (' || u.email || ')'
        WHEN dp.role IS NOT NULL THEN 'Role: ' || dp.role
        ELSE 'Unknown'
    END AS "Granted To",
    u.username AS "Username",
    u.id AS "User ID",
    CASE 
        WHEN dp.folder_id IS NOT NULL THEN f.folder_name
        WHEN dp.document_id IS NOT NULL THEN d.original_file_name
        ELSE 'N/A'
    END AS "Target (Folder/Document)",
    dp.folder_id AS "Folder ID",
    dp.document_id AS "Document ID",
    dp.granted_at AS "Granted At"
FROM document_permissions dp
LEFT JOIN users u ON dp.user_id = u.id
LEFT JOIN folders f ON dp.folder_id = f.id
LEFT JOIN documents d ON dp.document_id = d.id
ORDER BY dp.granted_at DESC;

-- OPTION 2: COMPLETE VIEW - Shows ALL IDs + Names (including role_name and document_name)
SELECT 
    dp.id,
    dp.permission_type AS "Permission",
    -- User info
    dp.user_id AS "User ID",
    u.full_name AS "User Name",
    u.email AS "User Email",
    -- Role: Shows user's role from users table, or permission role if granted to role
    -- Cast to VARCHAR to avoid CHECK constraint issues
    CAST(COALESCE(CAST(u.role AS VARCHAR), CAST(dp.role_name AS VARCHAR)) AS VARCHAR) AS "Role Name",
    -- Folder info
    dp.folder_id AS "Folder ID",
    f.folder_name AS "Folder Name",
    -- Document info (always shown, even if null)
    dp.document_id AS "Document ID",
    d.original_file_name AS "Document Name",
    -- Summary columns
    CASE 
        WHEN dp.user_id IS NOT NULL THEN u.full_name || ' (' || u.role || ')'
        WHEN dp.role_name IS NOT NULL THEN 'Role: ' || dp.role_name
        ELSE 'Unknown'
    END AS "Granted To",
    CASE 
        WHEN dp.folder_id IS NOT NULL THEN 'Folder: ' || f.folder_name
        WHEN dp.document_id IS NOT NULL THEN 'Document: ' || d.original_file_name
        ELSE 'N/A'
    END AS "Target",
    dp.granted_by AS "Granted By",
    dp.granted_at AS "Date"
FROM document_permissions dp
LEFT JOIN users u ON dp.user_id = u.id
LEFT JOIN folders f ON dp.folder_id = f.id
LEFT JOIN documents d ON dp.document_id = d.id
ORDER BY dp.granted_at DESC;

-- OPTION 3: Find permissions for a specific user (replace USER_ID)
SELECT 
    dp.id,
    dp.permission_type,
    CASE 
        WHEN dp.folder_id IS NOT NULL THEN 'Folder: ' || f.folder_name
        WHEN dp.document_id IS NOT NULL THEN 'Document: ' || d.original_file_name
        ELSE 'N/A'
    END AS target,
    dp.granted_by,
    dp.granted_at
FROM document_permissions dp
LEFT JOIN folders f ON dp.folder_id = f.id
LEFT JOIN documents d ON dp.document_id = d.id
WHERE dp.user_id = 1  -- Change to your user ID
ORDER BY dp.granted_at DESC;

-- OPTION 4: Find permissions for a specific folder (replace FOLDER_ID)
SELECT 
    dp.id,
    dp.permission_type,
    CASE 
        WHEN dp.user_id IS NOT NULL THEN u.full_name || ' (' || u.username || ')'
        WHEN dp.role IS NOT NULL THEN 'Role: ' || dp.role
        ELSE 'Unknown'
    END AS granted_to,
    dp.granted_by,
    dp.granted_at
FROM document_permissions dp
LEFT JOIN users u ON dp.user_id = u.id
WHERE dp.folder_id = 1  -- Change to your folder ID
ORDER BY dp.granted_at DESC;

-- OPTION 5: Find which user has which ID
SELECT id, username, email, full_name, role 
FROM users 
ORDER BY id;
