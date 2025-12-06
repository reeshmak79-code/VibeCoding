# 🔧 Fix Password Constraint Error

## Problem
Getting error: `Check constraint violation: "CONSTRAINT_4"` when creating users.

## Root Cause
H2 database created a check constraint from `@Size(min = 6)` annotation on User.password field. The constraint might be checking the wrong value or the database schema needs to be updated.

## Solution Options

### Option 1: Drop Database and Recreate (Easiest - for development)
**⚠️ This will DELETE all your data!**

1. Stop the backend
2. Delete the database file:
   ```powershell
   cd C:\Users\reesh\VibeCoding\trialsite-app\backend
   Remove-Item data\trialsite.mv.db
   Remove-Item data\trialsite.trace.db
   ```
3. Restart backend - it will recreate the database with correct constraints

### Option 2: Fix Constraint in H2 Console (Keeps data)
1. Open H2 Console: http://localhost:8080/h2-console
2. Login:
   - JDBC URL: `jdbc:h2:file:./data/trialsite`
   - Username: `sa`
   - Password: (empty)
3. Run this SQL to drop the constraint:
   ```sql
   ALTER TABLE users DROP CONSTRAINT IF EXISTS CONSTRAINT_4;
   ```
4. Restart backend - it will recreate the constraint correctly

### Option 3: Change DDL Mode (Temporary)
Update `application.properties`:
```properties
spring.jpa.hibernate.ddl-auto=create-drop
```
**⚠️ This will recreate database on every restart (loses data)**

Then change back to `update` after first restart.

## Recommended: Option 2 (Keeps your data)

After fixing, the password field will work correctly:
- ✅ Empty password → Auto-generates 12-char password
- ✅ Generate button → Creates random password
- ✅ Custom password → Must be 6+ characters
- ✅ All passwords encoded with BCrypt (60 chars) → Passes constraint
