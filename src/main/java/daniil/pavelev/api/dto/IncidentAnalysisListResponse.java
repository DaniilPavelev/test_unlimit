package daniil.pavelev.api.dto;

import java.util.List;

public record IncidentAnalysisListResponse(
        List<IncidentAnalysisListItemResponse> items
) {
}
