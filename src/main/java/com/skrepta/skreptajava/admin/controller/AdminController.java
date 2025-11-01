package com.skrepta.skreptajava.admin.controller;

import com.skrepta.skreptajava.admin.dto.UserRoleUpdateRequest;
import com.skrepta.skreptajava.admin.service.AdminService;
import com.skrepta.skreptajava.auth.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Все методы в этом контроллере доступны только ADMIN
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequest request
    ) {
        return ResponseEntity.ok(adminService.updateUserRole(userId, request.getNewRole()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
