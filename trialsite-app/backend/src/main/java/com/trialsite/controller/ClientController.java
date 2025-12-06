package com.trialsite.controller;

import com.trialsite.dto.ClientRequest;
import com.trialsite.dto.MessageResponse;
import com.trialsite.model.Client;
import com.trialsite.repository.ClientRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ClientController {
    
    @Autowired
    private ClientRepository clientRepository;
    
    @GetMapping
    public ResponseEntity<List<Client>> getAllClients() {
        List<Client> clients = clientRepository.findAll();
        return ResponseEntity.ok(clients);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getClientById(@PathVariable Long id) {
        return clientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Client>> searchClients(@RequestParam String q) {
        List<Client> clients = clientRepository.searchClients(q);
        return ResponseEntity.ok(clients);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Client>> getClientsByStatus(@PathVariable Client.ClientStatus status) {
        List<Client> clients = clientRepository.findByStatus(status);
        return ResponseEntity.ok(clients);
    }
    
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Client>> getClientsByType(@PathVariable Client.ClientType type) {
        List<Client> clients = clientRepository.findByType(type);
        return ResponseEntity.ok(clients);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getClientStats() {
        long totalClients = clientRepository.count();
        long activeClients = clientRepository.countByStatus(Client.ClientStatus.ACTIVE);
        long potentialClients = clientRepository.countByStatus(Client.ClientStatus.POTENTIAL);
        
        return ResponseEntity.ok(new Object() {
            public final long total = totalClients;
            public final long active = activeClients;
            public final long potential = potentialClients;
        });
    }
    
    @PostMapping
    public ResponseEntity<?> createClient(@Valid @RequestBody ClientRequest request) {
        Client client = new Client();
        client.setCompanyName(request.getCompanyName());
        client.setContactPerson(request.getContactPerson());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setType(request.getType());
        client.setStatus(request.getStatus());
        client.setSpecialtyAreas(request.getSpecialtyAreas());
        client.setNotes(request.getNotes());
        
        Client savedClient = clientRepository.save(client);
        return ResponseEntity.ok(savedClient);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClient(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        return clientRepository.findById(id)
                .map(client -> {
                    client.setCompanyName(request.getCompanyName());
                    client.setContactPerson(request.getContactPerson());
                    client.setEmail(request.getEmail());
                    client.setPhone(request.getPhone());
                    client.setType(request.getType());
                    client.setStatus(request.getStatus());
                    client.setSpecialtyAreas(request.getSpecialtyAreas());
                    client.setNotes(request.getNotes());
                    
                    Client updatedClient = clientRepository.save(client);
                    return ResponseEntity.ok(updatedClient);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClient(@PathVariable Long id) {
        return clientRepository.findById(id)
                .map(client -> {
                    clientRepository.delete(client);
                    return ResponseEntity.ok(new MessageResponse("Client deleted successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
