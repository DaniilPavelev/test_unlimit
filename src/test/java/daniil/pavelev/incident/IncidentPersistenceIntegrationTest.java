package daniil.pavelev.incident;

import daniil.pavelev.incident.api.dto.IncidentAnalysisDetailResponse;
import daniil.pavelev.incident.api.dto.IncidentStatisticsResponse;
import daniil.pavelev.incident.api.dto.PageResponse;
import daniil.pavelev.incident.domain.AnalysisStatus;
import daniil.pavelev.incident.domain.CompactionCheckpointEntity;
import daniil.pavelev.incident.domain.IncidentAnalysisEntity;
import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.domain.IncidentSeverity;
import daniil.pavelev.incident.memory.compaction.MemoryCompactionService;
import daniil.pavelev.incident.persistence.AggregateMemoryRepository;
import daniil.pavelev.incident.persistence.CompactionCheckpointRepository;
import daniil.pavelev.incident.persistence.IncidentAnalysisRepository;
import daniil.pavelev.incident.service.IncidentAnalysisService;
import daniil.pavelev.incident.service.IncidentHistoryService;
import daniil.pavelev.incident.service.IncidentStatisticsService;
import daniil.pavelev.llm.LlmClient;
import daniil.pavelev.llm.LlmCompactionRequest;
import daniil.pavelev.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
class IncidentPersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private IncidentAnalysisService analysisService;

    @Autowired
    private IncidentHistoryService historyService;

    @Autowired
    private IncidentStatisticsService statisticsService;

    @Autowired
    private IncidentAnalysisRepository analysisRepository;

    @Autowired
    private AggregateMemoryRepository aggregateMemoryRepository;

    @Autowired
    private CompactionCheckpointRepository checkpointRepository;

    @Autowired
    private MemoryCompactionService compactionService;

    @MockitoSpyBean
    private LlmClient llmClient;

    @DynamicPropertySource
    static void compactionThreshold(DynamicPropertyRegistry registry) {
        registry.add("memory.compaction.enabled", () -> "true");
        registry.add("memory.compaction.minimum-pending", () -> "3");
        registry.add("memory.compaction.batch-size", () -> "50");
        registry.add("memory.retrieval.max-incidents", () -> "5");
    }

    @BeforeEach
    void clean() {
        reset(llmClient);
        analysisRepository.deleteAll();
        aggregateMemoryRepository.deleteAll();
        CompactionCheckpointEntity checkpoint = checkpointRepository.findById(1)
                .orElseGet(() -> checkpointRepository.save(new CompactionCheckpointEntity(1)));
        checkpoint.reset();
        checkpointRepository.save(checkpoint);
    }

    @Test
    void savesAndLoadsCompleteAnalysis() {
        IncidentAnalysisService.AnalysisOutcome outcome = analysisService.analyze(
                "payment-service returned 502 from Stripe timeout while processing checkout"
        );

        IncidentAnalysisDetailResponse loaded = historyService.getById(outcome.entity().getId());

        assertThat(loaded.id()).isEqualTo(outcome.entity().getId());
        assertThat(loaded.status()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(loaded.mentionedServices()).contains("payment-service");
        assertThat(loaded.providerNames()).contains("Stripe");
        assertThat(loaded.httpStatusCodes()).contains(502);
        assertThat(loaded.hypotheses()).isNotEmpty();
        assertThat(loaded.nextSteps()).isNotEmpty();
        assertThat(loaded.summary()).isNotBlank();
    }

    @Test
    void paginatesHistory() {
        analysisService.analyze("payment-service outage with Stripe 500");
        analysisService.analyze("auth-service unavailable on AWS");
        analysisService.analyze("billing-service latency with Redis");

        PageResponse<?> page0 = historyService.findHistory(0, 2, null, null, null, null, null, null, null);
        PageResponse<?> page1 = historyService.findHistory(1, 2, null, null, null, null, null, null, null);

        assertThat(page0.items()).hasSize(2);
        assertThat(page0.totalElements()).isEqualTo(3);
        assertThat(page0.totalPages()).isEqualTo(2);
        assertThat(page1.items()).hasSize(1);
        assertThat(page0.size()).isEqualTo(2);
    }

    @Test
    void filtersByCategoryAndSeverity() {
        analysisService.analyze("critical outage payment-service down unavailable");
        analysisService.analyze("intermittent auth-service slow latency timeout");

        var byCategory = historyService.findHistory(
                0, 20, IncidentCategory.AVAILABILITY, null, null, null, null, null, null
        );
        var bySeverity = historyService.findHistory(
                0, 20, null, IncidentSeverity.CRITICAL, null, null, null, null, null
        );

        assertThat(byCategory.items()).isNotEmpty();
        assertThat(byCategory.items()).allMatch(i -> i.category() == IncidentCategory.AVAILABILITY);
        assertThat(bySeverity.items()).isNotEmpty();
        assertThat(bySeverity.items()).allMatch(i -> i.severity() == IncidentSeverity.CRITICAL);
    }

    @Test
    void filtersByServiceOrProvider() {
        analysisService.analyze("payment-service failed with Stripe 502");
        analysisService.analyze("inventory-service failed with Redis timeout");

        var byService = historyService.findHistory(0, 20, null, null, "payment-service", null, null, null, null);
        var byProvider = historyService.findHistory(0, 20, null, null, null, "Redis", null, null, null);

        assertThat(byService.items()).hasSize(1);
        assertThat(byService.items().getFirst().mentionedServices()).contains("payment-service");
        assertThat(byProvider.items()).hasSize(1);
        assertThat(byProvider.items().getFirst().providerNames()).contains("Redis");
    }

    @Test
    void statisticsAreDeterministicFromDatabase() {
        analysisService.analyze("payment-service Stripe 502 outage critical");
        analysisService.analyze("payment-service Stripe 503 degraded");
        analysisService.analyze("auth-service AWS unavailable");

        IncidentStatisticsResponse stats = statisticsService.statistics();

        assertThat(stats.totalAnalyses()).isEqualTo(3);
        assertThat(stats.completedAnalyses()).isEqualTo(3);
        assertThat(stats.countByCategory()).isNotEmpty();
        assertThat(stats.countBySeverity()).isNotEmpty();
        assertThat(stats.mostFrequentServices().getFirst().name()).isEqualTo("payment-service");
        assertThat(stats.mostFrequentProviders().getFirst().name()).isEqualTo("Stripe");
        assertThat(stats.analysesOverTime()).isNotEmpty();
    }

    @Test
    void newAnalysisUsesSelectedHistoricalMemoryOnly() {
        IncidentAnalysisService.AnalysisOutcome first = analysisService.analyze(
                "payment-service Stripe 502 timeout checkout"
        );
        IncidentAnalysisService.AnalysisOutcome second = analysisService.analyze(
                "payment-service Stripe 502 timeout refunds"
        );

        assertThat(second.memory().selectedAnalysisIds()).contains(first.entity().getId());
        assertThat(second.memory().selectedIncidents().size()).isLessThanOrEqualTo(5);
        assertThat(second.promptSnapshot()).contains(first.entity().getId().toString());
        assertThat(analysisRepository.count()).isEqualTo(2);
    }

    @Test
    void fallbackWhenNoRelevantMemoryExists() {
        IncidentAnalysisService.AnalysisOutcome first = analysisService.analyze(
                "payment-service Stripe 502"
        );
        IncidentAnalysisService.AnalysisOutcome unrelated = analysisService.analyze(
                "completely different narrative without known markers"
        );

        assertThat(first.memory().selectedAnalysisIds()).isEmpty();
        assertThat(unrelated.entity().getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
    }

    @Test
    void compactionThresholdAndIdempotencyPreserveSources() {
        for (int i = 0; i < 3; i++) {
            analysisService.analyze("payment-service Stripe 502 batch-" + i);
        }

        MemoryCompactionService.CompactionResult first = compactionService.compactSafely();
        assertThat(first.ran()).isTrue();
        assertThat(aggregateMemoryRepository.count()).isEqualTo(1);

        assertThat(analysisRepository.count()).isEqualTo(3);
        assertThat(analysisRepository.findAll()).allMatch(IncidentAnalysisEntity::isCompacted);

        MemoryCompactionService.CompactionResult second = compactionService.compactSafely();
        assertThat(second.ran()).isFalse();
        assertThat(aggregateMemoryRepository.count()).isEqualTo(1);
        assertThat(analysisRepository.count()).isEqualTo(3);
    }

    @Test
    void failedCompactionDoesNotMarkRecordsCompacted() {
        for (int i = 0; i < 3; i++) {
            analysisService.analyze("billing-service AWS 500 failure-" + i);
        }

        doThrow(new RuntimeException("boom")).when(llmClient).compactTextualPatterns(any(LlmCompactionRequest.class));

        MemoryCompactionService.CompactionResult result = compactionService.compactSafely();
        assertThat(result.ran()).isFalse();
        assertThat(result.status()).startsWith("failed:");
        assertThat(analysisRepository.findAll()).allMatch(a -> !a.isCompacted());
        assertThat(aggregateMemoryRepository.count()).isZero();
    }

    @Test
    void llmNeverReceivesFullDatabaseHistoryAndRespectsMaxIncidents() {
        for (int i = 0; i < 6; i++) {
            analysisService.analyze("payment-service Stripe 502 history-" + i);
        }

        IncidentAnalysisService.AnalysisOutcome latest = analysisService.analyze(
                "payment-service Stripe 502 newest"
        );

        assertThat(latest.memory().selectedIncidents()).hasSizeLessThanOrEqualTo(5);
        assertThat(latest.memory().selectedAnalysisIds()).hasSizeLessThanOrEqualTo(5);
        assertThat(analysisRepository.count()).isEqualTo(7);

        long originalsInPrompt = analysisRepository.findAll().stream()
                .map(IncidentAnalysisEntity::getOriginalDescription)
                .filter(desc -> latest.promptSnapshot().contains(desc))
                .count();
        assertThat(originalsInPrompt).isLessThanOrEqualTo(1);
    }
}
