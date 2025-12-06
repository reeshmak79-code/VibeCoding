package com.trialsite.dto;

import com.trialsite.model.Project;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String projectName;
    private Long clientId;
    private String clientName;
    private Project.ServiceType serviceType;
    private Project.ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private String description;
    private String deliverables;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static ProjectResponse fromProject(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getProjectName(),
            project.getClient().getId(),
            project.getClient().getCompanyName(),
            project.getServiceType(),
            project.getStatus(),
            project.getStartDate(),
            project.getEndDate(),
            project.getBudget(),
            project.getDescription(),
            project.getDeliverables(),
            project.getNotes(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}
