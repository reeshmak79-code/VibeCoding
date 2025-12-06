package com.trialsite.repository;

import com.trialsite.model.DocumentPermission;
import com.trialsite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {
    List<DocumentPermission> findByDocumentId(Long documentId);
    List<DocumentPermission> findByFolderId(Long folderId);
    List<DocumentPermission> findByUserId(Long userId);
    List<DocumentPermission> findByRole(User.Role role);
    Optional<DocumentPermission> findByDocumentIdAndUserId(Long documentId, Long userId);
    Optional<DocumentPermission> findByDocumentIdAndRole(Long documentId, User.Role role);
    Optional<DocumentPermission> findByFolderIdAndUserId(Long folderId, Long userId);
    Optional<DocumentPermission> findByFolderIdAndRole(Long folderId, User.Role role);
    void deleteByDocumentId(Long documentId);
    void deleteByFolderId(Long folderId);
}
