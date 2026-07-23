package daniil.pavelev.llm;

import daniil.pavelev.exception.LlmTimeoutException;
import daniil.pavelev.exception.LlmUpstreamException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class SpringAiLlmClient implements LlmClient {

    private final ChatClient chatClient;

    @Override
    public LlmCompletion complete(String systemPrompt, String userPrompt) {
        try {
            ChatResponse response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new LlmUpstreamException("LLM provider returned an empty response");
            }

            String content = response.getResult().getOutput().getText();
            if (content == null || content.isBlank()) {
                throw new LlmUpstreamException("LLM provider returned an empty response");
            }

            Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
            Integer promptTokens = usage == null || usage.getPromptTokens() == null
                    ? null
                    : usage.getPromptTokens().intValue();
            Integer completionTokens = usage == null || usage.getCompletionTokens() == null
                    ? null
                    : usage.getCompletionTokens().intValue();
            Integer totalTokens = usage == null || usage.getTotalTokens() == null
                    ? null
                    : usage.getTotalTokens().intValue();

            return new LlmCompletion(content, promptTokens, completionTokens, totalTokens);
        } catch (LlmUpstreamException | LlmTimeoutException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new LlmTimeoutException("LLM request timed out", ex);
        } catch (RestClientResponseException ex) {
            throw new LlmUpstreamException("LLM provider returned HTTP " + ex.getStatusCode().value(), ex);
        } catch (RuntimeException ex) {
            Throwable root = rootCause(ex);
            if (root instanceof ResourceAccessException resourceAccessException) {
                throw new LlmTimeoutException("LLM request timed out", resourceAccessException);
            }
            if (root instanceof RestClientResponseException responseException) {
                throw new LlmUpstreamException(
                        "LLM provider returned HTTP " + responseException.getStatusCode().value(),
                        responseException
                );
            }
            throw new LlmUpstreamException("LLM provider call failed", ex);
        }
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
