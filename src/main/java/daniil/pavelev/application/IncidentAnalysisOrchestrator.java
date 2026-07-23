package daniil.pavelev.application;

import daniil.pavelev.domain.AnalysisMetadata;
import daniil.pavelev.domain.IncidentAnalysis;
import daniil.pavelev.domain.IncidentSignals;
import daniil.pavelev.domain.LlmAnalysisPayload;
import daniil.pavelev.domain.Severity;
import daniil.pavelev.exception.InvalidLlmOutputException;
import daniil.pavelev.exception.LlmResponseParseException;
import daniil.pavelev.repository.IncidentRepository;
import daniil.pavelev.llm.LlmClient;
import daniil.pavelev.service.AnalysisValidatorService;
import daniil.pavelev.service.InputNormalizerService;
import daniil.pavelev.service.IncidentPromptService;
import daniil.pavelev.service.IncidentPromptService.PromptBundle;
import daniil.pavelev.service.SignalExtractorService;
import daniil.pavelev.service.KnowledgeBaseService;
import daniil.pavelev.service.LlmResponseParser;
import daniil.pavelev.service.SimilarIncidentService;
import daniil.pavelev.service.SimilarIncidentService.ScoredHistory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IncidentAnalysisOrchestrator {

    private static final int MAX_ATTEMPTS = 2;
    private static final Logger log = LoggerFactory.getLogger(IncidentAnalysisOrchestrator.class);

    private final InputNormalizerService inputNormalizerService;
    private final SignalExtractorService signalExtractorService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final SimilarIncidentService similarIncidentService;
    private final IncidentPromptService promptService;
    private final LlmClient llmClient;
    private final LlmResponseParser responseParser;
    private final AnalysisValidatorService validator;
    private final IncidentRepository historyStore;

    public IncidentAnalysis analyze(String description) {
        long started = System.currentTimeMillis();
        UUID analysisId = UUID.randomUUID();

        String normalized = inputNormalizerService.normalize(description);
        IncidentSignals signals = signalExtractorService.extract(normalized);
        List<String> knowledge = knowledgeBaseService.selectRelevantKnowledge(signals);
        List<ScoredHistory> history = similarIncidentService.findHistory(signals, historyStore.findAll());

        PromptBundle prompt = promptService.buildAnalysisPrompt(
                description, normalized, signals, knowledge, history
        );

        String systemPrompt = prompt.systemPrompt();
        String userPrompt = prompt.userPrompt();
        LlmAnalysisPayload payload = null;
        int attempts = 0;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            attempts = attempt;
            log.info("stage=llm-call analysisId={} attempt={}", analysisId, attempt);
            String raw = llmClient.complete(systemPrompt, userPrompt).content();
            try {
                payload = responseParser.parse(raw);
                validator.validate(payload);
                log.info("stage=validate analysisId={} attempt={} success=true", analysisId, attempt);
                break;
            } catch (InvalidLlmOutputException | LlmResponseParseException ex) {
                List<String> errors = ex instanceof InvalidLlmOutputException invalid
                        ? invalid.getValidationErrors()
                        : List.of(ex.getMessage() == null ? "parse failed" : ex.getMessage());
                log.info("stage=validate analysisId={} attempt={} success=false errors={}", analysisId, attempt, errors);
                if (attempt == MAX_ATTEMPTS) {
                    throw new InvalidLlmOutputException(
                            "LLM output remained invalid after " + MAX_ATTEMPTS + " attempts",
                            errors
                    );
                }
                PromptBundle repair = promptService.buildRepairPrompt(raw == null ? "" : raw, errors);
                systemPrompt = repair.systemPrompt();
                userPrompt = repair.userPrompt();
            }
        }

        AnalysisMetadata metadata = new AnalysisMetadata(
                new ArrayList<>(signals.mentionedServices()),
                new ArrayList<>(signals.mentionedProviders()),
                prompt.selectedHistoricalIds(),
                attempts
        );

        IncidentAnalysis analysis = new IncidentAnalysis(
                analysisId,
                Instant.now(),
                description,
                normalized,
                payload.category().trim(),
                payload.summary().trim(),
                Severity.valueOf(payload.severity().trim()),
                validator.toHypotheses(payload),
                metadata
        );

        log.info("stage=persist-history analysisId={} durationMs={}", analysisId, System.currentTimeMillis() - started);
        historyStore.save(analysis);
        return analysis;
    }
}
