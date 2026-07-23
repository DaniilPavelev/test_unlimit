package daniil.pavelev.service;

import daniil.pavelev.domain.Hypothesis;
import daniil.pavelev.domain.LlmAnalysisPayload;
import daniil.pavelev.domain.Severity;
import daniil.pavelev.exception.InvalidLlmOutputException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class AnalysisValidatorService {

    private static final Pattern CYRILLIC = Pattern.compile("[\\u0400-\\u04FF]");

    public void validate(LlmAnalysisPayload payload) {
        List<String> errors = new ArrayList<>();
        if (payload == null) {
            throw new InvalidLlmOutputException("LLM payload is null", List.of("payload is null"));
        }
        if (isBlank(payload.category())) {
            errors.add("category is blank");
        }
        if (isBlank(payload.summary())) {
            errors.add("summary is blank");
        }
        try {
            Severity.valueOf(payload.severity() == null ? "" : payload.severity().trim());
        } catch (RuntimeException ex) {
            errors.add("severity is unsupported");
        }
        if (payload.hypotheses() == null || payload.hypotheses().isEmpty() || payload.hypotheses().size() > 3) {
            errors.add("hypotheses count must be between 1 and 3");
        } else {
            for (int i = 0; i < payload.hypotheses().size(); i++) {
                LlmAnalysisPayload.HypothesisDraft hypothesis = payload.hypotheses().get(i);
                if (hypothesis == null || isBlank(hypothesis.title())) {
                    errors.add("hypothesis[" + i + "].title is blank");
                }
                if (hypothesis == null || isBlank(hypothesis.reasoning())) {
                    errors.add("hypothesis[" + i + "].reasoning is blank");
                }
                if (hypothesis == null || hypothesis.nextSteps() == null
                        || hypothesis.nextSteps().size() < 2 || hypothesis.nextSteps().size() > 3) {
                    errors.add("hypothesis[" + i + "] must have 2 or 3 next steps");
                } else {
                    for (int j = 0; j < hypothesis.nextSteps().size(); j++) {
                        if (isBlank(hypothesis.nextSteps().get(j))) {
                            errors.add("hypothesis[" + i + "].nextSteps[" + j + "] is blank");
                        }
                    }
                }
            }
        }
        if (!looksLikeEnglish(payload)) {
            errors.add("response does not appear to be English");
        }
        if (!errors.isEmpty()) {
            throw new InvalidLlmOutputException("LLM output failed validation", errors);
        }
    }

    public List<Hypothesis> toHypotheses(LlmAnalysisPayload payload) {
        return payload.hypotheses().stream()
                .map(h -> new Hypothesis(
                        h.title().trim(),
                        h.reasoning().trim(),
                        h.nextSteps().stream().map(String::trim).toList()
                ))
                .toList();
    }

    private boolean looksLikeEnglish(LlmAnalysisPayload payload) {
        StringBuilder text = new StringBuilder();
        text.append(nullToEmpty(payload.category())).append(' ')
                .append(nullToEmpty(payload.summary()));
        if (payload.hypotheses() != null) {
            payload.hypotheses().forEach(h -> {
                if (h != null) {
                    text.append(' ').append(nullToEmpty(h.title()))
                            .append(' ').append(nullToEmpty(h.reasoning()));
                }
            });
        }
        String value = text.toString();
        if (CYRILLIC.matcher(value).find()) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).matches("(?s).*[a-z].*");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
