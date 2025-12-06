package com.trialsite.controller;

import com.trialsite.dto.MessageResponse;
import com.trialsite.dto.ProjectRequest;
import com.trialsite.dto.ProjectResponse;
import com.trialsite.model.Client;
import com.trialsite.model.Project;
import com.trialsite.repository.ClientRepository;
import com.trialsite.repository.ProjectRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProjectController {
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private ClientRepository clientRepository;
    
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        List<ProjectResponse> responses = projects.stream()
                .map(ProjectResponse::fromProject)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getProjectById(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(project -> ResponseEntity.ok(ProjectResponse.fromProject(project)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<ProjectResponse>> searchProjects(@RequestParam String q) {
        List<Project> projects = projectRepository.searchProjects(q);
        List<ProjectResponse> responses = projects.stream()
                .map(ProjectResponse::fromProject)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ProjectResponse>> getProjectsByClient(@PathVariable Long clientId) {
        List<Project> projects = projectRepository.findByClientId(clientId);
        List<ProjectResponse> responses = projects.stream()
                .map(ProjectResponse::fromProject)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProjectResponse>> getProjectsByStatus(@PathVariable Project.ProjectStatus status) {
        List<Project> projects = projectRepository.findByStatus(status);
        List<ProjectResponse> responses = projects.stream()
                .map(ProjectResponse::fromProject)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getProjectStats() {
        long totalProjects = projectRepository.count();
        long activeProjects = projectRepository.countByStatus(Project.ProjectStatus.ACTIVE);
        long completedProjects = projectRepository.countByStatus(Project.ProjectStatus.COMPLETED);
        long proposalProjects = projectRepository.countByStatus(Project.ProjectStatus.PROPOSAL);
        
        return ResponseEntity.ok(new Object() {
            public final long total = totalProjects;
            public final long active = activeProjects;
            public final long completed = completedProjects;
            public final long proposal = proposalProjects;
        });
    }
    
    @PostMapping
    public ResponseEntity<?> createProject(@Valid @RequestBody ProjectRequest request) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));
        
        Project project = new Project();
        project.setProjectName(request.getProjectName());
        project.setClient(client);
        project.setServiceType(request.getServiceType());
        project.setStatus(request.getStatus());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setBudget(request.getBudget());
        project.setDescription(request.getDescription());
        project.setDeliverables(request.getDeliverables());
        project.setNotes(request.getNotes());
        
        Project savedProject = projectRepository.save(project);
        return ResponseEntity.ok(ProjectResponse.fromProject(savedProject));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projectRepository.findById(id)
                .map(project -> {
                    Client client = clientRepository.findById(request.getClientId())
                            .orElseThrow(() -> new RuntimeException("Client not found"));
                    
                    project.setProjectName(request.getProjectName());
                    project.setClient(client);
                    project.setServiceType(request.getServiceType());
                    project.setStatus(request.getStatus());
                    project.setStartDate(request.getStartDate());
                    project.setEndDate(request.getEndDate());
                    project.setBudget(request.getBudget());
                    project.setDescription(request.getDescription());
                    project.setDeliverables(request.getDeliverables());
                    project.setNotes(request.getNotes());
                    
                    Project updatedProject = projectRepository.save(project);
                    return ResponseEntity.ok(ProjectResponse.fromProject(updatedProject));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(project -> {
                    projectRepository.delete(project);
                    return ResponseEntity.ok(new MessageResponse("Project deleted successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
