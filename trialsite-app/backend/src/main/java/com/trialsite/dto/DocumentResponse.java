package com.trialsite.dto;

import com.trialsite.model.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String fileType;
    private String documentType;
    private String description;
    private Long projectId;
    private String projectName;
    private Long folderId;
    private String folderName;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    
    public DocumentResponse(Document document) {
        this.id = document.getId();
        this.fileName = document.getFileName();
        this.originalFileName = document.getOriginalFileName();
        this.fileSize = document.getFileSize();
        this.fileType = document.getFileType();
        // Handle null documentType for old documents
        this.documentType = document.getDocumentType() != null 
            ? document.getDocumentType().name() 
            : Document.DocumentType.OTHER.name();
        this.description = document.getDescription();
        // Handle null project (shouldn't happen, but safety check)
        if (document.getProject() != null) {
            this.projectId = document.getProject().getId();
            this.projectName = document.getProject().getProjectName();
        } else {
            this.projectId = null;
            this.projectName = null;
        }
        this.folderId = document.getFolder() != null ? document.getFolder().getId() : null;
        this.folderName = document.getFolder() != null ? document.getFolder().getFolderName() : null;
        this.uploadedBy = document.getUploadedBy();
        this.uploadedAt = document.getUploadedAt();
    }
}
