package daniil.pavelev.incident.service;

import daniil.pavelev.config.LlmProperties;
import daniil.pavelev.incident.domain.AnalysisStatus;
import daniil.pavelev.incident.domain.IncidentAnalysisEntity;
import daniil.pavelev.incident.domain.OrderedTextItem;
import daniil.pavelev.incident.memory.IncidentMemoryRetriever;
import daniil.pavelev.incident.memory.MemoryRetrievalResult;
import daniil.pavelev.incident.persistence.IncidentAnalysisRepository;
import daniil.pavelev.incident.signal.DescriptionNormalizer;
import daniil.pavelev.incident.signal.ExtractedSignals;
import daniil.pavelev.incident.signal.SignalExtractor;
import daniil.pavelev.llm.LlmAnalysisRequest;
import daniil.pavelev.llm.LlmAnalysisResponse;
import daniil.pavelev.llm.LlmClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class IncidentAnalysisService {

    private static final List<String> STATIC_KNOWLEDGE = List.of(
            "Prefer reproducible checks before speculative restarts.",
            "Correlate HTTP status codes with upstream dependency health.",
            "Do not expose secrets in analysis notes."
    );

    private final IncidentAnalysisRepository repository;
    private final DescriptionNormalizer normalizer;
    private final SignalExtractor signalExtractor;
    private final IncidentMemoryRetriever memoryRetriever;
    private final LlmClient llmClient;
    private final LlmProperties llmProperties;

    public IncidentAnalysisService(
            IncidentAnalysisRepository repository,
            DescriptionNormalizer normalizer,
            SignalExtractor signalExtractor,
            IncidentMemoryRetriever memoryRetriever,
            LlmClient llmClient,
            LlmProperties llmProperties
    ) {
        this.repository = repository;
        this.normalizer = normalizer;
        this.signalExtractor = signalExtractor;
        this.memoryRetriever = memoryRetriever;
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
    }

    @Transactional
    public AnalysisOutcome analyze(String originalDescription) {
        Instant started = Instant.now();
        String normalized = normalizer.normalize(originalDescription);
        ExtractedSignals signals = signalExtractor.extract(normalized);

        MemoryRetrievalResult memory = memoryRetriever.retrieveSafely(signals, null);

        LlmAnalysisResponse llmResponse;
        try {
            llmResponse = llmClient.analyze(new LlmAnalysisRequest(
                    normalized,
                    signals,
                    memory.selectedIncidents(),
                    memory.selectedAggregates(),
                    STATIC_KNOWLEDGE
            ));
        } catch (Exception ex) {
            IncidentAnalysisEntity failed = new IncidentAnalysisEntity(
                    UUID.randomUUID(),
                    originalDescription,
                    normalized,
                    started
            );
            applySignals(failed, signals);
            failed.setStatus(AnalysisStatus.FAILED);
            failed.setCompletedAt(Instant.now());
            failed.setErrorCode("LLM_FAILURE");
            failed.setProcessingDurationMs(Math.max(0, Instant.now().toEpochMilli() - started.toEpochMilli()));
            failed.setModel(llmProperties.getModel());
            repository.save(failed);
            throw new AnalysisPersistenceException("LLM analysis failed", ex);
        }

        IncidentAnalysisEntity entity = new IncidentAnalysisEntity(
                UUID.randomUUID(),
                originalDescription,
                normalized,
                started
        );
        applySignals(entity, signals);
        entity.setCategory(llmResponse.category());
        entity.setSeverity(llmResponse.severity());
        entity.setSummary(llmResponse.summary());
        entity.setHypotheses(toOrdered(llmResponse.hypotheses()));
        entity.setNextSteps(toOrdered(llmResponse.nextSteps()));
        entity.setStatus(AnalysisStatus.COMPLETED);
        entity.setCompletedAt(Instant.now());
        entity.setModel(llmResponse.model());
        entity.setLlmAttempts(llmResponse.attempts());
        entity.setPromptTokens(llmResponse.promptTokens());
        entity.setCompletionTokens(llmResponse.completionTokens());
        entity.setTotalTokens(llmResponse.totalTokens());
        entity.setProcessingDurationMs(Math.max(0, Instant.now().toEpochMilli() - started.toEpochMilli()));

        try {
            IncidentAnalysisEntity saved = repository.saveAndFlush(entity);
            // Force-initialize collections before leaving the transactional boundary.
            saved.getHypotheses().size();
            saved.getNextSteps().size();
            saved.getMentionedServices().size();
            saved.getProviderNames().size();
            saved.getHttpStatusCodes().size();
            saved.getKeywords().size();
            saved.getAffectedFunctionality().size();
            return new AnalysisOutcome(saved, memory, llmResponse.rawPromptSnapshot());
        } catch (AnalysisPersistenceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AnalysisPersistenceException(
                    "Analysis completed but persistence failed; refusing to pretend it was stored",
                    ex
            );
        }
    }

    private void applySignals(IncidentAnalysisEntity entity, ExtractedSignals signals) {
        entity.setMentionedServices(new java.util.LinkedHashSet<>(signals.mentionedServices()));
        entity.setProviderNames(new java.util.LinkedHashSet<>(signals.providerNames()));
        entity.setHttpStatusCodes(new java.util.LinkedHashSet<>(signals.httpStatusCodes()));
        entity.setKeywords(new java.util.LinkedHashSet<>(signals.keywords()));
        entity.setAffectedFunctionality(new java.util.LinkedHashSet<>(signals.affectedFunctionality()));
    }

    private List<OrderedTextItem> toOrdered(List<String> values) {
        List<OrderedTextItem> items = new ArrayList<>();
        if (values == null) {
            return items;
        }
        for (int i = 0; i < values.size(); i++) {
            items.add(new OrderedTextItem(i, values.get(i)));
        }
        return items;
    }

    public record AnalysisOutcome(
            IncidentAnalysisEntity entity,
            MemoryRetrievalResult memory,
            String promptSnapshot
    ) {
    }

    public static class AnalysisPersistenceException extends RuntimeException {
        public AnalysisPersistenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
