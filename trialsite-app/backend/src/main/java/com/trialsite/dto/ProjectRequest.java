package com.trialsite.dto;

import com.trialsite.model.Project;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProjectRequest {
    
    @NotBlank(message = "Project name is required")
    @Size(min = 2, max = 200, message = "Project name must be between 2 and 200 characters")
    private String projectName;
    
    @NotNull(message = "Client is required")
    private Long clientId;
    
    @NotNull(message = "Service type is required")
    private Project.ServiceType serviceType;
    
    @NotNull(message = "Status is required")
    private Project.ProjectStatus status;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private BigDecimal budget;
    
    private String description;
    
    private String deliverables;
    
    private String notes;
}
