package com.akosgyongyosi.cashflow.controller.admin;

import com.akosgyongyosi.cashflow.entity.Role;
import com.akosgyongyosi.cashflow.entity.User;
import com.akosgyongyosi.cashflow.repository.UserRepository;
import com.akosgyongyosi.cashflow.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public AdminController(UserRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId, Principal principal) {
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        User targetUser = userRepository.findById(userId).orElse(null);
        auditLogService.logAction(principal.getName(), "DELETE_USER", 
            Map.of("userId", userId, "targetEmail", targetUser != null ? targetUser.getEmail() : "unknown"));
        userRepository.deleteById(userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/{userId}/promote")
    public ResponseEntity<?> promoteToAdmin(@PathVariable Long userId, Principal principal) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        Role previousRole = user.getRole();
        user.setRole(Role.ADMIN);
        userRepository.save(user);
        auditLogService.logAction(principal.getName(), "PROMOTE_USER", 
            Map.of("userId", userId, "targetEmail", user.getEmail(), 
                   "fromRole", previousRole.name(), "toRole", "ADMIN"));
        return ResponseEntity.ok(user);
    }
}
