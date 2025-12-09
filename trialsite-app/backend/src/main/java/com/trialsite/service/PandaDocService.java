package com.trialsite.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class PandaDocService {
    
    @Value("${pandadoc.api.key}")
    private String apiKey;
    
    @Value("${pandadoc.api.url}")
    private String apiUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Upload document to PandaDoc and create a signing request
     * @param documentPath Path to the document file
     * @param documentName Original document name
     * @param recipientEmail Email of the person who needs to sign
     * @param recipientName Name of the person who needs to sign
     * @param message Optional message for the signer
     * @return PandaDoc document ID and signing URL
     */
    public Map<String, String> createDocumentForSigning(
            String documentPath,
            String documentName,
            String recipientEmail,
            String recipientName,
            String message) throws Exception {
        
        System.out.println("=== DEBUG PandaDocService: createDocumentForSigning called");
        System.out.println("=== DEBUG PandaDocService: documentPath: " + documentPath);
        System.out.println("=== DEBUG PandaDocService: documentName: " + documentName);
        System.out.println("=== DEBUG PandaDocService: recipientEmail: " + recipientEmail);
        System.out.println("=== DEBUG PandaDocService: recipientName: " + recipientName);
        
        try {
            // Use single-step approach: Upload file and create document in one request
            System.out.println("=== DEBUG PandaDocService: Creating document with file upload in single request");
            String pandadocDocId = createDocumentWithFileUpload(documentPath, documentName, recipientEmail, recipientName, message);
            System.out.println("=== DEBUG PandaDocService: Document created, PandaDoc ID: " + pandadocDocId);
            
            // Skip waiting - PandaDoc documents can be sent even if still processing
            // The document will be processed asynchronously by PandaDoc
            System.out.println("=== DEBUG PandaDocService: Skipping wait - proceeding to send document");
            
            // Try to send for signing immediately
            // If document isn't ready, we'll get the URL when user clicks "Sign Now"
            System.out.println("=== DEBUG PandaDocService: Sending for signing");
            String signingUrl = null;
            try {
                signingUrl = sendForSigning(pandadocDocId);
                System.out.println("=== DEBUG PandaDocService: Signing URL: " + signingUrl);
            } catch (Exception e) {
                System.err.println("=== WARN PandaDocService: Could not send immediately, but document is created: " + e.getMessage());
                // Document is created, but not ready yet (409 error or URL not available)
                // Signing URL will be null - we'll fetch it when user clicks "Sign Now"
                // This is fine - the document will be processed and URL will be available shortly
                signingUrl = null;
            }
            
            Map<String, String> result = new HashMap<>();
            result.put("pandadocDocumentId", pandadocDocId);
            result.put("signingUrl", signingUrl);
            
            System.out.println("=== DEBUG PandaDocService: Successfully completed all steps");
            return result;
        } catch (Exception e) {
            System.err.println("=== ERROR PandaDocService: Exception in createDocumentForSigning");
            System.err.println("=== ERROR PandaDocService: Exception type: " + e.getClass().getName());
            System.err.println("=== ERROR PandaDocService: Exception message: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Create a document with file upload in a single request (PandaDoc recommended approach)
     */
    private String createDocumentWithFileUpload(
            String filePath,
            String documentName,
            String recipientEmail,
            String recipientName,
            String message) throws Exception {
        
        System.out.println("=== DEBUG PandaDocService.createDocumentWithFileUpload: Starting");
        System.out.println("=== DEBUG PandaDocService.createDocumentWithFileUpload: filePath: " + filePath);
        System.out.println("=== DEBUG PandaDocService.createDocumentWithFileUpload: documentName: " + documentName);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", "API-Key " + apiKey);
        
        // Resolve file path
        File file;
        if (Paths.get(filePath).isAbsolute()) {
            file = new File(filePath);
            System.out.println("=== DEBUG PandaDocService.createDocumentWithFileUpload: Using absolute path");
        } else {
            String workingDir = System.getProperty("user.dir");
            System.out.println("=== DEBUG PandaDocService.createDocumentWithFileUpload: Working dir: " + workingDir);
            if (workingDir.endsWith("backend")) {
                file = new File(workingDir, filePath);
            } else {
                file = new File(workingDir, "backend/" + filePath);
            }
        }
        
        System.out.println("=== DEBUG PandaDocService.createDocumentWithFileUpload: Resolved file path: " + file.getAbsolutePath());
        
        if (!file.exists()) {
            System.err.println("=== ERROR PandaDocService.createDocumentWithFileUpload: File not found: " + file.getAbsolutePath());
            throw new RuntimeException("File not found: " + file.getAbsolutePath());
        }
        
        if (!file.isFile()) {
            System.err.println("=== ERROR PandaDocService.createDocumentWithFileUpload: Path is not a file: " + file.getAbsolutePath());
            throw new RuntimeException("Path is not a file: " + file.getAbsolutePath());
        }
        
        FileSystemResource resource = new FileSystemResource(file);
        
        // Build recipients array
        Map<String, Object> recipient = new HashMap<>();
        recipient.put("email", recipientEmail);
        recipient.put("first_name", recipientName.split(" ")[0]);
        recipient.put("last_name", recipientName.split(" ").length > 1 ? 
                recipientName.substring(recipientName.indexOf(" ") + 1) : "");
        recipient.put("role", "signer");
        
        java.util.List<Map<String, Object>> recipientsList = new java.util.ArrayList<>();
        recipientsList.add(recipient);
        
        // Build JSON data payload
        Map<String, Object> dataPayload = new HashMap<>();
        dataPayload.put("name", documentName);
        dataPayload.put("recipients", recipientsList);
        if (message != null && !message.isEmpty()) {
            dataPayload.put("message", message);
        }
        
        // Convert data payload to JSON string
        String dataJson = objectMapper.writeValueAsString(dataPayload);
        System.out.println("=== DEBUG PandaDocService.createDocumentWithFileUpload: Data payload: " + dataJson);
        
        // Wrap JSON string in HttpEntity with proper content type for multipart form
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> jsonEntity = new HttpEntity<>(dataJson, jsonHeaders);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("data", jsonEntity);
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        System.out.println("=== DEBUG PandaDocService.createDocumentWithFileUpload: Making POST request to: " + apiUrl + "/documents");
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl + "/documents",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        
        System.out.println("=== DEBUG PandaDocService.createDocumentWithFileUpload: Response status: " + response.getStatusCode());
        System.out.println("=== DEBUG PandaDocService.createDocumentWithFileUpload: Response body: " + response.getBody());
        
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String docId = jsonNode.get("id").asText();
            System.out.println("=== DEBUG PandaDocService.createDocumentWithFileUpload: Document created, ID: " + docId);
            return docId;
        } else {
            // Parse error response
            String errorMessage = "Failed to create document in PandaDoc";
            try {
                JsonNode errorNode = objectMapper.readTree(response.getBody());
                System.err.println("=== ERROR PandaDocService.createDocumentWithFileUpload: Error response: " + response.getBody());
                if (errorNode.has("detail") && errorNode.get("detail").has("message")) {
                    errorMessage = errorNode.get("detail").get("message").asText();
                    if (errorNode.has("detail") && errorNode.get("detail").has("code")) {
                        String errorCode = errorNode.get("detail").get("code").asText();
                        System.err.println("=== ERROR PandaDocService.createDocumentWithFileUpload: Error code: " + errorCode);
                        if ("email-not-verified".equals(errorCode)) {
                            errorMessage = "Recipient email is not verified in PandaDoc. Please verify the email address in your PandaDoc account settings.";
                        }
                    }
                } else if (errorNode.has("message")) {
                    errorMessage = errorNode.get("message").asText();
                }
            } catch (Exception e) {
                System.err.println("=== ERROR PandaDocService.createDocumentWithFileUpload: Failed to parse error: " + e.getMessage());
                errorMessage = response.getBody();
            }
            throw new RuntimeException(errorMessage + " (Status: " + response.getStatusCode() + ")");
        }
    }
    
    /**
     * Upload a file to PandaDoc (deprecated - use createDocumentWithFileUpload instead)
     */
    private String uploadDocument(String filePath, String fileName) throws Exception {
        System.out.println("=== DEBUG PandaDocService.uploadDocument: Starting upload");
        System.out.println("=== DEBUG PandaDocService.uploadDocument: filePath: " + filePath);
        System.out.println("=== DEBUG PandaDocService.uploadDocument: fileName: " + fileName);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", "API-Key " + apiKey);
        
        // Resolve file path - handle both absolute and relative paths
        File file;
        if (Paths.get(filePath).isAbsolute()) {
            file = new File(filePath);
            System.out.println("=== DEBUG PandaDocService.uploadDocument: Using absolute path");
        } else {
            // If relative, resolve against current working directory or project root
            String workingDir = System.getProperty("user.dir");
            System.out.println("=== DEBUG PandaDocService.uploadDocument: Working dir: " + workingDir);
            // Check if we're in backend directory or root
            if (workingDir.endsWith("backend")) {
                file = new File(workingDir, filePath);
            } else {
                // Assume we're in project root
                file = new File(workingDir, "backend/" + filePath);
            }
        }
        
        System.out.println("=== DEBUG PandaDocService.uploadDocument: Resolved file path: " + file.getAbsolutePath());
        
        // Verify file exists
        if (!file.exists()) {
            System.err.println("=== ERROR PandaDocService.uploadDocument: File not found: " + file.getAbsolutePath());
            throw new RuntimeException("File not found: " + file.getAbsolutePath() + 
                    " (original path: " + filePath + ")");
        }
        
        if (!file.isFile()) {
            System.err.println("=== ERROR PandaDocService.uploadDocument: Path is not a file: " + file.getAbsolutePath());
            throw new RuntimeException("Path is not a file: " + file.getAbsolutePath());
        }
        
        System.out.println("=== DEBUG PandaDocService.uploadDocument: File verified, creating resource");
        FileSystemResource resource = new FileSystemResource(file);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("name", fileName);
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        System.out.println("=== DEBUG PandaDocService.uploadDocument: Making POST request to: " + apiUrl + "/documents");
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl + "/documents",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        
        System.out.println("=== DEBUG PandaDocService.uploadDocument: Response status: " + response.getStatusCode());
        System.out.println("=== DEBUG PandaDocService.uploadDocument: Response body: " + response.getBody());
        
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String fileId = jsonNode.get("id").asText();
            System.out.println("=== DEBUG PandaDocService.uploadDocument: Upload successful, file ID: " + fileId);
            return fileId;
        } else {
            // Parse error response for better error messages
            String errorMessage = "Failed to upload document to PandaDoc";
            try {
                JsonNode errorNode = objectMapper.readTree(response.getBody());
                System.err.println("=== ERROR PandaDocService.uploadDocument: Error response: " + response.getBody());
                if (errorNode.has("detail") && errorNode.get("detail").has("message")) {
                    errorMessage = errorNode.get("detail").get("message").asText();
                } else if (errorNode.has("message")) {
                    errorMessage = errorNode.get("message").asText();
                }
            } catch (Exception e) {
                // If parsing fails, use raw response
                errorMessage = response.getBody();
            }
            throw new RuntimeException(errorMessage + " (Status: " + response.getStatusCode() + ")");
        }
    }
    
    /**
     * Create a document from uploaded file and add recipient
     */
    private String createDocumentFromFile(
            String fileId,
            String documentName,
            String recipientEmail,
            String recipientName,
            String message) throws Exception {
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "API-Key " + apiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", documentName);
        requestBody.put("file_id", fileId);
        
        // Add recipient
        Map<String, Object> recipient = new HashMap<>();
        recipient.put("email", recipientEmail);
        recipient.put("first_name", recipientName.split(" ")[0]);
        recipient.put("last_name", recipientName.split(" ").length > 1 ? 
                recipientName.substring(recipientName.indexOf(" ") + 1) : "");
        recipient.put("role", "signer");
        
        // PandaDoc API expects recipients as a List, not an array
        java.util.List<Map<String, Object>> recipientsList = new java.util.ArrayList<>();
        recipientsList.add(recipient);
        requestBody.put("recipients", recipientsList);
        
        if (message != null && !message.isEmpty()) {
            requestBody.put("message", message);
        }
        
        System.out.println("=== DEBUG PandaDocService.createDocumentFromFile: Request body: " + requestBody);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        System.out.println("=== DEBUG PandaDocService.createDocumentFromFile: Making POST request to: " + apiUrl + "/documents");
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl + "/documents",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        
        System.out.println("=== DEBUG PandaDocService.createDocumentFromFile: Response status: " + response.getStatusCode());
        System.out.println("=== DEBUG PandaDocService.createDocumentFromFile: Response body: " + response.getBody());
        
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String docId = jsonNode.get("id").asText();
            System.out.println("=== DEBUG PandaDocService.createDocumentFromFile: Document created, ID: " + docId);
            return docId;
        } else {
            // Parse error response for better error messages
            String errorMessage = "Failed to create document in PandaDoc";
            try {
                JsonNode errorNode = objectMapper.readTree(response.getBody());
                System.err.println("=== ERROR PandaDocService.createDocumentFromFile: Error response: " + response.getBody());
                if (errorNode.has("detail") && errorNode.get("detail").has("message")) {
                    errorMessage = errorNode.get("detail").get("message").asText();
                    // Check for specific error codes
                    if (errorNode.has("detail") && errorNode.get("detail").has("code")) {
                        String errorCode = errorNode.get("detail").get("code").asText();
                        System.err.println("=== ERROR PandaDocService.createDocumentFromFile: Error code: " + errorCode);
                        if ("email-not-verified".equals(errorCode)) {
                            errorMessage = "Recipient email is not verified in PandaDoc. Please verify the email address in your PandaDoc account settings.";
                        }
                    }
                } else if (errorNode.has("message")) {
                    errorMessage = errorNode.get("message").asText();
                }
            } catch (Exception e) {
                System.err.println("=== ERROR PandaDocService.createDocumentFromFile: Failed to parse error: " + e.getMessage());
                // If parsing fails, use raw response
                errorMessage = response.getBody();
            }
            throw new RuntimeException(errorMessage + " (Status: " + response.getStatusCode() + ")");
        }
    }
    
    /**
     * Wait for document to be processed (poll until status is ready)
     */
    private void waitForDocumentReady(String pandadocDocumentId) throws Exception {
        System.out.println("=== DEBUG PandaDocService.waitForDocumentReady: Starting to wait for document: " + pandadocDocumentId);
        
        int maxAttempts = 10; // Maximum 10 attempts (30 seconds with 3 second intervals)
        int attempt = 0;
        long pollInterval = 3000; // Poll every 3 seconds to avoid rate limiting
        
        while (attempt < maxAttempts) {
            attempt++;
            System.out.println("=== DEBUG PandaDocService.waitForDocumentReady: Attempt " + attempt + "/" + maxAttempts);
            
            try {
                String status = getDocumentStatus(pandadocDocumentId);
                System.out.println("=== DEBUG PandaDocService.waitForDocumentReady: Document status: " + status);
                
                // Document is ready when status is "document.draft" or "document.sent"
                if ("document.draft".equals(status) || "document.sent".equals(status)) {
                    System.out.println("=== DEBUG PandaDocService.waitForDocumentReady: Document is ready!");
                    return;
                }
                
                // If status is "document.uploaded", continue waiting
                if ("document.uploaded".equals(status)) {
                    System.out.println("=== DEBUG PandaDocService.waitForDocumentReady: Document still processing, waiting...");
                    Thread.sleep(pollInterval);
                    continue;
                }
                
                // If status is an error state, throw exception
                if (status != null && status.startsWith("document.error")) {
                    throw new RuntimeException("Document processing failed with status: " + status);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for document to be ready", e);
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                // Handle rate limiting (429) - wait longer before retrying
                if (errorMessage != null && errorMessage.contains("429")) {
                    System.err.println("=== WARN PandaDocService.waitForDocumentReady: Rate limited, waiting longer...");
                    // Extract wait time from error if available, otherwise wait 5 seconds
                    long waitTime = 5000; // Default 5 seconds
                    try {
                        // Try to extract wait time from error message
                        if (errorMessage.contains("Expected available in")) {
                            // Parse "Expected available in X seconds"
                            int startIdx = errorMessage.indexOf("Expected available in") + "Expected available in".length();
                            int endIdx = errorMessage.indexOf(" seconds", startIdx);
                            if (endIdx > startIdx) {
                                String waitSeconds = errorMessage.substring(startIdx, endIdx).trim();
                                waitTime = Long.parseLong(waitSeconds) * 1000 + 1000; // Add 1 second buffer
                            }
                        }
                    } catch (Exception parseEx) {
                        // If parsing fails, use default
                    }
                    
                    if (attempt < maxAttempts) {
                        try {
                            System.out.println("=== DEBUG PandaDocService.waitForDocumentReady: Waiting " + (waitTime/1000) + " seconds due to rate limit...");
                            Thread.sleep(waitTime);
                            continue;
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting", ie);
                        }
                    } else {
                        throw new RuntimeException("Rate limited and exceeded max attempts");
                    }
                } else {
                    // For other errors, log and continue (might be transient)
                    System.err.println("=== WARN PandaDocService.waitForDocumentReady: Error checking status: " + errorMessage);
                    if (attempt < maxAttempts) {
                        try {
                            Thread.sleep(pollInterval);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting", ie);
                        }
                        continue;
                    } else {
                        throw e;
                    }
                }
            }
        }
        
        // If we've exhausted all attempts, log a warning but don't throw - let the calling code decide
        System.out.println("=== WARN PandaDocService.waitForDocumentReady: Document did not become ready within " + (maxAttempts * pollInterval / 1000) + " seconds, but status is: " + 
                (attempt > 0 ? "still processing" : "unknown"));
        // Don't throw - let the calling code proceed anyway
        // The document might still work even if status hasn't changed yet
    }
    
    /**
     * Get document status from PandaDoc
     */
    private String getDocumentStatus(String pandadocDocumentId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "API-Key " + apiKey);
        
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        
        System.out.println("=== DEBUG PandaDocService.getDocumentStatus: Getting status for document: " + pandadocDocumentId);
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl + "/documents/" + pandadocDocumentId,
                HttpMethod.GET,
                requestEntity,
                String.class
        );
        
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String status = jsonNode.has("status") ? jsonNode.get("status").asText() : null;
            System.out.println("=== DEBUG PandaDocService.getDocumentStatus: Status retrieved: " + status);
            return status;
        } else {
            throw new RuntimeException("Failed to get document status: " + response.getStatusCode());
        }
    }
    
    /**
     * Send document for signing and get signing URL
     */
    private String sendForSigning(String pandadocDocumentId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "API-Key " + apiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", "Please review and sign this document");
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        System.out.println("=== DEBUG PandaDocService.sendForSigning: Making POST request to: " + apiUrl + "/documents/" + pandadocDocumentId + "/send");
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl + "/documents/" + pandadocDocumentId + "/send",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        
        System.out.println("=== DEBUG PandaDocService.sendForSigning: Response status: " + response.getStatusCode());
        System.out.println("=== DEBUG PandaDocService.sendForSigning: Response body: " + response.getBody());
        
        if (response.getStatusCode().is2xxSuccessful()) {
            // After sending, we can't get signing URL here because we don't have recipient email
            // The signing URL will be fetched when user clicks "Sign Now"
            // Return null to indicate it needs to be fetched later
            System.out.println("=== DEBUG PandaDocService.sendForSigning: Document sent successfully, signing URL will be fetched on demand");
            return null;
        } else {
            // Check if it's a 409 Conflict (document not ready yet)
            if (response.getStatusCode().value() == 409) {
                System.out.println("=== WARN PandaDocService.sendForSigning: Document not ready yet (409), but will be processed.");
                // Document is created but not ready - return null so we know to fetch it later
                // Don't return a placeholder URL that will be used incorrectly
                throw new RuntimeException("Document is still being processed. Signing URL will be available shortly.");
            }
            throw new RuntimeException("Failed to send document for signing: " + response.getBody());
        }
    }
    
    /**
     * Get signing URL for a document
     * Note: This creates a signing session and returns the URL
     * The actual URL format may vary based on PandaDoc API version
     */
    public String getSigningUrl(String pandadocDocumentId, String recipientEmail) throws Exception {
        System.out.println("=== DEBUG PandaDocService.getSigningUrl: Getting signing URL for document: " + pandadocDocumentId);
        System.out.println("=== DEBUG PandaDocService.getSigningUrl: Recipient email: " + recipientEmail);
        
        // First, check document status - if still uploading, wait and retry
        String status = null;
        int maxRetries = 5;
        int retryCount = 0;
        long waitInterval = 3000; // 3 seconds
        
        while (retryCount < maxRetries) {
            try {
                status = getDocumentStatus(pandadocDocumentId);
                System.out.println("=== DEBUG PandaDocService.getSigningUrl: Document status (attempt " + (retryCount + 1) + "): " + status);
                
                // If document is ready (draft or sent), break out of loop
                if ("document.draft".equals(status) || "document.sent".equals(status)) {
                    System.out.println("=== DEBUG PandaDocService.getSigningUrl: Document is ready!");
                    break;
                }
                
                // If still uploading, wait and retry
                if ("document.uploaded".equals(status)) {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        System.out.println("=== DEBUG PandaDocService.getSigningUrl: Document still processing, waiting " + (waitInterval/1000) + " seconds...");
                        Thread.sleep(waitInterval);
                    }
                } else {
                    // Unknown status, break and proceed
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for document to be ready", e);
            } catch (Exception e) {
                System.err.println("=== WARN PandaDocService.getSigningUrl: Could not check status: " + e.getMessage());
                // If we can't check status, proceed anyway
                break;
            }
        }
        
        if ("document.uploaded".equals(status) && retryCount >= maxRetries) {
            System.out.println("=== WARN PandaDocService.getSigningUrl: Document still processing after " + maxRetries + " attempts, but proceeding anyway");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "API-Key " + apiKey);
        
        // Create a signing session - PandaDoc requires recipient email
        Map<String, Object> requestBody = new HashMap<>();
        if (recipientEmail != null && !recipientEmail.isEmpty()) {
            requestBody.put("recipient", recipientEmail);
        }
        
        System.out.println("=== DEBUG PandaDocService.getSigningUrl: Request body: " + requestBody);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        System.out.println("=== DEBUG PandaDocService.getSigningUrl: Making POST request to: " + apiUrl + "/documents/" + pandadocDocumentId + "/session");
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                    apiUrl + "/documents/" + pandadocDocumentId + "/session",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Handle 409 or other errors
            System.err.println("=== ERROR PandaDocService.getSigningUrl: HTTP error: " + e.getStatusCode() + ", Body: " + e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 409) {
                // Document still processing - provide helpful message
                throw new RuntimeException("Document is still being processed by PandaDoc. Please wait a moment and try again. The document will be ready shortly.");
            }
            throw new RuntimeException("Failed to create signing session: " + e.getResponseBodyAsString());
        }
        
        System.out.println("=== DEBUG PandaDocService.getSigningUrl: Response status: " + response.getStatusCode());
        System.out.println("=== DEBUG PandaDocService.getSigningUrl: Response body: " + response.getBody());
        
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            // PandaDoc returns session info - construct the signing URL
            // Format may be: https://app.pandadoc.com/s/{sessionId}
            // Or it may be in the response directly
            String signingUrl = null;
            if (jsonNode.has("url")) {
                signingUrl = jsonNode.get("url").asText();
            } else if (jsonNode.has("id")) {
                String sessionId = jsonNode.get("id").asText();
                signingUrl = "https://app.pandadoc.com/s/" + sessionId;
            } else if (jsonNode.has("session_url")) {
                signingUrl = jsonNode.get("session_url").asText();
            } else {
                // Try to get any URL field
                java.util.Iterator<String> fieldNames = jsonNode.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    if (fieldName.toLowerCase().contains("url") || fieldName.toLowerCase().contains("link")) {
                        signingUrl = jsonNode.get(fieldName).asText();
                        break;
                    }
                }
            }
            
            if (signingUrl == null || signingUrl.isEmpty()) {
                System.err.println("=== ERROR PandaDocService.getSigningUrl: Could not extract signing URL from response: " + response.getBody());
                throw new RuntimeException("Could not extract signing URL from PandaDoc response. Response: " + response.getBody());
            }
            
            System.out.println("=== DEBUG PandaDocService.getSigningUrl: Signing URL: " + signingUrl);
            return signingUrl;
        } else {
            String errorBody = response.getBody();
            System.err.println("=== ERROR PandaDocService.getSigningUrl: Failed to get signing URL. Status: " + response.getStatusCode() + ", Body: " + errorBody);
            throw new RuntimeException("Failed to get signing URL: " + errorBody);
        }
    }
    
    /**
     * Check document status
     */
    public String checkDocumentStatus(String pandadocDocumentId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "API-Key " + apiKey);
        
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl + "/documents/" + pandadocDocumentId,
                HttpMethod.GET,
                requestEntity,
                String.class
        );
        
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("status").asText();
        } else {
            throw new RuntimeException("Failed to check document status: " + response.getBody());
        }
    }
    
    /**
     * Download signed document
     */
    public byte[] downloadSignedDocument(String pandadocDocumentId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "API-Key " + apiKey);
        headers.setAccept(java.util.Arrays.asList(MediaType.APPLICATION_PDF, MediaType.APPLICATION_OCTET_STREAM));
        
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        
        ResponseEntity<byte[]> response = restTemplate.exchange(
                apiUrl + "/documents/" + pandadocDocumentId + "/download",
                HttpMethod.GET,
                requestEntity,
                byte[].class
        );
        
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to download signed document");
        }
    }
}
