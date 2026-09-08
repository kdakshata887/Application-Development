package com.examly.springapp.controller;

import com.examly.springapp.dto.ApiResponse;
import com.examly.springapp.dto.LoginRequest;
import com.examly.springapp.dto.LoginResponse;
import com.examly.springapp.dto.RegisterRequest;
import com.examly.springapp.model.User;
import com.examly.springapp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints.
 *
 * Security model:
 *   - POST /api/auth/login       — public (no authentication required)
 *   - POST /api/auth/register    — requires ADMIN or PRINCIPAL role.
 *
 * SECURITY: No open self-registration. All accounts must be created by
 * an administrator. This prevents privilege escalation via the register endpoint.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user account.
     * Requires ADMIN or PRINCIPAL authentication — prevents any anonymous
     * or low-privilege user from creating accounts with elevated roles.
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.builder()
                        .success(true)
                        .message("User registered successfully with role: " + request.getRole())
                        .data(user)
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
