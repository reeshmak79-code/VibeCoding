package com.trialsite.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SignatureAssignmentRequest {
    @NotNull(message = "Document ID is required")
    private Long documentId;
    
    @NotNull(message = "User ID to assign signature is required")
    private Long assignedToUserId;
    
    private String message; // Optional message for the signer
}
