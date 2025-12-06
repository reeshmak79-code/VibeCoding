package com.trialsite.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_signatures")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSignature {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id", nullable = false)
    private User assignedToUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id", nullable = false)
    private User assignedByUser;
    
    @Column(name = "pandadoc_document_id")
    private String pandadocDocumentId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignatureStatus status = SignatureStatus.PENDING;
    
    @Column(name = "signing_url", length = 2000)
    private String signingUrl;
    
    @Column(name = "message", length = 1000)
    private String message;
    
    @Column(name = "signed_at")
    private LocalDateTime signedAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum SignatureStatus {
        PENDING,    // Created but not sent to PandaDoc yet
        SENT,       // Sent to PandaDoc, waiting for recipient
        VIEWED,     // Recipient has viewed the document
        SIGNED,     // Document has been signed
        DECLINED,   // Recipient declined to sign
        EXPIRED,    // Document signing expired
        CANCELLED   // Admin cancelled the signing request
    }
}
