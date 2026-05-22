package com.benbanking.api.controllers;

import com.benbanking.api.auth.RequestAuth;
import com.benbanking.api.auth.SessionAuthFilter;
import com.benbanking.api.auth.SessionService;
import com.benbanking.api.dto.LoginRequest;
import com.benbanking.api.dto.RegisterRequest;
import com.benbanking.api.models.User;
import com.benbanking.api.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;

    public AuthController(AuthService authService, SessionService sessionService) {
        this.authService = authService;
        this.sessionService = sessionService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // AuthService throws IllegalArgumentException on validation failures;
        // GlobalExceptionHandler converts those into 400 responses automatically.
        User user = authService.registerUser(
                request.getUsername(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber(),
                request.getEmail(),
                request.getAddress()
        );
        String token = sessionService.createSession(user);
        return ResponseEntity.ok(authResponse(user, token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.loginUser(request.getUsername(), request.getPassword());

        if (user == null) {
            return ResponseEntity.status(401).body(
                    Map.of("message", "Invalid username or password.")
            );
        }

        String token = sessionService.createSession(user);
        return ResponseEntity.ok(authResponse(user, token));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        SessionAuthFilter.extractBearerToken(request).ifPresent(sessionService::invalidate);
        return ResponseEntity.ok(Map.of("message", "Signed out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        var session = RequestAuth.requireSession(request);
        return ResponseEntity.ok(Map.of(
                "userId", session.getUserId(),
                "username", session.getUsername(),
                "role", session.getRole()
        ));
    }

    private Map<String, Object> authResponse(User user, String token) {
        return Map.of(
                "message", "Authentication successful.",
                "token", token,
                "userId", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole()
        );
    }
}
