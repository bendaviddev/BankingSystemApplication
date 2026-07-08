package com.benbanking.api.controllers;

import com.benbanking.api.auth.RequestAuth;
import com.benbanking.api.services.AlertService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<?> getAlerts(HttpServletRequest request) {
        int userId = RequestAuth.requireSession(request).getUserId();
        return ResponseEntity.ok(alertService.getAlertsForUser(userId));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(HttpServletRequest request, @PathVariable("id") int alertId) {
        int userId = RequestAuth.requireSession(request).getUserId();
        alertService.markRead(alertId, userId);
        return ResponseEntity.ok(Map.of("message", "Alert marked as read."));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead(HttpServletRequest request) {
        int userId = RequestAuth.requireSession(request).getUserId();
        alertService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("message", "All alerts marked as read."));
    }
}
