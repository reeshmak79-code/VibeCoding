package com.trialsite.dto;

import com.trialsite.model.DocumentPermission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    private Long id;
    private String permissionType;
    private Long documentId;
    private String documentName;
    private Long folderId;
    private String folderName;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userRole;  // User's role from users table (ADMIN, USER, AUDITOR, etc.)
    private String role;  // Permission role (if granted to a role, not a user)
    private String grantedBy;
    private LocalDateTime grantedAt;
    
    public PermissionResponse(DocumentPermission permission) {
        this.id = permission.getId();
        this.permissionType = permission.getPermissionType().name();
        this.documentId = permission.getDocument() != null ? permission.getDocument().getId() : null;
        this.documentName = permission.getDocument() != null ? permission.getDocument().getOriginalFileName() : null;
        this.folderId = permission.getFolder() != null ? permission.getFolder().getId() : null;
        this.folderName = permission.getFolder() != null ? permission.getFolder().getFolderName() : null;
        this.userId = permission.getUser() != null ? permission.getUser().getId() : null;
        this.userName = permission.getUser() != null ? permission.getUser().getFullName() : null;
        this.userEmail = permission.getUser() != null ? permission.getUser().getEmail() : null;
        this.userRole = permission.getUser() != null ? permission.getUser().getRole().name() : null;  // User's role from users table
        this.role = permission.getRole() != null ? permission.getRole().name() : null;  // Permission role (if granted to role)
        this.grantedBy = permission.getGrantedBy();
        this.grantedAt = permission.getGrantedAt();
    }
}
