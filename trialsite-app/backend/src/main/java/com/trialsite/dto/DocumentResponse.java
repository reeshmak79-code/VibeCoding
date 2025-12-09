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
        System.out.println("=== DEBUG DocumentResponse: Creating response for document ID: " + document.getId());
        try {
            this.id = document.getId();
            System.out.println("=== DEBUG DocumentResponse: id = " + this.id);
            
            this.fileName = document.getFileName();
            System.out.println("=== DEBUG DocumentResponse: fileName = " + this.fileName);
            
            this.originalFileName = document.getOriginalFileName();
            System.out.println("=== DEBUG DocumentResponse: originalFileName = " + this.originalFileName);
            
            this.fileSize = document.getFileSize();
            System.out.println("=== DEBUG DocumentResponse: fileSize = " + this.fileSize);
            
            this.fileType = document.getFileType();
            System.out.println("=== DEBUG DocumentResponse: fileType = " + this.fileType);
            
            // Handle null documentType for old documents
            if (document.getDocumentType() != null) {
                this.documentType = document.getDocumentType().name();
                System.out.println("=== DEBUG DocumentResponse: documentType = " + this.documentType);
            } else {
                System.out.println("=== DEBUG DocumentResponse: WARNING - documentType is NULL, using OTHER");
                this.documentType = Document.DocumentType.OTHER.name();
            }
            
            this.description = document.getDescription();
            System.out.println("=== DEBUG DocumentResponse: description = " + this.description);
            
            // Handle null project (shouldn't happen, but safety check)
            if (document.getProject() != null) {
                this.projectId = document.getProject().getId();
                this.projectName = document.getProject().getProjectName();
                System.out.println("=== DEBUG DocumentResponse: projectId = " + this.projectId + ", projectName = " + this.projectName);
            } else {
                System.out.println("=== DEBUG DocumentResponse: WARNING - project is NULL");
                this.projectId = null;
                this.projectName = null;
            }
            
            if (document.getFolder() != null) {
                this.folderId = document.getFolder().getId();
                this.folderName = document.getFolder().getFolderName();
                System.out.println("=== DEBUG DocumentResponse: folderId = " + this.folderId + ", folderName = " + this.folderName);
            } else {
                this.folderId = null;
                this.folderName = null;
                System.out.println("=== DEBUG DocumentResponse: folder is null");
            }
            
            this.uploadedBy = document.getUploadedBy();
            System.out.println("=== DEBUG DocumentResponse: uploadedBy = " + this.uploadedBy);
            
            this.uploadedAt = document.getUploadedAt();
            System.out.println("=== DEBUG DocumentResponse: uploadedAt = " + this.uploadedAt);
            
            System.out.println("=== DEBUG DocumentResponse: Successfully created response for document ID: " + document.getId());
        } catch (Exception e) {
            System.err.println("=== ERROR DocumentResponse: Exception creating response for document ID: " + document.getId());
            System.err.println("=== ERROR DocumentResponse: Exception type: " + e.getClass().getName());
            System.err.println("=== ERROR DocumentResponse: Exception message: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to be caught by controller
        }
    }
}
