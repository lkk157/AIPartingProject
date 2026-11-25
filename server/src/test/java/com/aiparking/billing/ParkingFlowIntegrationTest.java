package com.aiparking.billing;

import com.aiparking.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ParkingFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;

    @BeforeEach
    void setUpAdminToken() throws Exception {
        adminToken = login("admin", "admin123");
    }

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).path("data").path("token").asText();
    }

    private String registerUser() throws Exception {
        String username = unique("p");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk());
        return login(username, PASSWORD);
    }

    private long createLotWithSpaces(int spaceCount) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        MvcResult lotResult = mockMvc.perform(post("/api/admin/lots")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"流程停车场%s","address":"流程路 1 号","area":"流程区%s"}
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
                                {"count":%d,"prefix":"P%s","zone":"A区"}
                                """.formatted(spaceCount, suffix.substring(0, 4))))
                .andExpect(status().isOk());

        return lotId;
    }

    private long firstSpaceId(long lotId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/lots/{lotId}/spaces", lotId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).path("data").path("records").get(0).path("id").asLong();
    }

    @Test
    void should_complete_reserve_entry_exit_pay_flow() throws Exception {
        long lotId = createLotWithSpaces(1);
        long spaceId = firstSpaceId(lotId);
        String userToken = registerUser();

        LocalDateTime start = LocalDateTime.now().plusHours(1).truncatedTo(java.time.temporal.ChronoUnit.HOURS);
        MvcResult reserveResult = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lotId":%d,"spaceId":%d,"startTime":"%s","hours":2}
                                """.formatted(lotId, spaceId, start)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statusText").value("待使用"))
                .andReturn();
        long reservationId = json(reserveResult).path("data").path("id").asLong();

        // 预约占用后车位进入「预约锁定」
        mockMvc.perform(get("/api/lots/{lotId}/spaces", lotId)
                        .param("status", "2")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(spaceId));

        MvcResult entryResult = mockMvc.perform(post("/api/parking/entry")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lotId":%d,"spaceId":%d,"plateNo":"粤B12345","reservationId":%d}
                                """.formatted(lotId, spaceId, reservationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statusText").value("停车中"))
                .andReturn();
        long recordId = json(entryResult).path("data").path("id").asLong();

        // 模拟停车 90 分钟
        jdbcTemplate.update("UPDATE parking_record SET entry_time = ? WHERE id = ?",
                LocalDateTime.now().minusMinutes(90), recordId);

        MvcResult exitResult = mockMvc.perform(post("/api/parking/records/{recordId}/exit", recordId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amountCents").value(1000))
                .andExpect(jsonPath("$.data.billedMinutes").value(90))
                .andExpect(jsonPath("$.data.statusText").value("待支付"))
                .andReturn();
        long orderId = json(exitResult).path("data").path("id").asLong();

        mockMvc.perform(post("/api/orders/{orderId}/pay", orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1));

        // 重复支付被拒绝
        mockMvc.perform(post("/api/orders/{orderId}/pay", orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单当前状态不可支付"));

        // 离场后车位回到空闲
        mockMvc.perform(get("/api/lots/{lotId}/spaces", lotId)
                        .param("status", "0")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(spaceId));

        // 预约进入已完成
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].statusText").value("已完成"));

        // 订单列表可见且为已支付
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[0].statusText").value("已支付"));
    }

    @Test
    void should_expose_current_parking_and_enrich_order_and_lot_list() throws Exception {
        long lotId = createLotWithSpaces(1);
        long spaceId = firstSpaceId(lotId);
        String userToken = registerUser();

        MvcResult entryResult = mockMvc.perform(post("/api/parking/entry")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lotId":%d,"spaceId":%d,"plateNo":"粤B66666"}
                                """.formatted(lotId, spaceId)))
                .andExpect(status().isOk())
                .andReturn();
        long recordId = json(entryResult).path("data").path("id").asLong();

        mockMvc.perform(get("/api/parking/current")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId").value(recordId))
                .andExpect(jsonPath("$.data.lotName").exists())
                .andExpect(jsonPath("$.data.spaceNo").exists())
                .andExpect(jsonPath("$.data.estimatedCents").isNumber())
                .andExpect(jsonPath("$.data.elapsedMinutes").isNumber());

        mockMvc.perform(post("/api/parking/records/{recordId}/exit", recordId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amountCents").value(0));

        MvcResult currentAfterExit = mockMvc.perform(get("/api/parking/current")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode currentNode = json(currentAfterExit).path("data");
        assertThat(currentNode.isNull() || currentNode.isMissingNode()).isTrue();

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].lotName").exists())
                .andExpect(jsonPath("$.data.records[0].spaceNo").exists());

        mockMvc.perform(get("/api/lots")
                        .param("size", "50")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].freeCount").isNumber())
                .andExpect(jsonPath("$.data.records[0].priceFromCents").isNumber());
    }

    @Test
    void should_release_space_after_reservation_cancelled() throws Exception {
        long lotId = createLotWithSpaces(1);
        long spaceId = firstSpaceId(lotId);
        String userToken = registerUser();

        LocalDateTime start = LocalDate.now().plusDays(3).atTime(LocalTime.of(9, 0));
        MvcResult reserveResult = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lotId":%d,"spaceId":%d,"startTime":"%s","hours":1}
                                """.formatted(lotId, spaceId, start)))
                .andExpect(status().isOk())
                .andReturn();
        long reservationId = json(reserveResult).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/reservations/{id}", reservationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lots/{lotId}/spaces", lotId)
                        .param("status", "0")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(spaceId));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].statusText").value("已取消"));
    }

    @Test
    void should_reject_entry_when_space_belongs_to_another_lot() throws Exception {
        long lotId = createLotWithSpaces(1);
        long otherLotId = createLotWithSpaces(1);
        long spaceId = firstSpaceId(otherLotId);
        String userToken = registerUser();

        mockMvc.perform(post("/api/parking/entry")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lotId":%d,"spaceId":%d,"plateNo":"粤B12345"}
                                """.formatted(lotId, spaceId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("车位不属于该停车场"));
    }

    @Test
    void should_return_free_order_when_parking_within_free_minutes() throws Exception {
        long lotId = createLotWithSpaces(1);
        long spaceId = firstSpaceId(lotId);
        String userToken = registerUser();

        MvcResult entryResult = mockMvc.perform(post("/api/parking/entry")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lotId":%d,"spaceId":%d,"plateNo":"粤B88888"}
                                """.formatted(lotId, spaceId)))
                .andExpect(status().isOk())
                .andReturn();
        long recordId = json(entryResult).path("data").path("id").asLong();

        jdbcTemplate.update("UPDATE parking_record SET entry_time = ? WHERE id = ?",
                LocalDateTime.now().minusMinutes(5), recordId);

        mockMvc.perform(post("/api/parking/records/{recordId}/exit", recordId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amountCents").value(0))
                .andExpect(jsonPath("$.data.freeApplied").value(1))
                .andExpect(jsonPath("$.data.statusText").value("已支付"));

        assertThat(true).isTrue();
    }
}
