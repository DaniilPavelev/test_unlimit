package daniil.pavelev.pipeline;

import daniil.pavelev.domain.LlmAnalysisPayload;
import daniil.pavelev.exception.InvalidLlmOutputException;
import daniil.pavelev.service.AnalysisValidatorService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisValidatorServiceTest {

    private final AnalysisValidatorService validator = new AnalysisValidatorService();

    @Test
    void rejectsUnsupportedSeverity() {
        assertThatThrownBy(() -> validator.validate(payload("critical", 1, 2)))
                .isInstanceOf(InvalidLlmOutputException.class)
                .satisfies(ex -> {
                    InvalidLlmOutputException invalid = (InvalidLlmOutputException) ex;
                    org.assertj.core.api.Assertions.assertThat(invalid.getValidationErrors())
                            .anyMatch(e -> e.contains("severity"));
                });
    }

    @Test
    void rejectsMoreThanThreeHypotheses() {
        assertThatThrownBy(() -> validator.validate(payload("HIGH", 4, 2)))
                .isInstanceOf(InvalidLlmOutputException.class)
                .satisfies(ex -> {
                    InvalidLlmOutputException invalid = (InvalidLlmOutputException) ex;
                    org.assertj.core.api.Assertions.assertThat(invalid.getValidationErrors())
                            .anyMatch(e -> e.contains("hypotheses count"));
                });
    }

    private LlmAnalysisPayload payload(String severity, int hypothesisCount, int steps) {
        List<LlmAnalysisPayload.HypothesisDraft> hypotheses = java.util.stream.IntStream.range(0, hypothesisCount)
                .mapToObj(i -> new LlmAnalysisPayload.HypothesisDraft(
                        "title-" + i,
                        "reasoning-" + i,
                        java.util.stream.IntStream.range(0, steps).mapToObj(j -> "step-" + j).toList()
                ))
                .toList();
        return new LlmAnalysisPayload("category", "summary", severity, hypotheses);
    }
}
