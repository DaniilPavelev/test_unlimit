package daniil.pavelev.llm;

public interface LlmClient {

    LlmCompletion complete(String systemPrompt, String userPrompt);
}
