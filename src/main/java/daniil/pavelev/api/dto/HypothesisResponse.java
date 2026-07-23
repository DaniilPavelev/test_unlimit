package daniil.pavelev.api.dto;

import java.util.List;

public record HypothesisResponse(
        String title,
        String reasoning,
        List<String> nextSteps
) {
}
