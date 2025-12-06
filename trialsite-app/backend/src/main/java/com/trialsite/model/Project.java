package com.trialsite.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Project name is required")
    @Column(nullable = false)
    private String projectName;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    @NotNull(message = "Client is required")
    private Client client;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;
    
    @Column(nullable = false)
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal budget;
    
    @Column(length = 2000)
    private String description;
    
    @Column(length = 2000)
    private String deliverables;
    
    @Column(length = 1000)
    private String notes;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum ServiceType {
        DOCUMENT_PREPARATION("Essential Document Preparation"),
        REGULATORY_COMPLIANCE("Regulatory/Compliance"),
        EDC_ECRF_SERVICES("EDC/eCRF Services"),
        INVESTIGATOR_RECRUITMENT("Investigator Recruitment"),
        PERSONNEL_TRAINING("Personnel Training"),
        CONTRACT_BUDGET_NEGOTIATION("Contract/Budget Negotiation");
        
        private final String displayName;
        
        ServiceType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum ProjectStatus {
        LEAD("Lead"),
        PROPOSAL("Proposal Sent"),
        ACTIVE("Active"),
        ON_HOLD("On Hold"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");
        
        private final String displayName;
        
        ProjectStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
