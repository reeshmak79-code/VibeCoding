package com.trialsite.controller;

import com.trialsite.dto.MessageResponse;
import com.trialsite.dto.SignatureAssignmentRequest;
import com.trialsite.dto.SignatureResponse;
import com.trialsite.model.Document;
import com.trialsite.model.DocumentSignature;
import com.trialsite.model.User;
import com.trialsite.repository.DocumentRepository;
import com.trialsite.repository.DocumentSignatureRepository;
import com.trialsite.repository.UserRepository;
import com.trialsite.service.PandaDocService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/signatures")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SignatureController {
    
    @Autowired
    private DocumentSignatureRepository signatureRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PandaDocService pandaDocService;
    
    /**
     * Admin assigns a document for signing
     */
    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<?> assignDocumentForSigning(
            @Valid @RequestBody SignatureAssignmentRequest request,
            Authentication authentication) {
        try {
            // Get current user (admin/doctor assigning)
            User assignedBy = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get document
            Document document = documentRepository.findById(request.getDocumentId())
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            
            // Get user to assign signature to
            User assignedTo = userRepository.findById(request.getAssignedToUserId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            
            // Check if signature request already exists
            List<DocumentSignature> existing = signatureRepository.findByDocumentIdAndAssignedToUserId(
                    request.getDocumentId(), request.getAssignedToUserId());
            if (!existing.isEmpty()) {
                DocumentSignature existingSig = existing.stream()
                        .filter(s -> s.getStatus() == DocumentSignature.SignatureStatus.PENDING ||
                                   s.getStatus() == DocumentSignature.SignatureStatus.SENT ||
                                   s.getStatus() == DocumentSignature.SignatureStatus.VIEWED)
                        .findFirst()
                        .orElse(null);
                if (existingSig != null) {
                    return ResponseEntity.badRequest()
                            .body(new MessageResponse("A pending signature request already exists for this document and user."));
                }
            }
            
            // Create signature record
            DocumentSignature signature = new DocumentSignature();
            signature.setDocument(document);
            signature.setAssignedToUser(assignedTo);
            signature.setAssignedByUser(assignedBy);
            signature.setStatus(DocumentSignature.SignatureStatus.PENDING);
            signature.setMessage(request.getMessage());
            
            // Save first to get ID
            signature = signatureRepository.save(signature);
            
            // Upload to PandaDoc and create signing request
            try {
                System.out.println("=== DEBUG SignatureController: Starting PandaDoc upload");
                System.out.println("=== DEBUG SignatureController: Document ID: " + document.getId());
                System.out.println("=== DEBUG SignatureController: Document path: " + document.getFilePath());
                System.out.println("=== DEBUG SignatureController: Recipient email: " + assignedTo.getEmail());
                System.out.println("=== DEBUG SignatureController: Recipient name: " + assignedTo.getFullName());
                
                String documentPath = document.getFilePath();
                
                // Verify file exists before attempting upload
                java.nio.file.Path filePath = Paths.get(documentPath);
                if (!Paths.get(documentPath).isAbsolute()) {
                    // If relative path, resolve against backend directory
                    String workingDir = System.getProperty("user.dir");
                    System.out.println("=== DEBUG SignatureController: Working directory: " + workingDir);
                    if (workingDir.endsWith("backend")) {
                        filePath = Paths.get(workingDir, documentPath);
                    } else {
                        filePath = Paths.get(workingDir, "backend", documentPath);
                    }
                }
                
                System.out.println("=== DEBUG SignatureController: Resolved file path: " + filePath.toAbsolutePath());
                
                if (!java.nio.file.Files.exists(filePath)) {
                    System.err.println("=== ERROR SignatureController: File not found at: " + filePath.toAbsolutePath());
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new MessageResponse("Document file not found: " + filePath.toAbsolutePath()));
                }
                
                System.out.println("=== DEBUG SignatureController: File exists, calling PandaDoc service");
                Map<String, String> pandaDocResult = pandaDocService.createDocumentForSigning(
                        filePath.toString(),
                        document.getOriginalFileName(),
                        assignedTo.getEmail(),
                        assignedTo.getFullName(),
                        request.getMessage()
                );
                System.out.println("=== DEBUG SignatureController: PandaDoc result: " + pandaDocResult);
                
                // Update signature with PandaDoc info
                signature.setPandadocDocumentId(pandaDocResult.get("pandadocDocumentId"));
                String signingUrl = pandaDocResult.get("signingUrl");
                if (signingUrl != null && !signingUrl.isEmpty()) {
                    signature.setSigningUrl(signingUrl);
                }
                // Status is SENT if we got a URL, otherwise PENDING (will be sent when ready)
                if (signingUrl != null && !signingUrl.isEmpty()) {
                    signature.setStatus(DocumentSignature.SignatureStatus.SENT);
                } else {
                    signature.setStatus(DocumentSignature.SignatureStatus.PENDING);
                }
                signature = signatureRepository.save(signature);
                
                return ResponseEntity.ok(new SignatureResponse(signature));
            } catch (Exception e) {
                // If PandaDoc fails, mark as error but keep the record
                System.err.println("=== ERROR SignatureController: Exception in PandaDoc upload");
                System.err.println("=== ERROR SignatureController: Exception type: " + e.getClass().getName());
                System.err.println("=== ERROR SignatureController: Exception message: " + e.getMessage());
                e.printStackTrace();
                
                signature.setStatus(DocumentSignature.SignatureStatus.CANCELLED);
                signatureRepository.save(signature);
                
                // Provide user-friendly error messages
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("email-not-verified")) {
                    errorMessage = "The recipient's email address is not verified in PandaDoc. " +
                            "Please verify the email address in your PandaDoc account settings before sending signature requests.";
                } else if (errorMessage != null && errorMessage.contains("403")) {
                    errorMessage = "Access denied by PandaDoc. Please check your API key permissions and account settings.";
                }
                
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new MessageResponse("Failed to create signing request in PandaDoc: " + errorMessage));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error assigning document for signing: " + e.getMessage()));
        }
    }
    
    /**
     * Get all pending signatures for the current user
     */
    @GetMapping("/pending")
    public ResponseEntity<List<SignatureResponse>> getPendingSignatures(Authentication authentication) {
        try {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<DocumentSignature> signatures = signatureRepository.findByAssignedToUserIdAndStatusIn(
                    user.getId(),
                    List.of(
                            DocumentSignature.SignatureStatus.PENDING,
                            DocumentSignature.SignatureStatus.SENT,
                            DocumentSignature.SignatureStatus.VIEWED
                    )
            );
            
            List<SignatureResponse> response = signatures.stream()
                    .map(SignatureResponse::new)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get signing URL for a signature request
     */
    @GetMapping("/{id}/sign-url")
    public ResponseEntity<?> getSigningUrl(@PathVariable Long id, Authentication authentication) {
        try {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            DocumentSignature signature = signatureRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Signature request not found"));
            
            // Verify user is assigned to this signature
            if (!signature.getAssignedToUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Access denied. You are not assigned to sign this document."));
            }
            
            // Always try to get a fresh signing URL (don't use stored placeholder URLs)
            String signingUrl = signature.getSigningUrl();
            boolean isPlaceholder = signingUrl != null && signingUrl.contains("/document/") && !signingUrl.contains("/s/");
            
            // If no URL or it's a placeholder, get a fresh one
            if (signingUrl == null || signingUrl.isEmpty() || isPlaceholder) {
                if (signature.getPandadocDocumentId() != null) {
                    try {
                        // Get recipient email from the assigned user
                        String recipientEmail = signature.getAssignedToUser().getEmail();
                        System.out.println("=== DEBUG SignatureController: Getting signing URL for recipient: " + recipientEmail);
                        signingUrl = pandaDocService.getSigningUrl(signature.getPandadocDocumentId(), recipientEmail);
                        signature.setSigningUrl(signingUrl);
                        signatureRepository.save(signature);
                        System.out.println("=== DEBUG SignatureController: Got fresh signing URL: " + signingUrl);
                    } catch (Exception e) {
                        System.err.println("=== ERROR SignatureController: Failed to get signing URL: " + e.getMessage());
                        // If document isn't ready yet, return a helpful message
                        if (e.getMessage() != null && (e.getMessage().contains("409") || e.getMessage().contains("not ready"))) {
                            return ResponseEntity.status(HttpStatus.ACCEPTED)
                                    .body(new MessageResponse("Document is still being processed by PandaDoc. Please try again in a few moments."));
                        }
                        return ResponseEntity.badRequest()
                                .body(new MessageResponse("Error getting signing URL: " + e.getMessage()));
                    }
                } else {
                    return ResponseEntity.badRequest()
                            .body(new MessageResponse("Signing URL not available. Please contact administrator."));
                }
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("signingUrl", signingUrl);
            response.put("pandadocDocumentId", signature.getPandadocDocumentId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error getting signing URL: " + e.getMessage()));
        }
    }
    
    /**
     * Get all signatures for a document (Admin/Doctor only)
     */
    @GetMapping("/document/{documentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<List<SignatureResponse>> getDocumentSignatures(@PathVariable Long documentId) {
        try {
            List<DocumentSignature> signatures = signatureRepository.findByDocumentId(documentId);
            List<SignatureResponse> response = signatures.stream()
                    .map(SignatureResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Webhook endpoint for PandaDoc status updates
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> webhookData) {
        try {
            // Extract webhook data
            String event = (String) webhookData.get("event");
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");
            String documentId = (String) data.get("id");
            
            // Find signature by PandaDoc document ID
            DocumentSignature signature = signatureRepository.findByPandadocDocumentId(documentId)
                    .orElse(null);
            
            if (signature == null) {
                return ResponseEntity.ok().build(); // Ignore if not found
            }
            
            // Update status based on event
            switch (event) {
                case "document.viewed":
                    signature.setStatus(DocumentSignature.SignatureStatus.VIEWED);
                    break;
                case "document.completed":
                    signature.setStatus(DocumentSignature.SignatureStatus.SIGNED);
                    signature.setSignedAt(java.time.LocalDateTime.now());
                    // Optionally download signed document here
                    break;
                case "document.declined":
                    signature.setStatus(DocumentSignature.SignatureStatus.DECLINED);
                    break;
                case "document.expired":
                    signature.setStatus(DocumentSignature.SignatureStatus.EXPIRED);
                    break;
            }
            
            signatureRepository.save(signature);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Log error but return 200 to prevent PandaDoc from retrying
            System.err.println("Error processing webhook: " + e.getMessage());
            return ResponseEntity.ok().build();
        }
    }
}
