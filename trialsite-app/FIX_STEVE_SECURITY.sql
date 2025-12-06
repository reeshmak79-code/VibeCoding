-- ============================================
-- FIX STEVE SECURITY ISSUE
-- Run in H2 Console: http://localhost:8080/h2-console
-- ============================================

-- 1. Fix Steve's role back to USER (not ADMIN)
UPDATE users 
SET role = 'USER' 
WHERE username = 'steve' OR username = 'Steve';

-- 2. Delete permissions that Steve created (he shouldn't have been able to)
DELETE FROM document_permissions 
WHERE granted_by = 'steve' OR granted_by = 'Steve';

-- 3. Verify Steve's role is now USER
SELECT id, username, email, role, active 
FROM users 
WHERE username = 'steve' OR username = 'Steve';

-- 4. Verify Steve's permissions are deleted
SELECT * FROM document_permissions 
WHERE granted_by = 'steve' OR granted_by = 'Steve';
-- Should return 0 rows

-- 5. View all remaining permissions with FULL DETAILS (should only have reesh's)
SELECT 
    dp.id,
    dp.permission_type AS "Permission Type",
    dp.granted_by AS "Granted By",
    -- User info
    dp.user_id AS "User ID",
    u.full_name AS "User Name",
    u.email AS "User Email",
    -- Role: Shows user's role from users table, or permission role if granted to role
    COALESCE(u.role, dp.role_name) AS "Role Name",
    -- Target info
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

-- 6. SIMPLE VIEW - Just the essentials (no username, shows user role)
SELECT 
    dp.id,
    dp.permission_type,
    dp.user_id,
    u.full_name AS user_name,
    CAST(COALESCE(CAST(u.role AS VARCHAR), CAST(dp.role_name AS VARCHAR)) AS VARCHAR) AS role_name,
    CASE 
        WHEN dp.user_id IS NOT NULL THEN u.full_name || ' (' || u.role || ')'
        WHEN dp.role_name IS NOT NULL THEN 'Role: ' || dp.role_name
        ELSE 'Unknown'
    END AS granted_to,
    CASE 
        WHEN dp.folder_id IS NOT NULL THEN 'Folder: ' || f.folder_name
        WHEN dp.document_id IS NOT NULL THEN 'Document: ' || d.original_file_name
        ELSE 'N/A'
    END AS target,
    dp.granted_by,
    dp.granted_at
FROM document_permissions dp
LEFT JOIN users u ON dp.user_id = u.id
LEFT JOIN folders f ON dp.folder_id = f.id
LEFT JOIN documents d ON dp.document_id = d.id
ORDER BY dp.granted_at DESC;
