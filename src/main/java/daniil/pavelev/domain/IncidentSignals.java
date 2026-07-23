package daniil.pavelev.domain;

import java.util.Set;

public record IncidentSignals(
        Set<String> mentionedServices,
        Set<String> mentionedProviders,
        Set<Integer> httpStatusCodes,
        Set<String> indicators
) {
    public IncidentSignals {
        mentionedServices = Set.copyOf(mentionedServices);
        mentionedProviders = Set.copyOf(mentionedProviders);
        httpStatusCodes = Set.copyOf(httpStatusCodes);
        indicators = Set.copyOf(indicators);
    }
}
