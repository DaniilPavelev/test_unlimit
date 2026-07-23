package daniil.pavelev.domain;

import java.util.List;

public record Hypothesis(
        String title,
        String reasoning,
        List<String> nextSteps
) {
}
