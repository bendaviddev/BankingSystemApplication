package com.benbanking.api.controllers;

import com.benbanking.api.auth.RequestAuth;
import com.benbanking.api.services.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(HttpServletRequest request) {
        int userId = RequestAuth.requireSession(request).getUserId();
        return ResponseEntity.ok(analyticsService.getSummary(userId));
    }
}
