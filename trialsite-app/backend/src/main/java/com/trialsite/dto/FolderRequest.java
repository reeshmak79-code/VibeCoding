package com.trialsite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FolderRequest {
    
    @NotBlank(message = "Folder name is required")
    @Size(min = 1, max = 255, message = "Folder name must be between 1 and 255 characters")
    private String folderName;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @NotNull(message = "Project ID is required")
    private Long projectId;
    
    private Long parentFolderId; // null for root folders
}
