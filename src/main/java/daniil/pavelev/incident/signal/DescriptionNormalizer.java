package daniil.pavelev.incident.signal;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DescriptionNormalizer {

    private static final Pattern CREDENTIAL_PATTERN = Pattern.compile(
            "(?i)(api[_-]?key|authorization|bearer|password|secret|token)\\s*[:=]\\s*[^\\s,;]+"
    );
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public String normalize(String originalDescription) {
        if (originalDescription == null) {
            return "";
        }
        String sanitized = CREDENTIAL_PATTERN.matcher(originalDescription).replaceAll("$1=[REDACTED]");
        sanitized = WHITESPACE.matcher(sanitized.trim()).replaceAll(" ");
        return sanitized;
    }

    public String lowercase(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public Set<String> tokenizeKeywords(String normalized) {
        Set<String> keywords = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("[a-z0-9][a-z0-9._-]{2,}").matcher(lowercase(normalized));
        while (matcher.find()) {
            String token = matcher.group();
            if (!STOP_WORDS.contains(token)) {
                keywords.add(token);
            }
        }
        return keywords;
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "from", "that", "this", "have", "has",
            "was", "were", "are", "been", "being", "into", "onto", "about",
            "error", "failed", "failure", "issue", "incident", "service", "request"
    );
}
