package daniil.pavelev.llm;

public record LlmCompactionResponse(
        String recurringPatterns,
        String effectiveDiagnosticPatterns,
        String model
) {
}
