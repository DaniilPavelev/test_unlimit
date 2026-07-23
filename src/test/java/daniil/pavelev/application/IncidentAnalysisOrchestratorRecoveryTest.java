package daniil.pavelev.application;

import daniil.pavelev.config.IncidentProperties;
import daniil.pavelev.exception.InvalidLlmOutputException;
import daniil.pavelev.repository.IncidentRepository;
import daniil.pavelev.llm.LlmClient;
import daniil.pavelev.llm.LlmCompletion;
import daniil.pavelev.service.AnalysisValidatorService;
import daniil.pavelev.service.InputNormalizerService;
import daniil.pavelev.service.IncidentPromptService;
import daniil.pavelev.service.SignalExtractorService;
import daniil.pavelev.service.KnowledgeBaseService;
import daniil.pavelev.service.LlmResponseParser;
import daniil.pavelev.service.SimilarIncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentAnalysisOrchestratorRecoveryTest {

    private KnowledgeBaseService knowledgeBaseService;
    private IncidentRepository historyStore;
    private IncidentProperties properties;

    @BeforeEach
    void setUp() {
        properties = new IncidentProperties();
        var prompts = properties.getPrompts();
        prompts.setAnalysisSystem("analysis-system");
        prompts.setRepairSystem("Return corrected JSON only");
        prompts.setRepairUser("Validation errors:\n{errors}\n\nInvalid output:\n{invalidOutput}");
        prompts.setKnowledgeHeader("KNOWLEDGE");
        prompts.setHistoryHeader("HISTORY");
        prompts.setNoneItem("- none");
        prompts.setHistoryItem("- id={id}; category={category}; severity={severity}; summary={summary}; services={services}; providers={providers}; steps={steps}");
        prompts.setSignalsSection("SIGNALS\nservices={services}\nproviders={providers}\nhttpStatuses={httpStatuses}\nindicators={indicators}");
        prompts.setIncidentSection("ORIGINAL\n{originalDescription}\nNORMALIZED\n{normalizedDescription}");

        knowledgeBaseService = new KnowledgeBaseService(properties);
        historyStore = new IncidentRepository(properties);
    }

    @Test
    void recoversAfterFirstInvalidResponse() {
        AtomicInteger calls = new AtomicInteger();
        LlmClient llm = (system, user) -> {
            int n = calls.incrementAndGet();
            if (n == 1) {
                return new LlmCompletion("not-json", null, null, null);
            }
            return new LlmCompletion("""
                    {"category":"Recovered","summary":"Recovered summary for affected customers","severity":"MEDIUM","hypotheses":[{"title":"H1","reasoning":"R1","nextSteps":["A","B"]}]}
                    """, null, null, null);
        };
        IncidentAnalysisOrchestrator orchestrator = orchestrator(llm);
        var analysis = orchestrator.analyze("Customers cannot pay by card due to payment-service PayGate timeouts.");
        assertThat(analysis.category()).isEqualTo("Recovered");
        assertThat(analysis.metadata().llmAttempts()).isEqualTo(2);
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void failsAfterBothResponsesInvalidAndDoesNotRetryFurther() {
        AtomicInteger calls = new AtomicInteger();
        LlmClient llm = (system, user) -> {
            calls.incrementAndGet();
            return new LlmCompletion("{\"category\":\"\",\"summary\":\"\",\"severity\":\"nope\",\"hypotheses\":[]}", null, null, null);
        };
        IncidentAnalysisOrchestrator orchestrator = orchestrator(llm);
        assertThatThrownBy(() -> orchestrator.analyze("Customers cannot pay by card due to payment-service PayGate timeouts."))
                .isInstanceOf(InvalidLlmOutputException.class);
        assertThat(calls.get()).isEqualTo(2);
    }

    private IncidentAnalysisOrchestrator orchestrator(LlmClient llm) {
        JsonMapper mapper = JsonMapper.builder().build();
        return new IncidentAnalysisOrchestrator(
                new InputNormalizerService(),
                new SignalExtractorService(),
                knowledgeBaseService,
                new SimilarIncidentService(properties),
                new IncidentPromptService(properties),
                llm,
                new LlmResponseParser(mapper),
                new AnalysisValidatorService(),
                historyStore
        );
    }
}
