package com.benbanking.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionQueryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void paginationClampsSizeAndPaginatesCorrectly() throws Exception {
        String token = registerAndLogin("txq_page");
        int accountId = openAccount(token);

        deposit(token, accountId, "150.00", "INCOME");
        deposit(token, accountId, "50.00", "INCOME");
        withdraw(token, accountId, "20.00", "GROCERIES");
        withdraw(token, accountId, "10.00", "DINING");

        JsonNode page0 = getJson("/api/transactions?page=0&size=2", token);
        assertEquals(2, page0.get("items").size());
        assertEquals(0, page0.get("page").asInt());
        assertEquals(2, page0.get("size").asInt());
        assertEquals(4, page0.get("totalItems").asInt());
        assertEquals(2, page0.get("totalPages").asInt());

        JsonNode page1 = getJson("/api/transactions?page=1&size=2", token);
        assertEquals(2, page1.get("items").size());

        // size is clamped to [1,100] server-side (invariant #15).
        JsonNode oversized = getJson("/api/transactions?size=500", token);
        assertEquals(100, oversized.get("size").asInt());

        JsonNode undersized = getJson("/api/transactions?size=0", token);
        assertEquals(1, undersized.get("size").asInt());
    }

    @Test
    void filtersByCategoryAndType() throws Exception {
        String token = registerAndLogin("txq_filter");
        int accountId = openAccount(token);

        deposit(token, accountId, "150.00", "INCOME");
        withdraw(token, accountId, "20.00", "GROCERIES");
        withdraw(token, accountId, "10.00", "DINING");

        JsonNode groceries = getJson("/api/transactions?category=GROCERIES", token);
        assertEquals(1, groceries.get("items").size());
        assertEquals("GROCERIES", groceries.get("items").get(0).get("category").asText());

        JsonNode withdrawals = getJson("/api/transactions?type=WITHDRAWAL", token);
        assertEquals(2, withdrawals.get("items").size());
    }

    @Test
    void ownershipScopingHidesOtherUsersTransactions() throws Exception {
        String tokenA = registerAndLogin("txq_owner_a");
        int accountA = openAccount(tokenA);
        deposit(tokenA, accountA, "100.00", "INCOME");

        String tokenB = registerAndLogin("txq_owner_b");

        JsonNode resultB = getJson("/api/transactions", tokenB);
        assertEquals(0, resultB.get("items").size());
    }

    @Test
    void exportProducesCsvWithHeaderAndRows() throws Exception {
        String token = registerAndLogin("txq_export");
        int accountId = openAccount(token);
        deposit(token, accountId, "88.00", "INCOME");

        MvcResult result = mockMvc.perform(get("/api/transactions/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertNotNull(contentType);
        assertTrue(contentType.contains("text/csv"));

        String body = result.getResponse().getContentAsString();
        String[] lines = body.strip().split("\n");
        assertTrue(lines.length >= 2, "expected a header row plus at least one data row");
        assertTrue(lines[0].startsWith("reference,accountId"));
        assertTrue(body.contains("88.00"));
        assertTrue(body.contains("INCOME"));
    }

    private String registerAndLogin(String username) throws Exception {
        username = username + "_" + System.nanoTime();
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

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private int openAccount(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountType":"CHECKING","openingBalance":0}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accountId").asInt();
    }

    private void deposit(String token, int accountId, String amount, String category) throws Exception {
        mockMvc.perform(post("/api/accounts/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":%d,"amount":%s,"category":"%s"}
                                """.formatted(accountId, amount, category)))
                .andExpect(status().isOk());
    }

    private void withdraw(String token, int accountId, String amount, String category) throws Exception {
        mockMvc.perform(post("/api/accounts/withdraw")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":%d,"amount":%s,"category":"%s"}
                                """.formatted(accountId, amount, category)))
                .andExpect(status().isOk());
    }

    private JsonNode getJson(String uri, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(uri).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
