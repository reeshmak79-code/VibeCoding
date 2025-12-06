package com.trialsite.dto;

import com.trialsite.model.Folder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolderResponse {
    private Long id;
    private String folderName;
    private String description;
    private Long projectId;
    private String projectName;
    private Long parentFolderId;
    private String parentFolderName;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int documentCount;
    private int subFolderCount;
    private List<FolderResponse> subFolders;
    
    public FolderResponse(Folder folder) {
        this.id = folder.getId();
        this.folderName = folder.getFolderName();
        this.description = folder.getDescription();
        this.projectId = folder.getProject().getId();
        this.projectName = folder.getProject().getProjectName();
        this.parentFolderId = folder.getParentFolder() != null ? folder.getParentFolder().getId() : null;
        this.parentFolderName = folder.getParentFolder() != null ? folder.getParentFolder().getFolderName() : null;
        this.createdBy = folder.getCreatedBy();
        this.createdAt = folder.getCreatedAt();
        this.updatedAt = folder.getUpdatedAt();
        this.documentCount = folder.getDocuments() != null ? folder.getDocuments().size() : 0;
        this.subFolderCount = folder.getSubFolders() != null ? folder.getSubFolders().size() : 0;
        
        // Recursively map subfolders
        if (folder.getSubFolders() != null && !folder.getSubFolders().isEmpty()) {
            this.subFolders = folder.getSubFolders().stream()
                    .map(FolderResponse::new)
                    .collect(Collectors.toList());
        }
    }
}
