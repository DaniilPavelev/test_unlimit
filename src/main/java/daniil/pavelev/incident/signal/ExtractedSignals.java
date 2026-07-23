package daniil.pavelev.incident.signal;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class ExtractedSignals {

    private final Set<String> mentionedServices;
    private final Set<String> providerNames;
    private final Set<Integer> httpStatusCodes;
    private final Set<String> keywords;
    private final Set<String> affectedFunctionality;

    public ExtractedSignals(
            Set<String> mentionedServices,
            Set<String> providerNames,
            Set<Integer> httpStatusCodes,
            Set<String> keywords,
            Set<String> affectedFunctionality
    ) {
        this.mentionedServices = Set.copyOf(mentionedServices);
        this.providerNames = Set.copyOf(providerNames);
        this.httpStatusCodes = Set.copyOf(httpStatusCodes);
        this.keywords = Set.copyOf(keywords);
        this.affectedFunctionality = Set.copyOf(affectedFunctionality);
    }

    public static ExtractedSignals empty() {
        return new ExtractedSignals(
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
    }

    public Set<String> mentionedServices() {
        return mentionedServices;
    }

    public Set<String> providerNames() {
        return providerNames;
    }

    public Set<Integer> httpStatusCodes() {
        return httpStatusCodes;
    }

    public Set<String> keywords() {
        return keywords;
    }

    public Set<String> affectedFunctionality() {
        return affectedFunctionality;
    }

    public Set<String> mentionedServicesLower() {
        return toLower(mentionedServices);
    }

    public Set<String> providerNamesLower() {
        return toLower(providerNames);
    }

    public Set<String> keywordsLower() {
        return toLower(keywords);
    }

    private static Set<String> toLower(Set<String> values) {
        Set<String> lowered = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                lowered.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return lowered;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExtractedSignals that)) {
            return false;
        }
        return Objects.equals(mentionedServices, that.mentionedServices)
                && Objects.equals(providerNames, that.providerNames)
                && Objects.equals(httpStatusCodes, that.httpStatusCodes)
                && Objects.equals(keywords, that.keywords)
                && Objects.equals(affectedFunctionality, that.affectedFunctionality);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mentionedServices, providerNames, httpStatusCodes, keywords, affectedFunctionality);
    }
}
