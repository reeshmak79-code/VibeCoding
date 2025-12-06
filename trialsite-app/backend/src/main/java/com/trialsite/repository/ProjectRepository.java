package com.trialsite.repository;

import com.trialsite.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    List<Project> findByStatus(Project.ProjectStatus status);
    
    List<Project> findByServiceType(Project.ServiceType serviceType);
    
    List<Project> findByClientId(Long clientId);
    
    @Query("SELECT p FROM Project p WHERE " +
           "LOWER(p.projectName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.client.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Project> searchProjects(@Param("search") String search);
    
    long countByStatus(Project.ProjectStatus status);
    
    @Query("SELECT COUNT(p) FROM Project p WHERE p.status = :status AND p.client.id = :clientId")
    long countByStatusAndClientId(@Param("status") Project.ProjectStatus status, @Param("clientId") Long clientId);
}
