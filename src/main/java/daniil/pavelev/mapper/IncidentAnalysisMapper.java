package daniil.pavelev.mapper;

import daniil.pavelev.api.dto.HypothesisResponse;
import daniil.pavelev.api.dto.IncidentAnalysisListItemResponse;
import daniil.pavelev.api.dto.IncidentAnalysisListResponse;
import daniil.pavelev.api.dto.IncidentAnalysisMetadataResponse;
import daniil.pavelev.api.dto.IncidentAnalysisResponse;
import daniil.pavelev.domain.IncidentAnalysis;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncidentAnalysisMapper {

    public IncidentAnalysisResponse toResponse(IncidentAnalysis analysis) {
        return new IncidentAnalysisResponse(
                analysis.id(),
                analysis.createdAt(),
                analysis.category(),
                analysis.summary(),
                analysis.severity().name(),
                analysis.hypotheses().stream()
                        .map(h -> new HypothesisResponse(h.title(), h.reasoning(), h.nextSteps()))
                        .toList(),
                new IncidentAnalysisMetadataResponse(
                        analysis.metadata().mentionedServices(),
                        analysis.metadata().mentionedProviders(),
                        analysis.metadata().selectedHistoricalAnalysisIds(),
                        analysis.metadata().llmAttempts()
                )
        );
    }

    public IncidentAnalysisListItemResponse toListItem(IncidentAnalysis analysis) {
        return new IncidentAnalysisListItemResponse(
                analysis.id(),
                analysis.createdAt(),
                analysis.category(),
                analysis.summary(),
                analysis.severity().name(),
                analysis.metadata().mentionedServices(),
                analysis.metadata().mentionedProviders()
        );
    }

    public IncidentAnalysisListResponse toList(List<IncidentAnalysis> analyses) {
        return new IncidentAnalysisListResponse(
                analyses.stream().map(this::toListItem).toList()
        );
    }
}
