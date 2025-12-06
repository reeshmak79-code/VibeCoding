# Why is `role_name` NULL?

## Explanation

**`role_name` is NULL when:**
- Permission is granted to a **specific user** (user_id has a value)
- Example: You grant READ permission to "John Doe" → `user_id = 17`, `role_name = NULL`

**`role_name` has a value when:**
- Permission is granted to a **role** (like ADMIN, AUDITOR, DOCTOR)
- Example: You grant READ permission to "AUDITOR" role → `user_id = NULL`, `role_name = 'AUDITOR'`

## In Your Database

Looking at your permissions:
- All permissions have `user_id` set (1, 16, 17)
- All permissions have `role_name = NULL`
- This means: **Permissions were granted to specific users, not to roles**

## How to Grant Permission to a Role

In the permission modal:
1. Select "Grant Permission To" → **"Role (e.g., AUDITOR)"**
2. Select a role (ADMIN, AUDITOR, DOCTOR, etc.)
3. Select permission type
4. Click "Grant Permission"

Result: `user_id = NULL`, `role_name = 'AUDITOR'` (or whatever role you chose)

## Summary

- **User-specific permission:** `user_id` has value, `role_name = NULL` ✅ (This is what you have)
- **Role-based permission:** `user_id = NULL`, `role_name` has value ✅ (Use this for granting to all users with that role)

Both are valid! It depends on whether you want to grant to one user or all users with a specific role.
