package com.benbanking.api.auth;

import com.benbanking.api.models.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    public static final String REQUEST_ATTRIBUTE = "authSession";

    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new AuthSession(user.getId(), user.getUsername(), user.getRole()));
        return token;
    }

    public Optional<AuthSession> findSession(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(token));
    }

    public void invalidate(String token) {
        if (token != null && !token.isBlank()) {
            sessions.remove(token);
        }
    }
}
