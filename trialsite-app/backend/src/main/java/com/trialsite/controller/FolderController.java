package com.trialsite.controller;

import com.trialsite.dto.FolderRequest;
import com.trialsite.dto.FolderResponse;
import com.trialsite.dto.MessageResponse;
import com.trialsite.model.Folder;
import com.trialsite.model.Project;
import com.trialsite.repository.DocumentPermissionRepository;
import com.trialsite.repository.FolderRepository;
import com.trialsite.repository.ProjectRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/folders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FolderController {
    
    @Autowired
    private FolderRepository folderRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private DocumentPermissionRepository permissionRepository;
    
    @PostMapping
    public ResponseEntity<?> createFolder(
            @Valid @RequestBody FolderRequest request,
            Authentication authentication) {
        try {
            // Validate project exists
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            
            Folder folder = new Folder();
            folder.setFolderName(request.getFolderName());
            folder.setDescription(request.getDescription());
            folder.setProject(project);
            folder.setCreatedBy(authentication.getName());
            
            // Set parent folder if provided
            if (request.getParentFolderId() != null) {
                Folder parentFolder = folderRepository.findById(request.getParentFolderId())
                        .orElseThrow(() -> new RuntimeException("Parent folder not found"));
                folder.setParentFolder(parentFolder);
            }
            
            Folder savedFolder = folderRepository.save(folder);
            return ResponseEntity.ok(new FolderResponse(savedFolder));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error creating folder: " + e.getMessage()));
        }
    }
    
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<FolderResponse>> getProjectFolders(@PathVariable Long projectId) {
        List<Folder> folders = folderRepository.findByProjectIdAndParentFolderIsNull(projectId);
        List<FolderResponse> response = folders.stream()
                .map(FolderResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getFolder(@PathVariable Long id) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        return ResponseEntity.ok(new FolderResponse(folder));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFolder(
            @PathVariable Long id,
            @Valid @RequestBody FolderRequest request,
            Authentication authentication) {
        try {
            Folder folder = folderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            
            folder.setFolderName(request.getFolderName());
            folder.setDescription(request.getDescription());
            
            // Update parent folder if changed
            if (request.getParentFolderId() != null) {
                Folder parentFolder = folderRepository.findById(request.getParentFolderId())
                        .orElseThrow(() -> new RuntimeException("Parent folder not found"));
                folder.setParentFolder(parentFolder);
            } else {
                folder.setParentFolder(null);
            }
            
            Folder updatedFolder = folderRepository.save(folder);
            return ResponseEntity.ok(new FolderResponse(updatedFolder));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error updating folder: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteFolder(@PathVariable Long id) {
        try {
            Folder folder = folderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            
            // Check if folder has subfolders or documents
            if (folder.getSubFolders() != null && !folder.getSubFolders().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Cannot delete folder with subfolders. Delete subfolders first."));
            }
            
            if (folder.getDocuments() != null && !folder.getDocuments().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Cannot delete folder with documents. Move or delete documents first."));
            }
            
            // Delete all permissions associated with this folder first
            // This must be done before deleting the folder to avoid foreign key constraint violations
            permissionRepository.deleteByFolderId(id);
            
            // Now delete the folder
            folderRepository.delete(folder);
            return ResponseEntity.ok(new MessageResponse("Folder deleted successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error deleting folder: " + e.getMessage()));
        }
    }
}
