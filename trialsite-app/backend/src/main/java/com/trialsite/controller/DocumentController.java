package com.trialsite.controller;

import com.trialsite.dto.DocumentResponse;
import com.trialsite.dto.MessageResponse;
import com.trialsite.model.Document;
import com.trialsite.model.DocumentPermission;
import com.trialsite.model.Folder;
import com.trialsite.model.Project;
import com.trialsite.model.User;
import com.trialsite.repository.DocumentPermissionRepository;
import com.trialsite.repository.DocumentRepository;
import com.trialsite.repository.FolderRepository;
import com.trialsite.repository.ProjectRepository;
import com.trialsite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DocumentController {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private FolderRepository folderRepository;
    
    @Autowired
    private DocumentPermissionRepository permissionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private final String UPLOAD_DIR = "uploads/";
    
    public DocumentController() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!");
        }
    }
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Long projectId,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "folderId", required = false) Long folderId,
            Authentication authentication) {
        
        // Only ADMIN and DOCTOR can upload
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (currentUser.getRole() != User.Role.ADMIN && currentUser.getRole() != User.Role.DOCTOR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Access denied. Only admins and doctors can upload documents."));
        }
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Please select a file to upload"));
            }
            
            // Validate project exists
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            
            // Validate folder if provided
            Folder folder = null;
            if (folderId != null) {
                folder = folderRepository.findById(folderId)
                        .orElseThrow(() -> new RuntimeException("Folder not found"));
                // Ensure folder belongs to the same project
                if (!folder.getProject().getId().equals(projectId)) {
                    return ResponseEntity.badRequest()
                            .body(new MessageResponse("Folder does not belong to the selected project"));
                }
            }
            
            // Generate unique filename
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;
            
            // Save file to disk
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create document record
            Document document = new Document();
            document.setFileName(fileName);
            document.setOriginalFileName(originalFileName);
            document.setFilePath(filePath.toString());
            document.setFileSize(file.getSize());
            document.setFileType(file.getContentType());
            document.setDocumentType(Document.DocumentType.valueOf(documentType));
            document.setDescription(description);
            document.setProject(project);
            document.setFolder(folder);
            document.setUploadedBy(authentication.getName());
            
            Document savedDocument = documentRepository.save(document);
            
            return ResponseEntity.ok(new DocumentResponse(savedDocument));
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
    
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<DocumentResponse>> getProjectDocuments(
            @PathVariable Long projectId,
            Authentication authentication) {
        System.out.println("=== DEBUG: getProjectDocuments called for projectId: " + projectId);
        System.out.println("=== DEBUG: Authentication: " + authentication.getName());
        
        try {
            List<Document> documents = documentRepository.findByProjectId(projectId);
            System.out.println("=== DEBUG: Found " + (documents != null ? documents.size() : 0) + " documents from repository");
            
            // Filter by permissions for non-admin/doctor users
            User currentUser = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            System.out.println("=== DEBUG: Current user role: " + currentUser.getRole());
            
            if (currentUser.getRole() != User.Role.ADMIN && currentUser.getRole() != User.Role.DOCTOR) {
                System.out.println("=== DEBUG: Filtering documents by permissions");
                documents = filterDocumentsByPermissions(documents, currentUser);
                System.out.println("=== DEBUG: After permission filter: " + documents.size() + " documents");
            }
            
            System.out.println("=== DEBUG: Processing " + documents.size() + " documents for response");
            List<DocumentResponse> response = new java.util.ArrayList<>();
            int successCount = 0;
            int errorCount = 0;
            
            for (Document doc : documents) {
                try {
                    System.out.println("=== DEBUG: Processing document ID: " + doc.getId() + 
                            ", fileName: " + doc.getFileName() + 
                            ", documentType: " + (doc.getDocumentType() != null ? doc.getDocumentType() : "NULL") +
                            ", project: " + (doc.getProject() != null ? doc.getProject().getId() : "NULL"));
                    
                    // Filter out documents with null project (data integrity issue)
                    if (doc.getProject() == null) {
                        System.out.println("=== DEBUG: Skipping document " + doc.getId() + " - null project");
                        errorCount++;
                        continue;
                    }
                    
                    DocumentResponse docResponse = new DocumentResponse(doc);
                    response.add(docResponse);
                    successCount++;
                    System.out.println("=== DEBUG: Successfully created DocumentResponse for document ID: " + doc.getId());
                } catch (Exception e) {
                    // Log error but don't crash - skip this document
                    System.err.println("=== ERROR: Failed to create DocumentResponse for document ID " + doc.getId());
                    System.err.println("=== ERROR: Exception type: " + e.getClass().getName());
                    System.err.println("=== ERROR: Exception message: " + e.getMessage());
                    System.err.println("=== ERROR: Document details - ID: " + doc.getId() + 
                            ", fileName: " + doc.getFileName() + 
                            ", documentType: " + (doc.getDocumentType() != null ? doc.getDocumentType() : "NULL") +
                            ", project: " + (doc.getProject() != null ? doc.getProject().getId() : "NULL") +
                            ", folder: " + (doc.getFolder() != null ? doc.getFolder().getId() : "NULL"));
                    e.printStackTrace();
                    errorCount++;
                }
            }
            
            System.out.println("=== DEBUG: Response created - Success: " + successCount + ", Errors: " + errorCount + ", Total: " + response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("=== FATAL ERROR in getProjectDocuments for projectId " + projectId);
            System.err.println("=== FATAL ERROR: Exception type: " + e.getClass().getName());
            System.err.println("=== FATAL ERROR: Exception message: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new java.util.ArrayList<>());
        }
    }
    
    @GetMapping("/folder/{folderId}")
    public ResponseEntity<List<DocumentResponse>> getFolderDocuments(
            @PathVariable Long folderId,
            Authentication authentication) {
        List<Document> documents = documentRepository.findByFolderId(folderId);
        
        // Filter by permissions for non-admin/doctor users
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (currentUser.getRole() != User.Role.ADMIN && currentUser.getRole() != User.Role.DOCTOR) {
            documents = filterDocumentsByPermissions(documents, currentUser);
        }
        
        List<DocumentResponse> response = documents.stream()
                .filter(doc -> {
                    // Filter out documents with null project (data integrity issue)
                    return doc.getProject() != null;
                })
                .map(doc -> {
                    try {
                        return new DocumentResponse(doc);
                    } catch (Exception e) {
                        // Log error but don't crash - return null and filter it out
                        System.err.println("Error creating DocumentResponse for document ID " + doc.getId() + ": " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(docResponse -> docResponse != null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return ResponseEntity.ok(new DocumentResponse(document));
    }
    
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Document document = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            
            // Check permissions for non-admin/doctor users
            User currentUser = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (currentUser.getRole() != User.Role.ADMIN && currentUser.getRole() != User.Role.DOCTOR) {
                if (!hasPermission(document, currentUser, DocumentPermission.PermissionType.READ)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(null);
                }
            }
            
            Path filePath = Paths.get(document.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("File not found or not readable");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error downloading file: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteDocument(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Document document = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            
            // Only ADMIN and DOCTOR can delete (or restricted users with DELETE permission)
            User currentUser = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (currentUser.getRole() != User.Role.ADMIN && currentUser.getRole() != User.Role.DOCTOR) {
                if (!hasPermission(document, currentUser, DocumentPermission.PermissionType.DELETE)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new MessageResponse("Access denied. You do not have DELETE permission for this document."));
                }
            }
            
            // Delete all permissions associated with this document first
            // This must be done before deleting the document to avoid foreign key constraint violations
            permissionRepository.deleteByDocumentId(id);
            
            // Delete file from disk
            Path filePath = Paths.get(document.getFilePath());
            Files.deleteIfExists(filePath);
            
            // Delete from database
            documentRepository.delete(document);
            
            return ResponseEntity.ok(new MessageResponse("Document deleted successfully"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to delete file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
    
    @GetMapping("/stats/project/{projectId}")
    public ResponseEntity<?> getProjectDocumentStats(
            @PathVariable Long projectId,
            Authentication authentication) {
        try {
            List<Document> documents = documentRepository.findByProjectId(projectId);
            
            // Filter by permissions for non-admin/doctor users
            User currentUser = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (currentUser.getRole() != User.Role.ADMIN && currentUser.getRole() != User.Role.DOCTOR) {
                documents = filterDocumentsByPermissions(documents, currentUser);
            }
            
            // Filter out documents with null project (data integrity issue)
            documents = documents.stream()
                    .filter(doc -> doc.getProject() != null)
                    .collect(Collectors.toList());
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", documents.size());
            stats.put("totalSize", documents.stream()
                    .filter(d -> d.getFileSize() != null)
                    .mapToLong(Document::getFileSize)
                    .sum());
            
            // Count by document type (handle null documentType)
            Map<String, Long> byType = new HashMap<>();
            for (Document.DocumentType type : Document.DocumentType.values()) {
                long count = documents.stream()
                        .filter(d -> d.getDocumentType() != null && d.getDocumentType() == type)
                        .count();
                byType.put(type.name(), count);
            }
            // Count documents with null documentType as "OTHER"
            long nullTypeCount = documents.stream()
                    .filter(d -> d.getDocumentType() == null)
                    .count();
            if (nullTypeCount > 0) {
                byType.put("OTHER", byType.getOrDefault("OTHER", 0L) + nullTypeCount);
            }
            stats.put("byType", byType);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            // Return empty stats instead of crashing
            System.err.println("Error calculating stats for project " + projectId + ": " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", 0);
            stats.put("totalSize", 0);
            stats.put("byType", new HashMap<String, Long>());
            return ResponseEntity.ok(stats);
        }
    }
    
    // Helper method to filter documents by permissions for restricted users (USER, AUDITOR, etc.)
    private List<Document> filterDocumentsByPermissions(List<Document> documents, User user) {
        // Get all permissions for this user
        List<DocumentPermission> userPermissions = permissionRepository.findByUserId(user.getId());
        
        // Get document IDs and folder IDs that user has permission to
        java.util.Set<Long> allowedDocumentIds = new java.util.HashSet<>();
        java.util.Set<Long> allowedFolderIds = new java.util.HashSet<>();
        
        for (DocumentPermission perm : userPermissions) {
            if (perm.getDocument() != null) {
                allowedDocumentIds.add(perm.getDocument().getId());
            }
            if (perm.getFolder() != null) {
                allowedFolderIds.add(perm.getFolder().getId());
            }
        }
        
        // Also check role-based permissions
        List<DocumentPermission> rolePermissions = permissionRepository.findByRole(user.getRole());
        for (DocumentPermission perm : rolePermissions) {
            if (perm.getDocument() != null) {
                allowedDocumentIds.add(perm.getDocument().getId());
            }
            if (perm.getFolder() != null) {
                allowedFolderIds.add(perm.getFolder().getId());
            }
        }
        
        // Filter documents: user has permission to document OR document is in folder with permission
        return documents.stream()
                .filter(doc -> {
                    return allowedDocumentIds.contains(doc.getId()) || 
                           (doc.getFolder() != null && allowedFolderIds.contains(doc.getFolder().getId()));
                })
                .collect(Collectors.toList());
    }
    
    // Helper method to check if user has specific permission for a document
    private boolean hasPermission(Document document, User user, DocumentPermission.PermissionType requiredType) {
        // Check direct document permission (user-specific)
        List<DocumentPermission> docPermissions = permissionRepository.findByDocumentId(document.getId());
        for (DocumentPermission perm : docPermissions) {
            if (perm.getUser() != null && perm.getUser().getId().equals(user.getId())) {
                // READ permission allows READ
                // WRITE permission allows READ and WRITE
                // DELETE permission allows READ, WRITE, and DELETE
                if (requiredType == DocumentPermission.PermissionType.READ) {
                    return true; // Any permission type allows READ
                }
                if (requiredType == DocumentPermission.PermissionType.DELETE) {
                    return perm.getPermissionType() == DocumentPermission.PermissionType.DELETE;
                }
            }
            // Check role-based permission
            if (perm.getRole() != null && perm.getRole() == user.getRole()) {
                if (requiredType == DocumentPermission.PermissionType.READ) {
                    return true; // Any permission type allows READ
                }
                if (requiredType == DocumentPermission.PermissionType.DELETE) {
                    return perm.getPermissionType() == DocumentPermission.PermissionType.DELETE;
                }
            }
        }
        
        // Check folder permission (if document is in a folder)
        if (document.getFolder() != null) {
            List<DocumentPermission> folderPermissions = permissionRepository.findByFolderId(document.getFolder().getId());
            for (DocumentPermission perm : folderPermissions) {
                if (perm.getUser() != null && perm.getUser().getId().equals(user.getId())) {
                    if (requiredType == DocumentPermission.PermissionType.READ) {
                        return true;
                    }
                    if (requiredType == DocumentPermission.PermissionType.DELETE) {
                        return perm.getPermissionType() == DocumentPermission.PermissionType.DELETE;
                    }
                }
                if (perm.getRole() != null && perm.getRole() == user.getRole()) {
                    if (requiredType == DocumentPermission.PermissionType.READ) {
                        return true;
                    }
                    if (requiredType == DocumentPermission.PermissionType.DELETE) {
                        return perm.getPermissionType() == DocumentPermission.PermissionType.DELETE;
                    }
                }
            }
        }
        
        return false;
    }
}
