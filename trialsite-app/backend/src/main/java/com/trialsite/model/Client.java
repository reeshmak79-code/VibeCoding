package com.trialsite.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Company name is required")
    @Column(nullable = false)
    private String companyName;
    
    @NotBlank(message = "Contact person is required")
    @Column(nullable = false)
    private String contactPerson;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String phone;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientStatus status;
    
    @Column(length = 1000)
    private String specialtyAreas;
    
    @Column(length = 2000)
    private String notes;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum ClientType {
        RESEARCH_SITE("Research Site"),
        SITE_NETWORK("Site Network"),
        AMC("AMC"),
        CRO("CRO"),
        SPONSOR("Sponsor");
        
        private final String displayName;
        
        ClientType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum ClientStatus {
        ACTIVE("Active"),
        POTENTIAL("Potential"),
        COMPLETED("Completed"),
        ON_HOLD("On Hold");
        
        private final String displayName;
        
        ClientStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
