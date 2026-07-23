package daniil.pavelev.llm;

import java.util.List;

public record LlmCompactionRequest(
        List<String> summaries,
        List<String> hypotheses,
        List<String> diagnosticSteps
) {
}
