package daniil.pavelev.service;

import daniil.pavelev.config.IncidentProperties;
import daniil.pavelev.domain.IncidentAnalysis;
import daniil.pavelev.domain.IncidentSignals;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class SimilarIncidentService {

    public static final double SERVICE_WEIGHT = 5;
    public static final double PROVIDER_WEIGHT = 4;
    public static final double STATUS_WEIGHT = 2;
    public static final double INDICATOR_WEIGHT = 1;

    private final IncidentProperties properties;

    public List<ScoredHistory> findHistory(IncidentSignals signals, List<IncidentAnalysis> history) {
        List<ScoredHistory> scored = new ArrayList<>();
        for (IncidentAnalysis analysis : history) {
            double score = scoreAnalysis(signals, analysis);
            if (score > 0) {
                scored.add(new ScoredHistory(analysis, score));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredHistory::score).reversed());
        return scored.stream().limit(properties.getMemory().getMaxSelectedItems()).toList();
    }

    private double scoreAnalysis(IncidentSignals signals, IncidentAnalysis analysis) {
        double score = 0;
        for (String service : analysis.metadata().mentionedServices()) {
            if (signals.mentionedServices().stream().anyMatch(s -> s.equalsIgnoreCase(service))) {
                score += SERVICE_WEIGHT;
            }
        }
        for (String provider : analysis.metadata().mentionedProviders()) {
            if (signals.mentionedProviders().stream().anyMatch(p -> p.equalsIgnoreCase(provider))) {
                score += PROVIDER_WEIGHT;
            }
        }
        score += scoreText(signals, analysis.summary() + " " + analysis.category());
        return score;
    }

    private double scoreText(IncidentSignals signals, String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        double score = 0;
        for (String service : signals.mentionedServices()) {
            if (lower.contains(service.toLowerCase(Locale.ROOT))) {
                score += SERVICE_WEIGHT;
            }
        }
        for (String provider : signals.mentionedProviders()) {
            if (lower.contains(provider.toLowerCase(Locale.ROOT))) {
                score += PROVIDER_WEIGHT;
            }
        }
        for (Integer status : signals.httpStatusCodes()) {
            if (lower.contains(String.valueOf(status))) {
                score += STATUS_WEIGHT;
            }
        }
        for (String indicator : signals.indicators()) {
            if (lower.contains(indicator.toLowerCase(Locale.ROOT))) {
                score += INDICATOR_WEIGHT;
            }
        }
        return score;
    }

    public record ScoredHistory(IncidentAnalysis analysis, double score) {
    }
}
