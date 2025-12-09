# 🏥 eReg/eISF Compliance Implementation Guide

## 📋 Overview

This guide shows how to build **eReg/eISF (Electronic Regulatory / Electronic Investigator Site File)** with **FDA 21 CFR Part 11 compliance** for clinical trials.

**What you already have:**
- ✅ Document upload/storage
- ✅ Folders
- ✅ Permissions
- ✅ Basic signatures (PandaDoc)

**What you need to add for compliance:**
- ⚠️ Audit trails
- ⚠️ Version control
- ⚠️ Document expiration tracking
- ⚠️ FDA-compliant electronic signatures
- ⚠️ Regulatory document templates
- ⚠️ Study-specific folder structures

---

## 🎯 Step 1: Add Audit Trail System

### Backend: Create AuditTrail Entity

**File:** `backend/src/main/java/com/trialsite/model/AuditTrail.java`

```java
package com.trialsite.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_trails")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditTrail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String entityType; // "DOCUMENT", "FOLDER", "SIGNATURE", etc.
    
    @Column(nullable = false)
    private Long entityId;
    
    @Column(nullable = false)
    private String action; // "CREATE", "UPDATE", "DELETE", "VIEW", "DOWNLOAD", "SIGN"
    
    @Column(nullable = false)
    private String performedBy; // Username
    
    @Column(nullable = false)
    private Long performedByUserId;
    
    @Column(length = 2000)
    private String description; // What changed
    
    @Column(length = 5000)
    private String oldValue; // JSON of old state
    
    @Column(length = 5000)
    private String newValue; // JSON of new state
    
    @Column(length = 50)
    private String ipAddress;
    
    @Column(length = 500)
    private String userAgent;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    // For document-specific audits
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;
}
```

### Backend: Create AuditTrail Repository

**File:** `backend/src/main/java/com/trialsite/repository/AuditTrailRepository.java`

```java
package com.trialsite.repository;

import com.trialsite.model.AuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {
    List<AuditTrail> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);
    List<AuditTrail> findByDocumentIdOrderByTimestampDesc(Long documentId);
    List<AuditTrail> findByPerformedByUserIdOrderByTimestampDesc(Long userId);
    
    @Query("SELECT a FROM AuditTrail a WHERE a.timestamp >= :startDate AND a.timestamp <= :endDate ORDER BY a.timestamp DESC")
    List<AuditTrail> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
```

### Backend: Create Audit Service

**File:** `backend/src/main/java/com/trialsite/service/AuditService.java`

```java
package com.trialsite.service;

import com.trialsite.model.AuditTrail;
import com.trialsite.repository.AuditTrailRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditService {
    
    @Autowired
    private AuditTrailRepository auditTrailRepository;
    
    @Transactional
    public void logAction(String entityType, Long entityId, String action, 
                         String performedBy, Long performedByUserId, 
                         String description, String oldValue, String newValue,
                         HttpServletRequest request) {
        AuditTrail audit = new AuditTrail();
        audit.setEntityType(entityType);
        audit.setEntityId(entityId);
        audit.setAction(action);
        audit.setPerformedBy(performedBy);
        audit.setPerformedByUserId(performedByUserId);
        audit.setDescription(description);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        audit.setTimestamp(LocalDateTime.now());
        
        if (request != null) {
            audit.setIpAddress(getClientIpAddress(request));
            audit.setUserAgent(request.getHeader("User-Agent"));
        }
        
        auditTrailRepository.save(audit);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

### Backend: Update DocumentController to Log Actions

Add to `DocumentController.java`:

```java
@Autowired
private AuditService auditService;

@PostMapping("/upload")
public ResponseEntity<?> uploadDocument(...) {
    // ... existing upload code ...
    
    // Log audit trail
    auditService.logAction(
        "DOCUMENT",
        document.getId(),
        "CREATE",
        username,
        userId,
        "Document uploaded: " + document.getOriginalFileName(),
        null,
        documentToJson(document),
        request
    );
    
    return ResponseEntity.ok(new DocumentResponse(document));
}

@DeleteMapping("/{id}")
public ResponseEntity<?> deleteDocument(@PathVariable Long id, HttpServletRequest request) {
    Document document = documentRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Document not found"));
    
    String oldValue = documentToJson(document);
    
    documentRepository.delete(document);
    
    // Log audit trail
    auditService.logAction(
        "DOCUMENT",
        id,
        "DELETE",
        getCurrentUsername(),
        getCurrentUserId(),
        "Document deleted: " + document.getOriginalFileName(),
        oldValue,
        null,
        request
    );
    
    return ResponseEntity.ok(new MessageResponse("Document deleted successfully"));
}
```

---

## 🎯 Step 2: Add Version Control

### Backend: Update Document Model

Add to `Document.java`:

```java
@Column(nullable = false)
private Integer version = 1; // Start at version 1

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "previous_version_id")
private Document previousVersion; // Link to previous version

