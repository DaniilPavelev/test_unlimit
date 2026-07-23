package daniil.pavelev.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "aggregate_memories")
public class AggregateMemoryEntity {

    @Id
    private UUID id;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "source_analysis_count", nullable = false)
    private int sourceAnalysisCount;

    @Column(name = "category_counts_json", nullable = false, columnDefinition = "TEXT")
    private String categoryCountsJson;

    @Column(name = "severity_counts_json", nullable = false, columnDefinition = "TEXT")
    private String severityCountsJson;

    @Column(name = "frequent_services_json", nullable = false, columnDefinition = "TEXT")
    private String frequentServicesJson;

    @Column(name = "frequent_providers_json", nullable = false, columnDefinition = "TEXT")
    private String frequentProvidersJson;

    @Column(name = "recurring_patterns", columnDefinition = "TEXT")
    private String recurringPatterns;

    @Column(name = "effective_diagnostic_patterns", columnDefinition = "TEXT")
    private String effectiveDiagnosticPatterns;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "compaction_model", length = 128)
    private String compactionModel;

    @Column(name = "source_checkpoint", nullable = false)
    private long sourceCheckpoint;

    protected AggregateMemoryEntity() {
    }

    public AggregateMemoryEntity(
            UUID id,
            Instant periodStart,
            Instant periodEnd,
            int sourceAnalysisCount,
            String categoryCountsJson,
            String severityCountsJson,
            String frequentServicesJson,
            String frequentProvidersJson,
            String recurringPatterns,
            String effectiveDiagnosticPatterns,
            Instant createdAt,
            String compactionModel,
            long sourceCheckpoint
    ) {
        this.id = Objects.requireNonNull(id);
        this.periodStart = Objects.requireNonNull(periodStart);
        this.periodEnd = Objects.requireNonNull(periodEnd);
        this.sourceAnalysisCount = sourceAnalysisCount;
        this.categoryCountsJson = Objects.requireNonNull(categoryCountsJson);
        this.severityCountsJson = Objects.requireNonNull(severityCountsJson);
        this.frequentServicesJson = Objects.requireNonNull(frequentServicesJson);
        this.frequentProvidersJson = Objects.requireNonNull(frequentProvidersJson);
        this.recurringPatterns = recurringPatterns;
        this.effectiveDiagnosticPatterns = effectiveDiagnosticPatterns;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.compactionModel = compactionModel;
        this.sourceCheckpoint = sourceCheckpoint;
    }

    public UUID getId() {
        return id;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public int getSourceAnalysisCount() {
        return sourceAnalysisCount;
    }

    public String getCategoryCountsJson() {
        return categoryCountsJson;
    }

    public String getSeverityCountsJson() {
        return severityCountsJson;
    }

    public String getFrequentServicesJson() {
        return frequentServicesJson;
    }

    public String getFrequentProvidersJson() {
        return frequentProvidersJson;
    }

    public String getRecurringPatterns() {
        return recurringPatterns;
    }

    public String getEffectiveDiagnosticPatterns() {
        return effectiveDiagnosticPatterns;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCompactionModel() {
        return compactionModel;
    }

    public long getSourceCheckpoint() {
        return sourceCheckpoint;
    }
}
