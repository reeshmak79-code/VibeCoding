package com.trialsite.controller;

import com.trialsite.dto.DocumentResponse;
import com.trialsite.dto.MessageResponse;
import com.trialsite.model.Document;
import com.trialsite.model.Folder;
import com.trialsite.model.Project;
import com.trialsite.repository.DocumentRepository;
import com.trialsite.repository.FolderRepository;
import com.trialsite.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<List<DocumentResponse>> getProjectDocuments(@PathVariable Long projectId) {
        List<Document> documents = documentRepository.findByProjectId(projectId);
        List<DocumentResponse> response = documents.stream()
                .map(DocumentResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/folder/{folderId}")
    public ResponseEntity<List<DocumentResponse>> getFolderDocuments(@PathVariable Long folderId) {
        List<Document> documents = documentRepository.findByFolderId(folderId);
        List<DocumentResponse> response = documents.stream()
                .map(DocumentResponse::new)
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
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        try {
            Document document = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            
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
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            Document document = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            
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
    public ResponseEntity<?> getProjectDocumentStats(@PathVariable Long projectId) {
        List<Document> documents = documentRepository.findByProjectId(projectId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", documents.size());
        stats.put("totalSize", documents.stream().mapToLong(Document::getFileSize).sum());
        
        // Count by document type
        Map<String, Long> byType = new HashMap<>();
        for (Document.DocumentType type : Document.DocumentType.values()) {
            long count = documents.stream()
                    .filter(d -> d.getDocumentType() == type)
                    .count();
            byType.put(type.name(), count);
        }
        stats.put("byType", byType);
        
        return ResponseEntity.ok(stats);
    }
}
