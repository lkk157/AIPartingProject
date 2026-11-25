package com.aiparking.admin;

import com.aiparking.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminQueryIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(
                        result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("token").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void should_return_orders_and_reservations_when_admin_queries() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        MvcResult lotResult = mockMvc.perform(post("/api/admin/lots")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"查询停车场%s","address":"查询路","area":"查询区%s"}
                                """.formatted(suffix, suffix)))
                .andExpect(status().isOk())
                .andReturn();
        long lotId = json(lotResult).path("data").path("id").asLong();

        mockMvc.perform(post("/api/admin/lots/{lotId}/fee-rules", lotId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleName":"标准","freeMinutes":15,"firstHourCents":600,
                                 "extraPerHourCents":400,"dailyCapCents":4000}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/lots/{lotId}/spaces/batch", lotId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"count":2,"prefix":"Q%s","zone":"A区"}
                                """.formatted(suffix.substring(0, 4))))
                .andExpect(status().isOk());

        // 普通用户完成一次预约 + 入场离场，产生订单
        String username = "ad" + suffix;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk());
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        String userToken = json(loginResult).path("data").path("token").asText();

        MvcResult spacesResult = mockMvc.perform(get("/api/admin/lots/{lotId}/spaces", lotId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        long spaceId = json(spacesResult).path("data").path("records").get(0).path("id").asLong();

        LocalDateTime start = LocalDateTime.now().plusHours(1)
                .truncatedTo(java.time.temporal.ChronoUnit.HOURS);
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lotId":%d,"spaceId":%d,"startTime":"%s","hours":1}
                                """.formatted(lotId, spaceId, start)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());

        mockMvc.perform(get("/api/admin/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[0].lotName").exists());

        mockMvc.perform(get("/api/admin/stats/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lotCount").isNumber())
                .andExpect(jsonPath("$.data.spaceCount").isNumber())
                .andExpect(jsonPath("$.data.freeSpaces").isNumber())
                .andExpect(jsonPath("$.data.todayOrders").isNumber());
    }

    @Test
    void should_return_403_when_normal_user_queries_admin_stats() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "st" + suffix;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk());
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        String userToken = json(loginResult).path("data").path("token").asText();

        mockMvc.perform(get("/api/admin/stats/overview")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
