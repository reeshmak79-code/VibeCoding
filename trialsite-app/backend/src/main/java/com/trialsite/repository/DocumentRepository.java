package com.trialsite.repository;

import com.trialsite.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByProjectId(Long projectId);
    List<Document> findByFolderId(Long folderId);
    List<Document> findByProjectIdAndFolderIsNull(Long projectId);
    List<Document> findByDocumentType(Document.DocumentType documentType);
    List<Document> findByProjectIdAndDocumentType(Long projectId, Document.DocumentType documentType);
    long countByProjectId(Long projectId);
    long countByFolderId(Long folderId);
}