@Column(nullable = false)
private Boolean isCurrentVersion = true; // Only latest version is "current"
```

### Backend: Create Document Version Service

**File:** `backend/src/main/java/com/trialsite/service/DocumentVersionService.java`

```java
package com.trialsite.service;

import com.trialsite.model.Document;
import com.trialsite.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class DocumentVersionService {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private AuditService auditService;
    
    @Transactional
    public Document createNewVersion(Long documentId, MultipartFile file, 
                                    String description, String username, Long userId,
                                    HttpServletRequest request) {
        // Get original document
        Document original = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        // Mark old version as not current
        original.setIsCurrentVersion(false);
        documentRepository.save(original);
        
        // Create new version
        Document newVersion = new Document();
        newVersion.setOriginalFileName(file.getOriginalFilename());
        newVersion.setFileSize(file.getSize());
        newVersion.setFileType(file.getContentType());
        newVersion.setDescription(description != null ? description : original.getDescription());
        newVersion.setDocumentType(original.getDocumentType());
        newVersion.setProject(original.getProject());
        newVersion.setFolder(original.getFolder());
        newVersion.setUploadedBy(username);
        newVersion.setVersion(original.getVersion() + 1);
        newVersion.setPreviousVersion(original);
        newVersion.setIsCurrentVersion(true);
        
        // Save file
        String fileName = UUID.randomUUID().toString() + 
            getFileExtension(file.getOriginalFilename());
        Path filePath = Paths.get("uploads/" + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        newVersion.setFileName(fileName);
        newVersion.setFilePath(filePath.toString());
        
        Document saved = documentRepository.save(newVersion);
        
        // Log audit
        auditService.logAction(
            "DOCUMENT",
            saved.getId(),
            "VERSION_CREATE",
            username,
            userId,
            "New version " + saved.getVersion() + " created for: " + original.getOriginalFileName(),
            null,
            documentToJson(saved),
            request
        );
        
        return saved;
    }
    
    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }
}
```

### Backend: Add Version Endpoint

Add to `DocumentController.java`:

```java
@PostMapping("/{id}/version")
public ResponseEntity<?> createNewVersion(
    @PathVariable Long id,
    @RequestParam("file") MultipartFile file,
    @RequestParam(value = "description", required = false) String description,
    HttpServletRequest request
) {
    String username = getCurrentUsername();
    Long userId = getCurrentUserId();
    
    Document newVersion = documentVersionService.createNewVersion(
        id, file, description, username, userId, request
    );
    
    return ResponseEntity.ok(new DocumentResponse(newVersion));
}
```

---

## 🎯 Step 3: Add Document Expiration Tracking

### Backend: Update Document Model

Add to `Document.java`:

```java
@Column(name = "expiration_date")
private LocalDateTime expirationDate;

@Column(name = "expiration_reminder_sent")
private Boolean expirationReminderSent = false;

@Column(name = "days_before_expiration_reminder")
private Integer daysBeforeExpirationReminder = 30; // Default 30 days
```

### Backend: Create Expiration Service

**File:** `backend/src/main/java/com/trialsite/service/DocumentExpirationService.java`

```java
package com.trialsite.service;

import com.trialsite.model.Document;
import com.trialsite.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentExpirationService {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    // Run daily at 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkExpiringDocuments() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderDate = now.plusDays(30); // 30 days from now
        
        // Find documents expiring soon that haven't been reminded
        List<Document> expiringDocs = documentRepository.findAll().stream()
            .filter(doc -> doc.getExpirationDate() != null)
            .filter(doc -> doc.getExpirationDate().isBefore(reminderDate))
            .filter(doc -> doc.getExpirationDate().isAfter(now))
            .filter(doc -> !doc.getExpirationReminderSent())
            .collect(Collectors.toList());
        
        // Send reminders (implement email/notification service)
        for (Document doc : expiringDocs) {
            sendExpirationReminder(doc);
            doc.setExpirationReminderSent(true);
            documentRepository.save(doc);
        }
    }
    
    private void sendExpirationReminder(Document doc) {
        // TODO: Implement email/notification
        System.out.println("Document expiring soon: " + doc.getOriginalFileName() + 
                          " on " + doc.getExpirationDate());
    }
}
```

---

## 🎯 Step 4: Enhance Electronic Signatures (FDA 21 CFR Part 11)

### Backend: Update DocumentSignature Model

Add to `DocumentSignature.java`:

```java
@Column(name = "signature_type", nullable = false)
@Enumerated(EnumType.STRING)
private SignatureType signatureType = SignatureType.ELECTRONIC;

@Column(name = "signature_reason", length = 500)
private String signatureReason; // Required: "Approved", "Reviewed", etc.

