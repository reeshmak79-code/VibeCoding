package com.trialsite.repository;

import com.trialsite.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    
    List<Client> findByStatus(Client.ClientStatus status);
    
    List<Client> findByType(Client.ClientType type);
    
    @Query("SELECT c FROM Client c WHERE " +
           "LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.contactPerson) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Client> searchClients(@Param("search") String search);
    
    long countByStatus(Client.ClientStatus status);
}
