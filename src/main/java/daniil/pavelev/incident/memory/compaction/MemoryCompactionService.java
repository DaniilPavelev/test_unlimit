package daniil.pavelev.incident.memory.compaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import daniil.pavelev.config.MemoryCompactionProperties;
import daniil.pavelev.incident.domain.AggregateMemoryEntity;
import daniil.pavelev.incident.domain.CompactionCheckpointEntity;
import daniil.pavelev.incident.domain.IncidentAnalysisEntity;
import daniil.pavelev.incident.domain.OrderedTextItem;
import daniil.pavelev.incident.persistence.AggregateMemoryRepository;
import daniil.pavelev.incident.persistence.CompactionCheckpointRepository;
import daniil.pavelev.incident.persistence.CompactionLockRepository;
import daniil.pavelev.incident.persistence.IncidentAnalysisRepository;
import daniil.pavelev.llm.LlmClient;
import daniil.pavelev.llm.LlmCompactionRequest;
import daniil.pavelev.llm.LlmCompactionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Creates aggregate long-term memory from completed analyses.
 * Never deletes or replaces source incident analysis records.
 * <p>
 * Locking is suitable for a single application instance. Multi-instance
 * deployments require a distributed lock.
 */
@Service
public class MemoryCompactionService {

    public static final String LOCK_NAME = "memory-compaction";

    private static final Logger log = LoggerFactory.getLogger(MemoryCompactionService.class);

    private final MemoryCompactionProperties properties;
    private final IncidentAnalysisRepository analysisRepository;
    private final AggregateMemoryRepository aggregateMemoryRepository;
    private final CompactionCheckpointRepository checkpointRepository;
    private final CompactionLockRepository lockRepository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate requiresNewTemplate;

