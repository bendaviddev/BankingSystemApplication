package com.benbanking.api.controllers;

import com.benbanking.api.auth.AuthSession;
import com.benbanking.api.auth.RequestAuth;
import com.benbanking.api.auth.SessionAuthFilter;
import com.benbanking.api.auth.SessionService;
import com.benbanking.api.dto.ChangePasswordRequest;
import com.benbanking.api.dto.LoginRequest;
import com.benbanking.api.dto.MeResponse;
import com.benbanking.api.dto.RegisterRequest;
import com.benbanking.api.enums.ActivityType;
import com.benbanking.api.exceptions.BadRequestException;
import com.benbanking.api.exceptions.UnauthorizedException;
import com.benbanking.api.models.Log;
import com.benbanking.api.models.User;
import com.benbanking.api.repositories.LogRepository;
import com.benbanking.api.services.AlertService;
import com.benbanking.api.services.AuthService;
import com.benbanking.api.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final LogRepository logRepository;
    private final AlertService alertService;

    public AuthController(AuthService authService, SessionService sessionService,
            LogRepository logRepository, AlertService alertService) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.logRepository = logRepository;
        this.alertService = alertService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
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
        Optional<String> token = SessionAuthFilter.extractBearerToken(request);
        token.ifPresent(sessionService::invalidate);

        Object attr = request.getAttribute(SessionService.REQUEST_ATTRIBUTE);
        if (attr instanceof AuthSession session) {
            logRepository.save(new Log(0, session.getUserId(), ActivityType.LOGOUT, "User logged out.", LocalDateTime.now()));
        }

        return ResponseEntity.ok(Map.of("message", "Signed out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        AuthSession session = RequestAuth.requireSession(request);
        User user = authService.requireUser(session.getUserId());
        return ResponseEntity.ok(MeResponse.from(user));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(HttpServletRequest request, @Valid @RequestBody ChangePasswordRequest body) {
        AuthSession session = RequestAuth.requireSession(request);
        User user = authService.requireUser(session.getUserId());

        if (!PasswordUtil.checkPassword(body.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect.");
        }
        if (!PasswordUtil.isValidPassword(body.getNewPassword())) {
            throw new BadRequestException(
                    "Password must be at least 8 characters and include an uppercase letter, a number, and a special character.");
        }

        String hashed = PasswordUtil.hashPassword(body.getNewPassword());
        authService.updatePassword(session.getUserId(), hashed);

        SessionAuthFilter.extractBearerToken(request)
                .ifPresent(token -> sessionService.invalidateOtherSessions(session.getUserId(), token));

        logRepository.save(new Log(0, session.getUserId(), ActivityType.CHANGE_PASSWORD, "User changed their password.", LocalDateTime.now()));
        alertService.firePasswordChanged(session.getUserId());

        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

    private Map<String, Object> authResponse(User user, String token) {
        return Map.of(
                "message", "Authentication successful.",
                "token", token,
                "userId", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole(),
                "firstName", user.getFirstName()
        );
    }
}
