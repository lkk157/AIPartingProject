package com.aiparking.parking;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ParkingIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUpAdminToken() throws Exception {
        adminToken = login("admin", "admin123");
    }

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
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

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private long createLot(String name, String area, int status) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/lots")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","address":"测试路 1 号","area":"%s","status":%d}
                                """.formatted(name, area, status)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return json(result).path("data").path("id").asLong();
    }

    private void createFeeRule(long lotId) throws Exception {
        mockMvc.perform(post("/api/admin/lots/{lotId}/fee-rules", lotId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleName":"标准规则","freeMinutes":15,"firstHourCents":600,
                                 "extraPerHourCents":400,"dailyCapCents":4000}
                                """))
                .andExpect(status().isOk());
    }

    private void batchSpaces(long lotId, String prefix, int count) throws Exception {
        mockMvc.perform(post("/api/admin/lots/{lotId}/spaces/batch", lotId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"count":%d,"prefix":"%s","zone":"A区"}
                                """.formatted(count, prefix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(count));
    }

    @Test
    void should_create_lot_with_spaces_and_fee_rule_then_detail_counts() throws Exception {
        long lotId = createLot(unique("科技园停车场"), unique("南区"), 1);
        createFeeRule(lotId);
        batchSpaces(lotId, unique("A"), 3);

        mockMvc.perform(get("/api/admin/lots/{id}", lotId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spaceCount").value(3))
                .andExpect(jsonPath("$.data.freeCount").value(3))
                .andExpect(jsonPath("$.data.occupiedCount").value(0))
                .andExpect(jsonPath("$.data.enabledRule.ruleName").value("标准规则"))
                .andExpect(jsonPath("$.data.lot.capacity").value(3));
    }

    @Test
    void should_filter_lots_by_area() throws Exception {
        String area = unique("东区");
        createLot(unique("甲停车场"), area, 1);
        createLot(unique("乙停车场"), area, 1);
        createLot(unique("丙停车场"), unique("西区"), 1);

        MvcResult result = mockMvc.perform(get("/api/admin/lots")
                        .param("area", area)
                        .param("size", "50")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode records = json(result).path("data").path("records");
        assertThat(records).isNotEmpty();
        records.forEach(node -> assertThat(node.path("area").asText()).isEqualTo(area));
    }

    @Test
    void should_reject_duplicate_space_numbers() throws Exception {
        long lotId = createLot(unique("重复编号停车场"), unique("北区"), 1);
        String prefix = unique("D");
        batchSpaces(lotId, prefix, 2);

        mockMvc.perform(post("/api/admin/lots/{lotId}/spaces/batch", lotId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"count":2,"prefix":"%s","zone":"A区"}
                                """.formatted(prefix)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("车位编号已存在")));
    }

    @Test
    void should_reject_illegal_transition_from_maintenance_to_occupied() throws Exception {
        long lotId = createLot(unique("状态机停车场"), unique("中区"), 1);
        batchSpaces(lotId, unique("M"), 1);

        MvcResult listResult = mockMvc.perform(get("/api/admin/lots/{lotId}/spaces", lotId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        long spaceId = json(listResult).path("data").path("records").get(0).path("id").asLong();

        // 空闲 → 维护：允许
        mockMvc.perform(patch("/api/admin/spaces/{id}/status", spaceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":3,"reason":"设备检修"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statusText").value("维护"));

        // 维护 → 占用：拒绝
        mockMvc.perform(patch("/api/admin/spaces/{id}/status", spaceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":1,"reason":"直接占用"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("不允许")));
    }

    @Test
    void should_return_403_when_normal_user_calls_admin_lot_api() throws Exception {
        String username = unique("u");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk());
        String userToken = login(username, PASSWORD);

        mockMvc.perform(post("/api/admin/lots")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"越权停车场","address":"某路","area":"某区"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_hide_closed_lots_from_user_query() throws Exception {
        long closedId = createLot(unique("停业停车场"), unique("郊区"), 0);
        String userToken = login(registerAndReturnUsername(), PASSWORD);

        MvcResult result = mockMvc.perform(get("/api/lots")
                        .param("size", "100")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode records = json(result).path("data").path("records");
        records.forEach(node -> assertThat(node.path("id").asLong()).isNotEqualTo(closedId));
    }

    @Test
    void should_return_404_when_lot_missing() throws Exception {
        mockMvc.perform(get("/api/admin/lots/{id}", 99999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    private String registerAndReturnUsername() throws Exception {
        String username = unique("u");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk());
        return username;
    }
}
