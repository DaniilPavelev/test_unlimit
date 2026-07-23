package daniil.pavelev.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIncidentAnalysisRequest(
        @NotBlank
        @Size(min = 10, max = 10_000)
        String description
) {
}
