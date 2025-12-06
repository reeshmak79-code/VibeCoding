package com.trialsite.repository;

import com.trialsite.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByProjectId(Long projectId);
    List<Folder> findByProjectIdAndParentFolderIsNull(Long projectId);
    List<Folder> findByParentFolderId(Long parentFolderId);
    long countByProjectId(Long projectId);
}
