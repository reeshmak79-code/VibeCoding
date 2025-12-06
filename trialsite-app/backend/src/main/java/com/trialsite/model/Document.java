package com.trialsite.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String originalFileName;
    
    @Column(nullable = false)
    private String filePath;
    
    @Column(nullable = false)
    private Long fileSize;
    
    @Column(nullable = false)
    private String fileType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;
    
    @Column(length = 1000)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;
    
    @Column(nullable = false)
    private String uploadedBy;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
    
    public enum DocumentType {
        CONTRACT,
        PROPOSAL,
        DELIVERABLE,
        REPORT,
        TRAINING_MATERIAL,
        OTHER
    }
}
