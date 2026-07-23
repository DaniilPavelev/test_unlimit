package daniil.pavelev.service;

import daniil.pavelev.domain.LlmAnalysisPayload;
import daniil.pavelev.exception.LlmResponseParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class LlmResponseParser {

    private final ObjectMapper objectMapper;

    public String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new LlmResponseParseException("empty LLM response");
        }
        String text = stripFences(raw.trim());
        int start = findJsonStart(text);
        if (start < 0) {
            throw new LlmResponseParseException("no JSON object found");
        }
        String json = scanObject(text, start);
        if (json == null) {
            throw new LlmResponseParseException("malformed JSON object");
        }
        return json;
    }

    public LlmAnalysisPayload parse(String raw) {
        try {
            String json = extractJson(raw);
            return objectMapper.readValue(json, LlmAnalysisPayload.class);
        } catch (LlmResponseParseException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new LlmResponseParseException("failed to deserialize LLM JSON", ex);
        }
    }

    private static String stripFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private static int findJsonStart(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '{') {
                return i;
            }
        }
        return -1;
    }

    private static String scanObject(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (c == '\\') {
                    escaping = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
    }
}
