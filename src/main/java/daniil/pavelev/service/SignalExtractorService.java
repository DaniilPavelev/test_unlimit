package daniil.pavelev.service;

import daniil.pavelev.domain.IncidentSignals;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class SignalExtractorService {

    private static final Set<String> SERVICES = Set.of(
            "api-gateway", "auth-service", "payment-service",
            "billing-service", "notification-service", "reporting-service"
    );

    private static final Set<String> PROVIDERS = Set.of("paygate", "smtp", "sms");

    private static final Set<Integer> HTTP_STATUSES = Set.of(401, 500, 503, 504);

    private static final Map<String, String> INDICATORS = Map.ofEntries(
            Map.entry("timeout", "timeout"),
            Map.entry("latency", "latency"),
            Map.entry("slow", "latency"),
            Map.entry("database", "database"),
            Map.entry("postgres", "database"),
            Map.entry("cpu", "cpu"),
            Map.entry("email", "email"),
            Map.entry("smtp", "email"),
            Map.entry("sms", "sms"),
            Map.entry("auth", "authentication"),
            Map.entry("login", "authentication"),
            Map.entry("token", "token"),
            Map.entry("payment", "payment"),
            Map.entry("pay by card", "payment"),
            Map.entry("balance", "balance"),
            Map.entry("invoice", "balance"),
            Map.entry("gateway", "gateway")
    );

    public IncidentSignals extract(String normalizedDescription) {
        String lower = normalizedDescription.toLowerCase(Locale.ROOT);

        Set<String> services = new LinkedHashSet<>();
        for (String service : SERVICES) {
            if (lower.contains(service)) {
                services.add(service);
            }
        }

        Set<String> providers = new LinkedHashSet<>();
        for (String provider : PROVIDERS) {
            if (lower.contains(provider)) {
                providers.add(provider);
            }
        }

        Set<Integer> statuses = new LinkedHashSet<>();
        for (Integer status : HTTP_STATUSES) {
            if (lower.contains(String.valueOf(status))) {
                statuses.add(status);
            }
        }

        Set<String> indicators = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : INDICATORS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                indicators.add(entry.getValue());
            }
        }

        return new IncidentSignals(services, providers, statuses, indicators);
    }
}
