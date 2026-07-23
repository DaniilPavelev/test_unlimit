package daniil.pavelev.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void memoryProperties(DynamicPropertyRegistry registry) {
        registry.add("llm.mode", () -> "stub");
        registry.add("memory.compaction.enabled", () -> "false");
        registry.add("memory.retrieval.max-incidents", () -> "5");
        registry.add("memory.retrieval.max-context-characters", () -> "6000");
        registry.add("memory.retrieval.lookback-days", () -> "180");
        registry.add("memory.compaction.minimum-pending", () -> "10");
        registry.add("memory.compaction.batch-size", () -> "50");
    }
}
