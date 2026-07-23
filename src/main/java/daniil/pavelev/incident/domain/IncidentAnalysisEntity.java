package daniil.pavelev.incident.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "incident_analyses")
public class IncidentAnalysisEntity {

    @Id
    private UUID id;

    @Column(name = "original_description", nullable = false, columnDefinition = "TEXT")
    private String originalDescription;

    @Column(name = "normalized_description", nullable = false, columnDefinition = "TEXT")
    private String normalizedDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 64)
    private IncidentCategory category;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 32)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AnalysisStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "model", length = 128)
    private String model;

    @Column(name = "llm_attempts")
    private Integer llmAttempts;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "compacted", nullable = false)
    private boolean compacted;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "incident_hypotheses", joinColumns = @JoinColumn(name = "analysis_id"))
    @OrderBy("position ASC")
    private List<OrderedTextItem> hypotheses = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "incident_next_steps", joinColumns = @JoinColumn(name = "analysis_id"))
    @OrderBy("position ASC")
    private List<OrderedTextItem> nextSteps = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "incident_mentioned_services", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "service_name", nullable = false, length = 256)
    private Set<String> mentionedServices = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "incident_provider_names", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "provider_name", nullable = false, length = 256)
    private Set<String> providerNames = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "incident_http_status_codes", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "status_code", nullable = false)
    private Set<Integer> httpStatusCodes = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "incident_keywords", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "keyword", nullable = false, length = 256)
    private Set<String> keywords = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "incident_affected_functionality", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "functionality", nullable = false, length = 512)
    private Set<String> affectedFunctionality = new LinkedHashSet<>();

    protected IncidentAnalysisEntity() {
    }

    public IncidentAnalysisEntity(UUID id, String originalDescription, String normalizedDescription, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.originalDescription = Objects.requireNonNull(originalDescription);
        this.normalizedDescription = Objects.requireNonNull(normalizedDescription);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.status = AnalysisStatus.PENDING;
        this.compacted = false;
    }

    public UUID getId() {
        return id;
    }

    public String getOriginalDescription() {
        return originalDescription;
    }

    public String getNormalizedDescription() {
        return normalizedDescription;
    }

    public IncidentCategory getCategory() {
        return category;
    }

    public void setCategory(IncidentCategory category) {
        this.category = category;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IncidentSeverity severity) {
        this.severity = severity;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getLlmAttempts() {
        return llmAttempts;
    }

    public void setLlmAttempts(Integer llmAttempts) {
        this.llmAttempts = llmAttempts;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Long getProcessingDurationMs() {
        return processingDurationMs;
    }

    public void setProcessingDurationMs(Long processingDurationMs) {
        this.processingDurationMs = processingDurationMs;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public boolean isCompacted() {
        return compacted;
    }

    public void setCompacted(boolean compacted) {
        this.compacted = compacted;
    }

    public List<OrderedTextItem> getHypotheses() {
        return hypotheses;
    }

    public void setHypotheses(List<OrderedTextItem> hypotheses) {
        this.hypotheses = hypotheses;
    }

    public List<OrderedTextItem> getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(List<OrderedTextItem> nextSteps) {
        this.nextSteps = nextSteps;
    }

    public Set<String> getMentionedServices() {
        return mentionedServices;
    }

    public void setMentionedServices(Set<String> mentionedServices) {
        this.mentionedServices = mentionedServices;
    }

    public Set<String> getProviderNames() {
        return providerNames;
    }

    public void setProviderNames(Set<String> providerNames) {
        this.providerNames = providerNames;
    }

    public Set<Integer> getHttpStatusCodes() {
        return httpStatusCodes;
    }

    public void setHttpStatusCodes(Set<Integer> httpStatusCodes) {
        this.httpStatusCodes = httpStatusCodes;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    public Set<String> getAffectedFunctionality() {
        return affectedFunctionality;
    }

    public void setAffectedFunctionality(Set<String> affectedFunctionality) {
        this.affectedFunctionality = affectedFunctionality;
    }
}