    public MemoryCompactionService(
            MemoryCompactionProperties properties,
            IncidentAnalysisRepository analysisRepository,
            AggregateMemoryRepository aggregateMemoryRepository,
            CompactionCheckpointRepository checkpointRepository,
            CompactionLockRepository lockRepository,
            LlmClient llmClient,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.properties = properties;
        this.analysisRepository = analysisRepository;
        this.aggregateMemoryRepository = aggregateMemoryRepository;
        this.checkpointRepository = checkpointRepository;
        this.lockRepository = lockRepository;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    public CompactionResult compactIfNeeded() {
        if (!properties.isEnabled()) {
            return CompactionResult.skipped("disabled");
        }

        String owner = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant until = now.plus(Duration.ofMinutes(15));

        Boolean acquired = requiresNewTemplate.execute(status ->
                lockRepository.tryAcquire(LOCK_NAME, owner, until, now) > 0
        );
        if (acquired == null || !acquired) {
            return CompactionResult.skipped("lock-not-acquired");
        }

        try {
            return transactionTemplate.execute(status -> {
                try {
                    return runCompaction(now);
                } catch (Exception ex) {
                    status.setRollbackOnly();
                    log.error("Compaction failed; source records preserved for retry", ex);
                    return CompactionResult.failed(ex.getMessage());
                }
            });
        } finally {
            requiresNewTemplate.executeWithoutResult(status ->
                    lockRepository.release(LOCK_NAME, owner)
            );
        }
    }

    public CompactionResult compactSafely() {
        try {
            CompactionResult result = compactIfNeeded();
            return result == null ? CompactionResult.failed("null-result") : result;
        } catch (Exception ex) {
            log.error("Compaction failed unexpectedly", ex);
            return CompactionResult.failed(ex.getMessage());
        }
    }

    private CompactionResult runCompaction(Instant now) throws JsonProcessingException {
        CompactionCheckpointEntity checkpoint = checkpointRepository.findById(1)
                .orElseGet(() -> checkpointRepository.save(new CompactionCheckpointEntity(1)));

        Instant after = checkpoint.getLastCompactedCreatedAt();
        long pending = analysisRepository.countUncompactedCompleted(after);
        if (pending < properties.getMinimumPending()) {
            return CompactionResult.skipped("below-threshold:" + pending);
        }

        int limit = Math.min(properties.getBatchSize(), properties.getMaxSourceItemsPerRun());
        List<IncidentAnalysisEntity> batch = analysisRepository.findUncompactedCompleted(
                after,
                PageRequest.of(0, limit)
        );
        if (batch.isEmpty()) {
            return CompactionResult.skipped("empty-batch");
        }

        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        Map<String, Long> severityCounts = new LinkedHashMap<>();
        Map<String, Long> serviceCounts = new LinkedHashMap<>();
        Map<String, Long> providerCounts = new LinkedHashMap<>();

        List<String> summaries = new ArrayList<>();
        List<String> hypotheses = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();

        Instant periodStart = batch.getFirst().getCreatedAt();
        Instant periodEnd = batch.getFirst().getCreatedAt();

        for (IncidentAnalysisEntity analysis : batch) {
            if (analysis.getCreatedAt().isBefore(periodStart)) {
                periodStart = analysis.getCreatedAt();
            }
            if (analysis.getCreatedAt().isAfter(periodEnd)) {
                periodEnd = analysis.getCreatedAt();
            }
            if (analysis.getCategory() != null) {
                categoryCounts.merge(analysis.getCategory().name(), 1L, Long::sum);
            }
            if (analysis.getSeverity() != null) {
                severityCounts.merge(analysis.getSeverity().name(), 1L, Long::sum);
            }
            analysis.getMentionedServices().forEach(s -> serviceCounts.merge(s, 1L, Long::sum));
            analysis.getProviderNames().forEach(p -> providerCounts.merge(p, 1L, Long::sum));
            if (analysis.getSummary() != null) {
                summaries.add(analysis.getSummary());
            }
            analysis.getHypotheses().stream()
                    .sorted(Comparator.comparingInt(OrderedTextItem::getPosition))
                    .map(OrderedTextItem::getText)
                    .forEach(hypotheses::add);
            analysis.getNextSteps().stream()
                    .sorted(Comparator.comparingInt(OrderedTextItem::getPosition))
                    .map(OrderedTextItem::getText)
                    .forEach(diagnostics::add);
        }

        LlmCompactionResponse textual = llmClient.compactTextualPatterns(
                new LlmCompactionRequest(summaries, hypotheses, diagnostics)
        );

        List<String> frequentServices = topKeys(serviceCounts, 10);
        List<String> frequentProviders = topKeys(providerCounts, 10);

        long checkpointValue = periodEnd.toEpochMilli();
        AggregateMemoryEntity aggregate = new AggregateMemoryEntity(
                UUID.randomUUID(),
                periodStart,
                periodEnd,
                batch.size(),
                objectMapper.writeValueAsString(categoryCounts),
                objectMapper.writeValueAsString(severityCounts),
                objectMapper.writeValueAsString(frequentServices),
                objectMapper.writeValueAsString(frequentProviders),
                textual.recurringPatterns(),
                textual.effectiveDiagnosticPatterns(),
                now,
                textual.model(),
                checkpointValue
        );
        aggregateMemoryRepository.save(aggregate);

        List<UUID> ids = batch.stream().map(IncidentAnalysisEntity::getId).toList();
        analysisRepository.markCompacted(ids);

        checkpoint.setLastCompactedCreatedAt(periodEnd);
        checkpoint.setLastRunAt(now);
        checkpoint.setLastSourceCount(batch.size());
        checkpointRepository.save(checkpoint);

        return CompactionResult.success(batch.size(), aggregate.getId());
    }

    private List<String> topKeys(Map<String, Long> counts, int limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public record CompactionResult(boolean ran, String status, int sourceCount, UUID aggregateId) {
        static CompactionResult skipped(String reason) {
            return new CompactionResult(false, reason, 0, null);
        }

        static CompactionResult failed(String reason) {
            return new CompactionResult(false, "failed:" + reason, 0, null);
        }

        static CompactionResult success(int sourceCount, UUID aggregateId) {
            return new CompactionResult(true, "success", sourceCount, aggregateId);
        }
    }
}
