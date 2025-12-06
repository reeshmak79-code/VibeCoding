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
        
        // Step 1: Upload the document file
        String documentId = uploadDocument(documentPath, documentName);
        
        // Step 2: Create a document from the uploaded file
        String pandadocDocId = createDocumentFromFile(documentId, documentName, recipientEmail, recipientName, message);
        
        // Step 3: Send for signing
        String signingUrl = sendForSigning(pandadocDocId);
        
        Map<String, String> result = new HashMap<>();
        result.put("pandadocDocumentId", pandadocDocId);
        result.put("signingUrl", signingUrl);
        
        return result;
    }
    
    /**
     * Upload a file to PandaDoc
     */
    private String uploadDocument(String filePath, String fileName) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", "API-Key " + apiKey);
        
        File file = new File(filePath);
        FileSystemResource resource = new FileSystemResource(file);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("name", fileName);
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl + "/documents",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("id").asText();
        } else {
            throw new RuntimeException("Failed to upload document to PandaDoc: " + response.getBody());
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
        
        requestBody.put("recipients", new Map[]{recipient});
        
        if (message != null && !message.isEmpty()) {
            requestBody.put("message", message);
        }
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl + "/documents",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("id").asText();
        } else {
            throw new RuntimeException("Failed to create document in PandaDoc: " + response.getBody());
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
        
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl + "/documents/" + pandadocDocumentId + "/send",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        
        if (response.getStatusCode().is2xxSuccessful()) {
            // After sending, get the signing session URL
            return getSigningUrl(pandadocDocumentId);
        } else {
            throw new RuntimeException("Failed to send document for signing: " + response.getBody());
        }
    }
    
    /**
     * Get signing URL for a document
     * Note: This creates a signing session and returns the URL
     * The actual URL format may vary based on PandaDoc API version
     */
    public String getSigningUrl(String pandadocDocumentId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "API-Key " + apiKey);
        
        // Create a signing session
        Map<String, Object> requestBody = new HashMap<>();
        // You may need to specify recipient email or other parameters based on PandaDoc API
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl + "/documents/" + pandadocDocumentId + "/session",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            // PandaDoc returns session info - construct the signing URL
            // Format may be: https://app.pandadoc.com/s/{sessionId}
            // Or it may be in the response directly
            if (jsonNode.has("id")) {
                String sessionId = jsonNode.get("id").asText();
                return "https://app.pandadoc.com/s/" + sessionId;
            } else if (jsonNode.has("url")) {
                return jsonNode.get("url").asText();
            } else {
                // Fallback: return the session ID and construct URL
                String sessionId = jsonNode.get("id").asText();
                return "https://app.pandadoc.com/s/" + sessionId;
            }
        } else {
            throw new RuntimeException("Failed to get signing URL: " + response.getBody());
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
