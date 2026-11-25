package com.aiparking.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发抢约：10 个线程同时预约同一车位同一时段，只允许 1 个成功。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ReservationConcurrencyTest {

    private static final int THREADS = 10;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    private ResponseEntity<String> call(HttpMethod method, String path, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        String payload = null;
        try {
            payload = body == null ? null : objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return rest.exchange(path, method, new HttpEntity<>(payload, headers), String.class);
    }

    private String login(String username, String password) throws Exception {
        ResponseEntity<String> response = call(HttpMethod.POST, "/api/auth/login",
                java.util.Map.of("username", username, "password", password), null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return objectMapper.readTree(response.getBody()).path("data").path("token").asText();
    }

    @Test
    void should_allow_only_one_active_reservation_per_user() throws Exception {
        String adminToken = login("admin", "admin123");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        ResponseEntity<String> lotResponse = call(HttpMethod.POST, "/api/admin/lots", java.util.Map.of(
                "name", "单预约停车场" + suffix,
                "address", "规则路 1 号",
                "area", "规则区" + suffix), adminToken);
        long lotId = objectMapper.readTree(lotResponse.getBody()).path("data").path("id").asLong();

        call(HttpMethod.POST, "/api/admin/lots/" + lotId + "/fee-rules", java.util.Map.of(
                "ruleName", "标准", "freeMinutes", 15, "firstHourCents", 600,
                "extraPerHourCents", 400, "dailyCapCents", 4000), adminToken);

        call(HttpMethod.POST, "/api/admin/lots/" + lotId + "/spaces/batch", java.util.Map.of(
                "count", 2, "prefix", "R" + suffix.substring(0, 4)), adminToken);

        ResponseEntity<String> spacesResponse = call(HttpMethod.GET,
                "/api/admin/lots/" + lotId + "/spaces", null, adminToken);
        JsonNode records = objectMapper.readTree(spacesResponse.getBody())
                .path("data").path("records");
        long firstSpace = records.get(0).path("id").asLong();
        long secondSpace = records.get(1).path("id").asLong();

        String username = "u" + suffix;
        call(HttpMethod.POST, "/api/auth/register", java.util.Map.of(
                "username", username, "password", "secret123"), null);
        String userToken = login(username, "secret123");

        LocalDateTime start = LocalDate.now().plusDays(3).atTime(LocalTime.of(9, 0));

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (int i = 0; i < THREADS; i++) {
            final long spaceId = i % 2 == 0 ? firstSpace : secondSpace;
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await(5, TimeUnit.SECONDS);
                    java.util.Map<String, Object> body = new java.util.HashMap<>();
                    body.put("lotId", lotId);
                    body.put("spaceId", spaceId);
                    body.put("startTime", start.toString());
                    body.put("hours", 1);
                    ResponseEntity<String> response = call(HttpMethod.POST, "/api/reservations", body, userToken);
                    if (response.getStatusCode().value() == 200) {
                        success.incrementAndGet();
                    } else {
                        rejected.incrementAndGet();
                    }
                } catch (Exception e) {
                    rejected.incrementAndGet();
                }
            });
        }

        ready.await(10, TimeUnit.SECONDS);
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(success.get()).as("同一车主并发预约只能成功一次").isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(THREADS - 1);

        ResponseEntity<String> listResponse = call(HttpMethod.GET, "/api/reservations", null, userToken);
        JsonNode list = objectMapper.readTree(listResponse.getBody()).path("data");
        assertThat(list.size()).isEqualTo(1);
    }

    @Test
    void should_allow_only_one_success_when_ten_threads_reserve_same_slot() throws Exception {
        String adminToken = login("admin", "admin123");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        ResponseEntity<String> lotResponse = call(HttpMethod.POST, "/api/admin/lots", java.util.Map.of(
                "name", "并发停车场" + suffix,
                "address", "并发路 1 号",
                "area", "并发区" + suffix), adminToken);
        long lotId = objectMapper.readTree(lotResponse.getBody()).path("data").path("id").asLong();

        call(HttpMethod.POST, "/api/admin/lots/" + lotId + "/fee-rules", java.util.Map.of(
                "ruleName", "标准", "freeMinutes", 15, "firstHourCents", 600,
                "extraPerHourCents", 400, "dailyCapCents", 4000), adminToken);

        call(HttpMethod.POST, "/api/admin/lots/" + lotId + "/spaces/batch", java.util.Map.of(
                "count", 1, "prefix", "C" + suffix.substring(0, 4)), adminToken);

        ResponseEntity<String> spacesResponse = call(HttpMethod.GET,
                "/api/admin/lots/" + lotId + "/spaces", null, adminToken);
        long spaceId = objectMapper.readTree(spacesResponse.getBody())
                .path("data").path("records").get(0).path("id").asLong();

        String username = "c" + suffix;
        call(HttpMethod.POST, "/api/auth/register", java.util.Map.of(
                "username", username, "password", "secret123"), null);
        String userToken = login(username, "secret123");

        LocalDateTime start = LocalDate.now().plusDays(2).atTime(LocalTime.of(10, 0));
        java.util.Map<String, Object> reservation = new java.util.HashMap<>();
        reservation.put("lotId", lotId);
        reservation.put("spaceId", spaceId);
        reservation.put("startTime", start.toString());
        reservation.put("hours", 2);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        List<Integer> unexpected = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await(5, TimeUnit.SECONDS);
                    ResponseEntity<String> response = call(HttpMethod.POST, "/api/reservations",
                            reservation, userToken);
                    int code = response.getStatusCode().value();
                    if (code == 200) {
                        success.incrementAndGet();
                    } else if (code == 400 || code == 409) {
                        conflict.incrementAndGet();
                    } else {
                        synchronized (unexpected) {
                            unexpected.add(code);
                        }
                    }
                } catch (Exception e) {
                    synchronized (unexpected) {
                        unexpected.add(-1);
                    }
                }
            });
        }

        ready.await(10, TimeUnit.SECONDS);
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(unexpected).isEmpty();
        assertThat(success.get()).as("只允许一个线程抢到同一时段").isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(THREADS - 1);

        ResponseEntity<String> listResponse = call(HttpMethod.GET, "/api/reservations", null, userToken);
        JsonNode list = objectMapper.readTree(listResponse.getBody()).path("data");
        assertThat(list.size()).isEqualTo(1);
    }
}
