package com.trialsite.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPermission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionType permissionType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name")
    private User.Role role;
    
    @Column(nullable = false)
    private String grantedBy;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime grantedAt;
    
    public enum PermissionType {
        READ,
        WRITE,
        DELETE
    }
    
    // Helper method to check if permission applies to a role
    public boolean appliesToRole(User.Role userRole) {
        return this.role != null && this.role.equals(userRole);
    }
    
    // Helper method to check if permission applies to a user
    public boolean appliesToUser(Long userId) {
        return this.user != null && this.user.getId().equals(userId);
    }
}