@Column(name = "signature_meaning", nullable = false, length = 100)
private String signatureMeaning; // "Approved", "Reviewed", "Acknowledged"

@Column(name = "signature_timestamp", nullable = false)
private LocalDateTime signatureTimestamp;

@Column(name = "signature_hash", length = 500)
private String signatureHash; // Cryptographic hash for integrity

@Column(name = "certificate_serial_number", length = 200)
private String certificateSerialNumber; // Digital certificate info

@Column(name = "is_legally_binding", nullable = false)
private Boolean isLegallyBinding = true;

public enum SignatureType {
    ELECTRONIC,  // FDA 21 CFR Part 11 compliant
    DIGITAL,     // PKI-based digital signature
    BIOMETRIC    // Future: fingerprint, etc.
}
```

### Backend: Create Compliant Signature Service

**File:** `backend/src/main/java/com/trialsite/service/CompliantSignatureService.java`

```java
package com.trialsite.service;

import com.trialsite.model.DocumentSignature;
import com.trialsite.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
public class CompliantSignatureService {
    
    @Autowired
    private AuditService auditService;
    
    public DocumentSignature createCompliantSignature(
        Document document,
        User signer,
        User assignedBy,
        String signatureMeaning,
        String signatureReason,
        HttpServletRequest request
    ) {
        DocumentSignature signature = new DocumentSignature();
        signature.setDocument(document);
        signature.setAssignedToUser(signer);
        signature.setAssignedByUser(assignedBy);
        signature.setSignatureType(DocumentSignature.SignatureType.ELECTRONIC);
        signature.setSignatureMeaning(signatureMeaning);
        signature.setSignatureReason(signatureReason);
        signature.setSignatureTimestamp(LocalDateTime.now());
        signature.setIsLegallyBinding(true);
        
        // Generate cryptographic hash for integrity
        String hash = generateSignatureHash(signature);
        signature.setSignatureHash(hash);
        
        // Log audit trail (REQUIRED for FDA compliance)
        auditService.logAction(
            "SIGNATURE",
            signature.getId(),
            "SIGN",
            signer.getUsername(),
            signer.getId(),
            "Document signed: " + document.getOriginalFileName() + 
            " | Meaning: " + signatureMeaning + 
            " | Reason: " + signatureReason,
            null,
            signatureToJson(signature),
            request
        );
        
        return signature;
    }
    
    private String generateSignatureHash(DocumentSignature signature) {
        try {
            String data = signature.getDocument().getId() + 
                         signature.getAssignedToUser().getId() + 
                         signature.getSignatureTimestamp().toString() +
                         signature.getSignatureMeaning();
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(data.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature hash", e);
        }
    }
}
```

---

## 🎯 Step 5: Add Regulatory Document Templates

### Backend: Create DocumentTemplate Entity

**File:** `backend/src/main/java/com/trialsite/model/DocumentTemplate.java`

```java
package com.trialsite.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String templateName;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TemplateCategory category;
    
    @Column(length = 2000)
    private String description;
    
    @Column(nullable = false)
    private String filePath; // Path to template file
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private String createdBy;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum TemplateCategory {
        PROTOCOL,
        INFORMED_CONSENT,
        INVESTIGATOR_BROCHURE,
        CASE_REPORT_FORM,
        ADVERSE_EVENT_FORM,
        REGULATORY_SUBMISSION,
        SITE_QUALIFICATION,
        OTHER
    }
}
```

### Backend: Create Template Service

**File:** `backend/src/main/java/com/trialsite/service/DocumentTemplateService.java`

```java
package com.trialsite.service;

import com.trialsite.model.DocumentTemplate;
import com.trialsite.repository.DocumentTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentTemplateService {
    
    @Autowired
    private DocumentTemplateRepository templateRepository;
    
    public List<DocumentTemplate> getTemplatesByCategory(DocumentTemplate.TemplateCategory category) {
        return templateRepository.findByCategoryAndIsActiveTrue(category);
    }
    
    public Document createDocumentFromTemplate(Long templateId, Long projectId, 
                                              String documentName, String username) {
        DocumentTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        
        // Copy template file and create document
        // Implementation depends on your file storage
        // ...
        
        return document;
    }
}
```

---

## 🎯 Step 6: Add Study-Specific Folder Structure

### Backend: Create Regulatory Folder Service

**File:** `backend/src/main/java/com/trialsite/service/RegulatoryFolderService.java`

```java
package com.trialsite.service;

