package daniil.pavelev.pipeline;

import daniil.pavelev.domain.LlmAnalysisPayload;
import daniil.pavelev.service.LlmResponseParser;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmResponseParserTest {

    private final LlmResponseParser parser = new LlmResponseParser(JsonMapper.builder().build());

    @Test
    void parsesValidJson() {
        LlmAnalysisPayload payload = parser.parse("""
                {"category":"External payment provider issue","summary":"PayGate timeouts","severity":"HIGH","hypotheses":[{"title":"A","reasoning":"B","nextSteps":["C","D"]}]}
                """);
        assertThat(payload.category()).isEqualTo("External payment provider issue");
        assertThat(payload.severity()).isEqualTo("HIGH");
    }

    @Test
    void parsesJsonInsideMarkdownFences() {
        String json = parser.extractJson("""
                ```json
                {"category":"X","summary":"Y","severity":"LOW","hypotheses":[{"title":"A","reasoning":"B","nextSteps":["C","D"]}]}
                ```
                """);
        assertThat(json).startsWith("{").endsWith("}");
    }

    @Test
    void parsesJsonSurroundedByProse() {
        String json = parser.extractJson("""
                Here is the analysis:
                {"category":"X","summary":"value with {braces} inside","severity":"MEDIUM","hypotheses":[{"title":"A","reasoning":"B","nextSteps":["C","D"]}]}
                Thanks.
                """);
        assertThat(json).contains("value with {braces} inside");
        assertThat(parser.parse(json).summary()).isEqualTo("value with {braces} inside");
    }

    @Test
    void rejectsEmptyResponse() {
        assertThatThrownBy(() -> parser.extractJson("   "))
                .isInstanceOf(daniil.pavelev.exception.LlmResponseParseException.class)
                .hasMessageContaining("empty");
    }
}
