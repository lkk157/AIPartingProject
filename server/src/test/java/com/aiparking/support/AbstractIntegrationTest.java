package com.aiparking.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 集成测试基类：使用真实 MySQL（本地 docker compose 或 CI service container）。
 * 测试库为 parking_test，Flyway 自动建表；每个用户用例使用随机用户名避免相互影响。
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
}
