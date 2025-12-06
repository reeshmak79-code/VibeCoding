package com.trialsite.repository;

import com.trialsite.model.DocumentSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentSignatureRepository extends JpaRepository<DocumentSignature, Long> {
    List<DocumentSignature> findByAssignedToUserId(Long userId);
    List<DocumentSignature> findByAssignedToUserIdAndStatus(Long userId, DocumentSignature.SignatureStatus status);
    List<DocumentSignature> findByDocumentId(Long documentId);
    List<DocumentSignature> findByDocumentIdAndAssignedToUserId(Long documentId, Long userId);
    Optional<DocumentSignature> findByPandadocDocumentId(String pandadocDocumentId);
    List<DocumentSignature> findByAssignedToUserIdAndStatusIn(Long userId, List<DocumentSignature.SignatureStatus> statuses);
}
