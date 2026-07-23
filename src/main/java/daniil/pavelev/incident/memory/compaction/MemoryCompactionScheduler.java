package daniil.pavelev.incident.memory.compaction;

import daniil.pavelev.config.MemoryCompactionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled compaction runner. Runs asynchronously from user request threads.
 * For multi-instance deployments, replace the DB row lock with a distributed lock.
 */
@Component
public class MemoryCompactionScheduler {

    private static final Logger log = LoggerFactory.getLogger(MemoryCompactionScheduler.class);

    private final MemoryCompactionService compactionService;
    private final MemoryCompactionProperties properties;

    public MemoryCompactionScheduler(
            MemoryCompactionService compactionService,
            MemoryCompactionProperties properties
    ) {
        this.compactionService = compactionService;
        this.properties = properties;
    }

    @Scheduled(cron = "${memory.compaction.cron:0 0 2 * * *}")
    public void scheduledCompaction() {
        if (!properties.isEnabled()) {
            return;
        }
        MemoryCompactionService.CompactionResult result = compactionService.compactSafely();
        log.info("Scheduled compaction finished: {}", result);
    }
}
