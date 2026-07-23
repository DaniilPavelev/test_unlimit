package daniil.pavelev.support;

import daniil.pavelev.llm.LlmClient;
import daniil.pavelev.llm.LlmCompletion;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Locale;

/**
 * Test-only deterministic LLM double. Keeps tests offline (no OpenAI network calls).
 */
@TestConfiguration
public class StubLlmClientConfig {

    @Bean
    @Primary
    LlmClient stubLlmClient() {
        return (systemPrompt, userPrompt) -> {
            if (userPrompt.toLowerCase(Locale.ROOT).contains("validation errors")
                    || systemPrompt.toLowerCase(Locale.ROOT).contains("corrected json")) {
                return completion("""
                        {
                          "category":"Recovered analysis",
                          "summary":"Recovered summary for affected customers.",
                          "severity":"MEDIUM",
                          "hypotheses":[{"title":"Recovered hypothesis","reasoning":"Repair succeeded.","nextSteps":["Check service health.","Compare recent errors."]}]
                        }
                        """);
            }
            String lower = userPrompt.toLowerCase(Locale.ROOT);
            if (lower.contains("paygate") && lower.contains("timeout")) {
                return completion("""
                        {
                          "category":"External payment provider issue",
                          "summary":"PayGate timeouts are causing card payment failures. Likely affected: card-paying customers.",
                          "severity":"HIGH",
                          "hypotheses":[{"title":"PayGate degradation","reasoning":"Timeouts are isolated to payment-service calls to PayGate.","nextSteps":["Check PayGate status.","Compare PayGate latency with other providers.","Inspect payment-service timeout configuration."]}]
                        }
                        """);
            }
            if (lower.contains("401") || (lower.contains("token") && lower.contains("auth-service"))) {
                return completion("""
                        {
                          "category":"User authentication errors",
                          "summary":"Invalid token signatures block mobile login. Likely affected: mobile app users.",
                          "severity":"MEDIUM",
                          "hypotheses":[{"title":"Token signing key mismatch","reasoning":"auth-service returns HTTP 401 with signature errors.","nextSteps":["Verify signing keys across auth instances.","Reproduce login with a fresh token.","Check recent auth-service deployments."]}]
                        }
                        """);
            }
            return completion("""
                    {
                      "category":"Unclassified operational issue",
                      "summary":"Generic incident analysis based on limited evidence. Likely affected: users of the mentioned functionality.",
                      "severity":"MEDIUM",
                      "hypotheses":[{"title":"Insufficient evidence","reasoning":"Signals do not uniquely identify a root cause.","nextSteps":["Confirm impacted flows.","Gather error rates for mentioned services."]}]
                    }
                    """);
        };
    }

    private static LlmCompletion completion(String content) {
        return new LlmCompletion(content, 10, 20, 30);
    }
}
