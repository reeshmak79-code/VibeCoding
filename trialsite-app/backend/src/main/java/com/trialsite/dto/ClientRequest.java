package com.trialsite.dto;

import com.trialsite.model.Client;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ClientRequest {
    
    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String companyName;
    
    @NotBlank(message = "Contact person is required")
    @Size(min = 2, max = 100, message = "Contact person name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\.\\-']+$", message = "Contact person name should only contain letters")
    private String contactPerson;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Phone is required")
    @Size(min = 10, max = 10, message = "Phone number must be exactly 10 digits")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phone;
    
    @NotNull(message = "Client type is required")
    private Client.ClientType type;
    
    @NotNull(message = "Client status is required")
    private Client.ClientStatus status;
    
    private String specialtyAreas;
    
    private String notes;
}