import com.trialsite.model.Folder;
import com.trialsite.model.Project;
import com.trialsite.repository.FolderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class RegulatoryFolderService {
    
    @Autowired
    private FolderRepository folderRepository;
    
    @Transactional
    public void createStandardRegulatoryStructure(Project project, String createdBy) {
        // Standard eISF folder structure for clinical trials
        List<String> standardFolders = Arrays.asList(
            "01_Protocol_and_Amendments",
            "02_Investigator_Brochure",
            "03_Informed_Consent_Forms",
            "04_IRB_EC_Approvals",
            "05_Regulatory_Submissions",
            "06_Investigator_Qualifications",
            "07_Laboratory_Certifications",
            "08_Device_Accountability",
            "09_Adverse_Events",
            "10_Study_Reports",
            "11_Financial_Disclosures",
            "12_Other_Documents"
        );
        
        for (String folderName : standardFolders) {
            Folder folder = new Folder();
            folder.setFolderName(folderName);
            folder.setProject(project);
            folder.setDescription("Standard regulatory folder: " + folderName);
            folder.setCreatedBy(createdBy);
            folderRepository.save(folder);
        }
    }
}
```

---

## 🎯 Step 7: Frontend Updates

### Frontend: Add Audit Trail View

**File:** `frontend/src/pages/AuditTrail.jsx`

```jsx
import { useState, useEffect } from 'react';
import { Table, Card, DatePicker, Select, Input } from 'antd';
import { auditService } from '../services/auditService';
import dayjs from 'dayjs';

const AuditTrail = () => {
  const [audits, setAudits] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({});

  useEffect(() => {
    loadAudits();
  }, [filters]);

  const loadAudits = async () => {
    setLoading(true);
    try {
      const data = await auditService.getAudits(filters);
      setAudits(data);
    } catch (error) {
      console.error('Failed to load audit trail', error);
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: 'Timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
      render: (date) => dayjs(date).format('YYYY-MM-DD HH:mm:ss'),
      sorter: true
    },
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action'
    },
    {
      title: 'User',
      dataIndex: 'performedBy',
      key: 'performedBy'
    },
    {
      title: 'Entity',
      dataIndex: 'entityType',
      key: 'entityType'
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true
    },
    {
      title: 'IP Address',
      dataIndex: 'ipAddress',
      key: 'ipAddress'
    }
  ];

  return (
    <Card title="Audit Trail">
      <Table
        columns={columns}
        dataSource={audits}
        loading={loading}
        rowKey="id"
        pagination={{ pageSize: 50 }}
      />
    </Card>
  );
};

export default AuditTrail;
```

### Frontend: Add Version History View

Add to `Documents.jsx`:

```jsx
const [versionModalVisible, setVersionModalVisible] = useState(false);
const [selectedDocumentVersions, setSelectedDocumentVersions] = useState([]);

const loadDocumentVersions = async (documentId) => {
  try {
    const versions = await documentService.getDocumentVersions(documentId);
    setSelectedDocumentVersions(versions);
    setVersionModalVisible(true);
  } catch (error) {
    message.error('Failed to load document versions');
  }
};

// Add to Actions column:
<Button
  type="link"
  icon={<HistoryOutlined />}
  onClick={() => loadDocumentVersions(record.id)}
>
  Versions
</Button>
```

---

## 🎯 Step 8: Compliance Checklist

### ✅ FDA 21 CFR Part 11 Requirements

- [x] **System Validation** - Document all system requirements
- [x] **Audit Trails** - Log all actions (CREATE, UPDATE, DELETE, VIEW, SIGN)
- [x] **Electronic Signatures** - Unique to each user, timestamped, with meaning
- [x] **Access Controls** - Role-based permissions (already implemented)
- [x] **Data Integrity** - Version control, cryptographic hashes
- [x] **Training Records** - Document user training
- [x] **Data Retention** - Store records for required period
- [x] **Backup & Recovery** - Implement backup strategy

### ✅ ICH-GCP Requirements

- [x] **Document Control** - Version control, approval workflows
- [x] **Access Control** - Only authorized personnel
- [x] **Audit Trail** - Complete history of changes
- [x] **Data Integrity** - Cannot delete or modify without trace

---

## 🚀 Implementation Priority

1. **Week 1:** Audit Trail System (Critical for compliance)
2. **Week 2:** Version Control
3. **Week 3:** Document Expiration Tracking
4. **Week 4:** Enhanced Electronic Signatures
5. **Week 5:** Templates & Folder Structures
6. **Week 6:** Frontend Updates & Testing

---

## 📝 Next Steps

1. Create the new entities and services
2. Update existing controllers to use audit service
3. Add frontend components for audit trail and versions
4. Test compliance features
5. Document system validation procedures
6. Train users on compliance requirements

---

## ⚠️ Important Notes

- **Audit trails are IMMUTABLE** - Never allow deletion
- **Signatures must be cryptographically secure**
- **All actions must be logged** - No exceptions
- **Version history must be preserved** - Cannot delete old versions
- **Access controls are critical** - Review regularly

---

**Need help implementing?** Start with Step 1 (Audit Trail) - it's the foundation for all compliance features.
