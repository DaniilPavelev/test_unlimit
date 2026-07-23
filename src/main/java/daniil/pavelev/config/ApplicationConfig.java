package daniil.pavelev.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
        MemoryRetrievalProperties.class,
        MemoryCompactionProperties.class,
        IncidentHistoryProperties.class,
        LlmProperties.class
})
public class ApplicationConfig {
}
