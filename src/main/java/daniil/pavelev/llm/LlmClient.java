package daniil.pavelev.llm;

public interface LlmClient {

    LlmAnalysisResponse analyze(LlmAnalysisRequest request);

    LlmCompactionResponse compactTextualPatterns(LlmCompactionRequest request);
}
