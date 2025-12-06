package com.trialsite.controller;

import com.trialsite.model.Project;
import com.trialsite.repository.ClientRepository;
import com.trialsite.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {
    
    @Autowired
    private ClientRepository clientRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @GetMapping("/overview")
    public ResponseEntity<?> getDashboardOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        // Client stats
        long totalClients = clientRepository.count();
        long activeClients = clientRepository.countByStatus(com.trialsite.model.Client.ClientStatus.ACTIVE);
        long potentialClients = clientRepository.countByStatus(com.trialsite.model.Client.ClientStatus.POTENTIAL);
        
        Map<String, Long> clientStats = new HashMap<>();
        clientStats.put("total", totalClients);
        clientStats.put("active", activeClients);
        clientStats.put("potential", potentialClients);
        overview.put("clients", clientStats);
        
        // Project stats
        long totalProjects = projectRepository.count();
        long activeProjects = projectRepository.countByStatus(Project.ProjectStatus.ACTIVE);
        long completedProjects = projectRepository.countByStatus(Project.ProjectStatus.COMPLETED);
        long proposalProjects = projectRepository.countByStatus(Project.ProjectStatus.PROPOSAL);
        long leadProjects = projectRepository.countByStatus(Project.ProjectStatus.LEAD);
        
        Map<String, Long> projectStats = new HashMap<>();
        projectStats.put("total", totalProjects);
        projectStats.put("active", activeProjects);
        projectStats.put("completed", completedProjects);
        projectStats.put("proposal", proposalProjects);
        projectStats.put("lead", leadProjects);
        overview.put("projects", projectStats);
        
        return ResponseEntity.ok(overview);
    }
    
    @GetMapping("/projects-by-service")
    public ResponseEntity<?> getProjectsByService() {
        List<Project> allProjects = projectRepository.findAll();
        
        Map<String, Long> serviceCount = new HashMap<>();
        for (Project.ServiceType type : Project.ServiceType.values()) {
            long count = allProjects.stream()
                    .filter(p -> p.getServiceType() == type)
                    .count();
            serviceCount.put(type.name(), count);
        }
        
        return ResponseEntity.ok(serviceCount);
    }
    
    @GetMapping("/projects-by-status")
    public ResponseEntity<?> getProjectsByStatus() {
        Map<String, Long> statusCount = new HashMap<>();
        
        for (Project.ProjectStatus status : Project.ProjectStatus.values()) {
            long count = projectRepository.countByStatus(status);
            statusCount.put(status.name(), count);
        }
        
        return ResponseEntity.ok(statusCount);
    }
    
    @GetMapping("/revenue-stats")
    public ResponseEntity<?> getRevenueStats() {
        List<Project> allProjects = projectRepository.findAll();
        
        BigDecimal totalBudget = allProjects.stream()
                .filter(p -> p.getBudget() != null)
                .map(Project::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal activeBudget = allProjects.stream()
                .filter(p -> p.getStatus() == Project.ProjectStatus.ACTIVE && p.getBudget() != null)
                .map(Project::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal completedBudget = allProjects.stream()
                .filter(p -> p.getStatus() == Project.ProjectStatus.COMPLETED && p.getBudget() != null)
                .map(Project::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, BigDecimal> revenueStats = new HashMap<>();
        revenueStats.put("total", totalBudget);
        revenueStats.put("active", activeBudget);
        revenueStats.put("completed", completedBudget);
        
        return ResponseEntity.ok(revenueStats);
    }
    
    @GetMapping("/recent-projects")
    public ResponseEntity<?> getRecentProjects() {
        List<Project> allProjects = projectRepository.findAll();
        
        List<Project> recentProjects = allProjects.stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .limit(5)
                .toList();
        
        return ResponseEntity.ok(recentProjects);
    }
}
