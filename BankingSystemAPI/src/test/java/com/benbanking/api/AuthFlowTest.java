package com.benbanking.api;

import com.benbanking.api.enums.ActivityType;
import com.benbanking.api.models.Log;
import com.benbanking.api.models.User;
import com.benbanking.api.repositories.LogRepository;
import com.benbanking.api.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerLoginMeReturnsFullProfile() throws Exception {
        String username = uniqueUsername("flow_meflow");
        register(username);

        JsonNode loginResult = login(username, "Demo123!");
        String token = loginResult.get("token").asText();

        MvcResult meResult = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode me = objectMapper.readTree(meResult.getResponse().getContentAsString());

        assertEquals(username, me.get("username").asText());
        assertEquals("Test", me.get("firstName").asText());
        assertEquals("USER", me.get("role").asText());
        assertTrue(me.has("createdAt"));
        assertFalse(me.has("password"));
    }

    @Test
    void wrongPasswordReturns401AndLogsLoginFailed() throws Exception {
        String username = uniqueUsername("flow_wrongpw");
        register(username);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"WrongPass1!"}
                                """.formatted(username)))
                .andExpect(status().isUnauthorized());

        User user = userRepository.findByUsername(username);
        assertNotNull(user);
        List<Log> logs = logRepository.findByUserId(user.getId());
        assertTrue(logs.stream().anyMatch(l -> l.getActivityType() == ActivityType.LOGIN_FAILED));
    }

    @Test
    void passwordChangeRevokesOtherSessionsButKeepsCurrent() throws Exception {
        String username = uniqueUsername("flow_pwchange");
        String tokenA = register(username);
        String tokenB = login(username, "Demo123!").get("token").asText();

        // Both sessions valid before the change.
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenA)).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenB)).andExpect(status().isOk());

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Demo123!","newPassword":"NewPass456!"}
                                """))
                .andExpect(status().isOk());

        // Caller's own session (tokenA) survives; every other session (tokenB) is revoked.
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenA)).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenB)).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredSessionIsRejected() throws Exception {
        String username = uniqueUsername("flow_expired");
        String token = register(username);

        String tokenHash = sha256Hex(token);
        int updated = jdbcTemplate.update(
                "UPDATE sessions SET expires_at = ? WHERE token_hash = ?",
                Timestamp.valueOf(LocalDateTime.now().minusHours(1)), tokenHash
        );
        assertEquals(1, updated, "expected to find exactly one session row for the freshly registered token");

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminIsForbiddenFromAdminRoutes() throws Exception {
        String username = uniqueUsername("flow_nonadmin");
        String token = register(username);

        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + System.nanoTime();
    }

    private String register(String username) throws Exception {
        String body = """
                {
                  "username":"%s",
                  "password":"Demo123!",
                  "firstName":"Test",
                  "lastName":"User",
                  "phoneNumber":"555-0100",
                  "email":"%s@example.com",
                  "address":"1 Test St"
                }
                """.formatted(username, username);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private JsonNode login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Mirrors SessionService's private hashing so the test can locate a session row directly. */
    private String sha256Hex(String token) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashed);
    }
}
