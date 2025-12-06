package com.trialsite.service;

import com.trialsite.model.User;
import com.trialsite.repository.DocumentPermissionRepository;
import com.trialsite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DocumentPermissionRepository permissionRepository;
    
    @Transactional
    public void deleteUser(Long userId) {
        // Delete all document permissions associated with this user first
        // This must be done before deleting the user to avoid foreign key constraint violations
        permissionRepository.deleteByUserId(userId);
        
        // Now delete the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }
}
