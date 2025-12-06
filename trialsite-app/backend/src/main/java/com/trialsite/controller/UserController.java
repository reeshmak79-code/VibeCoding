package com.trialsite.controller;

import com.trialsite.dto.MessageResponse;
import com.trialsite.dto.UserRequest;
import com.trialsite.dto.UserResponse;
import com.trialsite.model.User;
import com.trialsite.repository.DocumentPermissionRepository;
import com.trialsite.repository.UserRepository;
import com.trialsite.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private DocumentPermissionRepository permissionRepository;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserResponse> response = users.stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(new UserResponse(user));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(
            @Valid @RequestBody UserRequest request,
            Authentication authentication) {
        try {
            // Check if username already exists
            if (userRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Username already exists"));
            }
            
            // Check if email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Email already exists"));
            }
            
            User user = new User();
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setUsername(request.getUsername());
            user.setRole(request.getRole());
            user.setActive(request.getActive() != null ? request.getActive() : true);
            
            // Generate password if not provided
            String password = request.getPassword();
            if (password == null || password.trim().isEmpty()) {
                // Auto-generate password (12 characters, alphanumeric)
                String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 12; i++) {
                    int index = (int) (Math.random() * chars.length());
                    sb.append(chars.charAt(index));
                }
                password = sb.toString();
            } else {
                // Trim and validate provided password
                password = password.trim();
                if (password.isEmpty() || password.length() < 6) {
                    return ResponseEntity.badRequest()
                            .body(new MessageResponse("Password must be at least 6 characters"));
                }
            }
            
            // Ensure password is valid (at least 6 characters) before encoding
            if (password == null || password.length() < 6) {
                // Fallback: generate password if somehow still invalid
                String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 12; i++) {
                    int index = (int) (Math.random() * chars.length());
                    sb.append(chars.charAt(index));
                }
                password = sb.toString();
            }
            
            // Encode password (BCrypt always produces 60-char hash, so constraint will pass)
            String encodedPassword = passwordEncoder.encode(password);
            user.setPassword(encodedPassword);
            
            User savedUser = userRepository.save(user);
            
            // Return user response with generated password (only for admin to share)
            UserResponse response = new UserResponse(savedUser);
            
            return ResponseEntity.ok(new MessageResponse(
                "User created successfully. Temporary password: " + password
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error creating user: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request,
            Authentication authentication) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if username already exists (for other users)
            if (!user.getUsername().equals(request.getUsername()) && 
                userRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Username already exists"));
            }
            
            // Check if email already exists (for other users)
            if (!user.getEmail().equals(request.getEmail()) && 
                userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Email already exists"));
            }
            
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setUsername(request.getUsername());
            user.setRole(request.getRole());
            if (request.getActive() != null) {
                user.setActive(request.getActive());
            }
            
            // Update password if provided
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }
            
            User updatedUser = userRepository.save(user);
            return ResponseEntity.ok(new UserResponse(updatedUser));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error updating user: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setActive(false);
            userRepository.save(user);
            
            return ResponseEntity.ok(new MessageResponse("User deactivated successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error deactivating user: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setActive(true);
            userRepository.save(user);
            
            return ResponseEntity.ok(new MessageResponse("User activated successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error activating user: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Prevent admin from deleting themselves
            User currentUser = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            
            if (user.getId().equals(currentUser.getId())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("You cannot delete your own account"));
            }
            
            // Use service method which has proper @Transactional annotation
            userService.deleteUser(user.getId());
            
            return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
            
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Handle any remaining foreign key constraint violations
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Cannot delete user: User is still referenced in other records. Please remove all associated data first."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error deleting user: " + e.getMessage()));
        }
    }
}
