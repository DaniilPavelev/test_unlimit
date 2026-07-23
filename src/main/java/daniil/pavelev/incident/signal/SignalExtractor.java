package daniil.pavelev.incident.signal;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SignalExtractor {

    private static final Pattern HTTP_STATUS = Pattern.compile("\\b([1-5][0-9]{2})\\b");
    private static final Pattern SERVICE_PATTERN = Pattern.compile(
            "(?i)\\b([a-z][a-z0-9-]{1,40}-(?:service|api|gateway|worker|consumer|producer))\\b"
    );
    private static final Pattern FUNCTIONALITY_PATTERN = Pattern.compile(
            "(?i)\\b(?:cannot|can't|unable to|fail(?:ed|ing)? to)\\s+([a-z0-9 _-]{3,60})"
    );

    private static final Map<String, String> KNOWN_PROVIDERS = Map.ofEntries(
            Map.entry("aws", "AWS"),
            Map.entry("amazon", "AWS"),
            Map.entry("azure", "Azure"),
            Map.entry("gcp", "GCP"),
            Map.entry("google cloud", "GCP"),
            Map.entry("stripe", "Stripe"),
            Map.entry("twilio", "Twilio"),
            Map.entry("sendgrid", "SendGrid"),
            Map.entry("datadog", "Datadog"),
            Map.entry("kafka", "Kafka"),
            Map.entry("redis", "Redis"),
            Map.entry("postgres", "PostgreSQL"),
            Map.entry("postgresql", "PostgreSQL"),
            Map.entry("mysql", "MySQL"),
            Map.entry("mongodb", "MongoDB")
    );

    private final DescriptionNormalizer normalizer;

    public SignalExtractor(DescriptionNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public ExtractedSignals extract(String normalizedDescription) {
        String text = normalizedDescription == null ? "" : normalizedDescription;
        String lower = text.toLowerCase(Locale.ROOT);

        Set<String> services = new LinkedHashSet<>();
        Matcher serviceMatcher = SERVICE_PATTERN.matcher(text);
        while (serviceMatcher.find()) {
            services.add(serviceMatcher.group(1).toLowerCase(Locale.ROOT));
        }

        Set<String> providers = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : KNOWN_PROVIDERS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                providers.add(entry.getValue());
            }
        }

        Set<Integer> statuses = new LinkedHashSet<>();
        Matcher statusMatcher = HTTP_STATUS.matcher(text);
        while (statusMatcher.find()) {
            statuses.add(Integer.parseInt(statusMatcher.group(1)));
        }

        Set<String> keywords = normalizer.tokenizeKeywords(text);

        Set<String> functionality = new LinkedHashSet<>();
        Matcher functionalityMatcher = FUNCTIONALITY_PATTERN.matcher(text);
        while (functionalityMatcher.find()) {
            String value = functionalityMatcher.group(1).trim().toLowerCase(Locale.ROOT);
            if (!value.isBlank()) {
                functionality.add(value.length() > 120 ? value.substring(0, 120) : value);
            }
        }

        return new ExtractedSignals(services, providers, statuses, keywords, functionality);
    }
}
