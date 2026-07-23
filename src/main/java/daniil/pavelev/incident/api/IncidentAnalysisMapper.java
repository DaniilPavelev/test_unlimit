package daniil.pavelev.incident.api;

import daniil.pavelev.incident.api.dto.IncidentAnalysisDetailResponse;
import daniil.pavelev.incident.api.dto.IncidentAnalysisListItemResponse;
import daniil.pavelev.incident.domain.IncidentAnalysisEntity;
import daniil.pavelev.incident.domain.OrderedTextItem;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class IncidentAnalysisMapper {

    public IncidentAnalysisListItemResponse toListItem(IncidentAnalysisEntity entity) {
        return new IncidentAnalysisListItemResponse(
                entity.getId(),
                entity.getCategory(),
                entity.getSummary(),
                entity.getSeverity(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getMentionedServices(),
                entity.getProviderNames(),
                entity.getErrorCode()
        );
    }

    public IncidentAnalysisDetailResponse toDetail(
            IncidentAnalysisEntity entity,
            List<UUID> memoryAnalysisIds,
            List<UUID> memoryAggregateIds
    ) {
        return new IncidentAnalysisDetailResponse(
                entity.getId(),
                entity.getOriginalDescription(),
                entity.getNormalizedDescription(),
                entity.getCategory(),
                entity.getSummary(),
                entity.getSeverity(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getModel(),
                entity.getLlmAttempts(),
                entity.getPromptTokens(),
                entity.getCompletionTokens(),
                entity.getTotalTokens(),
                entity.getProcessingDurationMs(),
                entity.getErrorCode(),
                orderedTexts(entity.getHypotheses()),
                orderedTexts(entity.getNextSteps()),
                entity.getMentionedServices(),
                entity.getProviderNames(),
                entity.getHttpStatusCodes(),
                entity.getKeywords(),
                entity.getAffectedFunctionality(),
                memoryAnalysisIds,
                memoryAggregateIds
        );
    }

    private List<String> orderedTexts(List<OrderedTextItem> items) {
        return items.stream()
                .sorted(Comparator.comparingInt(OrderedTextItem::getPosition))
                .map(OrderedTextItem::getText)
                .toList();
    }
}
