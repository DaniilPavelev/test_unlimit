package daniil.pavelev.incident.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIncidentAnalysisRequest(
        @NotBlank
        @Size(max = 20_000)
        String description
) {
}
