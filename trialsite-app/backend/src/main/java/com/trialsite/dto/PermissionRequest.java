package com.trialsite.dto;

import com.trialsite.model.DocumentPermission;
import com.trialsite.model.User;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PermissionRequest {
    
    @NotNull(message = "Permission type is required")
    private DocumentPermission.PermissionType permissionType;
    
    private Long documentId;
    private Long folderId;
    
    private Long userId; // For user-specific permission
    private User.Role role; // For role-based permission
    
    // Validation: Either documentId or folderId must be provided
    // Either userId or role must be provided
}
