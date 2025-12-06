package com.trialsite.controller;

import com.trialsite.dto.MessageResponse;
import com.trialsite.dto.PermissionRequest;
import com.trialsite.dto.PermissionResponse;
import com.trialsite.model.Document;
import com.trialsite.model.DocumentPermission;
import com.trialsite.model.Folder;
import com.trialsite.model.User;
import com.trialsite.repository.DocumentPermissionRepository;
import com.trialsite.repository.DocumentRepository;
import com.trialsite.repository.FolderRepository;
import com.trialsite.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/permissions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PermissionController {
    
    @Autowired
    private DocumentPermissionRepository permissionRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private FolderRepository folderRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<?> grantPermission(
            @Valid @RequestBody PermissionRequest request,
            Authentication authentication) {
        try {
            // Validate: Either documentId or folderId must be provided
            if (request.getDocumentId() == null && request.getFolderId() == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Either documentId or folderId must be provided"));
            }
            
            // Validate: Either userId or role must be provided
            if (request.getUserId() == null && request.getRole() == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Either userId or role must be provided"));
            }
            
            DocumentPermission permission = new DocumentPermission();
            permission.setPermissionType(request.getPermissionType());
            permission.setGrantedBy(authentication.getName());
            
            // Set document or folder
            if (request.getDocumentId() != null) {
                Document document = documentRepository.findById(request.getDocumentId())
                        .orElseThrow(() -> new RuntimeException("Document not found"));
                permission.setDocument(document);
            }
            
            if (request.getFolderId() != null) {
                Folder folder = folderRepository.findById(request.getFolderId())
                        .orElseThrow(() -> new RuntimeException("Folder not found"));
                permission.setFolder(folder);
            }
            
            // Set user or role
            if (request.getUserId() != null) {
                User user = userRepository.findById(request.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                permission.setUser(user);
            }
            
            if (request.getRole() != null) {
                permission.setRole(request.getRole());
            }
            
            // Check if permission already exists
            DocumentPermission existing = null;
            if (request.getDocumentId() != null && request.getUserId() != null) {
                existing = permissionRepository.findByDocumentIdAndUserId(
                        request.getDocumentId(), request.getUserId()).orElse(null);
            } else if (request.getDocumentId() != null && request.getRole() != null) {
                existing = permissionRepository.findByDocumentIdAndRole(
                        request.getDocumentId(), request.getRole()).orElse(null);
            } else if (request.getFolderId() != null && request.getUserId() != null) {
                existing = permissionRepository.findByFolderIdAndUserId(
                        request.getFolderId(), request.getUserId()).orElse(null);
            } else if (request.getFolderId() != null && request.getRole() != null) {
                existing = permissionRepository.findByFolderIdAndRole(
                        request.getFolderId(), request.getRole()).orElse(null);
            }
            
            if (existing != null) {
                // Update existing permission
                existing.setPermissionType(request.getPermissionType());
                DocumentPermission updated = permissionRepository.save(existing);
                return ResponseEntity.ok(new PermissionResponse(updated));
            } else {
                // Create new permission
                DocumentPermission saved = permissionRepository.save(permission);
                return ResponseEntity.ok(new PermissionResponse(saved));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error granting permission: " + e.getMessage()));
        }
    }
    
    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<PermissionResponse>> getDocumentPermissions(@PathVariable Long documentId) {
        List<DocumentPermission> permissions = permissionRepository.findByDocumentId(documentId);
        List<PermissionResponse> response = permissions.stream()
                .map(PermissionResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/folder/{folderId}")
    public ResponseEntity<List<PermissionResponse>> getFolderPermissions(@PathVariable Long folderId) {
        List<DocumentPermission> permissions = permissionRepository.findByFolderId(folderId);
        List<PermissionResponse> response = permissions.stream()
                .map(PermissionResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/role/{role}")
    public ResponseEntity<List<PermissionResponse>> getRolePermissions(@PathVariable String role) {
        try {
            User.Role userRole = User.Role.valueOf(role.toUpperCase());
            List<DocumentPermission> permissions = permissionRepository.findByRole(userRole);
            List<PermissionResponse> response = permissions.stream()
                    .map(PermissionResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> revokePermission(@PathVariable Long id) {
        try {
            permissionRepository.deleteById(id);
            return ResponseEntity.ok(new MessageResponse("Permission revoked successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error revoking permission: " + e.getMessage()));
        }
    }
}
