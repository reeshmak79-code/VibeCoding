package com.trialsite.dto;

import com.trialsite.model.DocumentSignature;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SignatureResponse {
    private Long id;
    private Long documentId;
    private String documentName;
    private Long assignedToUserId;
    private String assignedToUserName;
    private String assignedToUserEmail;
    private Long assignedByUserId;
    private String assignedByUserName;
    private String pandadocDocumentId;
    private DocumentSignature.SignatureStatus status;
    private String signingUrl;
    private String message;
    private LocalDateTime signedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public SignatureResponse(DocumentSignature signature) {
        this.id = signature.getId();
        this.documentId = signature.getDocument().getId();
        this.documentName = signature.getDocument().getOriginalFileName();
        this.assignedToUserId = signature.getAssignedToUser().getId();
        this.assignedToUserName = signature.getAssignedToUser().getFullName();
        this.assignedToUserEmail = signature.getAssignedToUser().getEmail();
        this.assignedByUserId = signature.getAssignedByUser().getId();
        this.assignedByUserName = signature.getAssignedByUser().getFullName();
        this.pandadocDocumentId = signature.getPandadocDocumentId();
        this.status = signature.getStatus();
        this.signingUrl = signature.getSigningUrl();
        this.message = signature.getMessage();
        this.signedAt = signature.getSignedAt();
        this.createdAt = signature.getCreatedAt();
        this.updatedAt = signature.getUpdatedAt();
    }
}
