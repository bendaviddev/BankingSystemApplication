package com.benbanking.api.controllers;

import com.benbanking.api.auth.AuthSession;
import com.benbanking.api.auth.RequestAuth;
import com.benbanking.api.dto.MeResponse;
import com.benbanking.api.dto.UpdateProfileRequest;
import com.benbanking.api.enums.ActivityType;
import com.benbanking.api.models.Log;
import com.benbanking.api.models.User;
import com.benbanking.api.repositories.LogRepository;
import com.benbanking.api.repositories.UserRepository;
import com.benbanking.api.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final LogRepository logRepository;
    private final AuthService authService;

    public ProfileController(UserRepository userRepository, LogRepository logRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.logRepository = logRepository;
        this.authService = authService;
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(HttpServletRequest request, @Valid @RequestBody UpdateProfileRequest body) {
        AuthSession session = RequestAuth.requireSession(request);

        userRepository.updateProfile(
                session.getUserId(), body.getFirstName(), body.getLastName(),
                body.getPhoneNumber(), body.getEmail(), body.getAddress()
        );

        logRepository.save(new Log(0, session.getUserId(), ActivityType.UPDATE_PERSONAL_INFORMATION,
                "User updated their profile.", LocalDateTime.now()));

        User updated = authService.requireUser(session.getUserId());
        return ResponseEntity.ok(MeResponse.from(updated));
    }
}
