package com.benbanking.api.auth;

import com.benbanking.api.models.Session;
import com.benbanking.api.models.User;
import com.benbanking.api.repositories.SessionRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {

    public static final String REQUEST_ATTRIBUTE = "authSession";

    private static final Duration EXPIRY = Duration.ofHours(24);
    private static final Duration LAST_SEEN_THROTTLE = Duration.ofSeconds(60);

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        sessionRepository.insert(hash(token), user.getId(), now, now.plus(EXPIRY), now);
        return token;
    }

    public Optional<AuthSession> findSession(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String tokenHash = hash(token);
        Optional<Session> row = sessionRepository.findByTokenHash(tokenHash);
        if (row.isEmpty()) {
            return Optional.empty();
        }

        Session session = row.get();
        LocalDateTime now = LocalDateTime.now();
        if (session.getExpiresAt().isBefore(now)) {
            sessionRepository.deleteByTokenHash(tokenHash);
            return Optional.empty();
        }

        if (Duration.between(session.getLastSeenAt(), now).compareTo(LAST_SEEN_THROTTLE) > 0) {
            sessionRepository.touchLastSeen(tokenHash, now);
        }

        return Optional.of(new AuthSession(session.getUserId(), session.getUsername(), session.getRole()));
    }

    public void invalidate(String token) {
        if (token != null && !token.isBlank()) {
            sessionRepository.deleteByTokenHash(hash(token));
        }
    }

    /** Password change: revoke every other session for this user, keep the caller's own token alive. */
    public void invalidateOtherSessions(int userId, String currentToken) {
        if (currentToken == null || currentToken.isBlank()) {
            return;
        }
        sessionRepository.deleteOtherSessions(userId, hash(currentToken));
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available.", e);
        }
    }
}
