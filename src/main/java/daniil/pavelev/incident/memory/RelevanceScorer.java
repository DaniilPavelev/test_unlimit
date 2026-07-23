package daniil.pavelev.incident.memory;

import daniil.pavelev.config.MemoryRetrievalProperties;
import daniil.pavelev.incident.domain.IncidentAnalysisEntity;
import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.domain.OrderedTextItem;
import daniil.pavelev.incident.signal.ExtractedSignals;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RelevanceScorer {

    public static final double SERVICE_WEIGHT = 5.0;
    public static final double PROVIDER_WEIGHT = 4.0;
    public static final double CATEGORY_WEIGHT = 3.0;
    public static final double HTTP_STATUS_WEIGHT = 2.0;
    public static final double KEYWORD_WEIGHT = 1.0;

    private final MemoryRetrievalProperties properties;

    public RelevanceScorer(MemoryRetrievalProperties properties) {
        this.properties = properties;
    }

    public ScoredCandidate score(
            IncidentAnalysisEntity candidate,
            ExtractedSignals query,
            IncidentCategory inferredCategory,
            Instant now
    ) {
        Set<String> matchingServices = intersection(
                lower(candidate.getMentionedServices()),
                query.mentionedServicesLower()
        );
        Set<String> matchingProviders = intersection(
                lower(candidate.getProviderNames()),
                query.providerNamesLower()
        );
        Set<Integer> matchingStatuses = intersection(candidate.getHttpStatusCodes(), query.httpStatusCodes());
        Set<String> matchingKeywords = intersection(lower(candidate.getKeywords()), query.keywordsLower());

        double score = 0.0;
        score += matchingServices.size() * SERVICE_WEIGHT;
        score += matchingProviders.size() * PROVIDER_WEIGHT;
        if (inferredCategory != null
                && candidate.getCategory() != null
                && inferredCategory == candidate.getCategory()) {
            score += CATEGORY_WEIGHT;
        }
        score += matchingStatuses.size() * HTTP_STATUS_WEIGHT;
        score += matchingKeywords.size() * KEYWORD_WEIGHT;

        if (candidate.getCreatedAt() != null
                && Duration.between(candidate.getCreatedAt(), now).toDays() <= properties.getRecentDays()) {
            score += properties.getRecentBonus();
        }

        List<String> diagnosticSteps = candidate.getNextSteps().stream()
                .sorted(Comparator.comparingInt(OrderedTextItem::getPosition))
                .map(OrderedTextItem::getText)
                .toList();

        return new ScoredCandidate(
                candidate,
                score,
                List.copyOf(matchingServices),
                List.copyOf(matchingProviders),
                diagnosticSteps
        );
    }

    private static Set<String> lower(Set<String> values) {
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static <T> Set<T> intersection(Set<T> left, Set<T> right) {
        Set<T> result = new LinkedHashSet<>(left);
        result.retainAll(right);
        return result;
    }

    public record ScoredCandidate(
            IncidentAnalysisEntity entity,
            double score,
            List<String> matchingServices,
            List<String> matchingProviders,
            List<String> diagnosticSteps
    ) {
    }
}
