package daniil.pavelev.llm;

public record LlmCompletion(String content, Integer promptTokens, Integer completionTokens, Integer totalTokens) {
}
