package com.skrepta.skreptajava.googleauth.controller;

import com.skrepta.skreptajava.auth.dto.AuthResponse;
import com.skrepta.skreptajava.googleauth.dto.GoogleAuthRequest;
import com.skrepta.skreptajava.googleauth.service.GoogleAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    @PostMapping
    public ResponseEntity<AuthResponse> googleAuth(@RequestBody GoogleAuthRequest request) {
        AuthResponse response = googleAuthService.authenticateWithGoogle(request.getToken());
        return ResponseEntity.ok(response);
    }
}