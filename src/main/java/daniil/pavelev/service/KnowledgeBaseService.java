package daniil.pavelev.service;

import daniil.pavelev.config.IncidentProperties;
import daniil.pavelev.domain.IncidentSignals;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final IncidentProperties properties;

    public List<String> selectRelevantKnowledge(IncidentSignals signals) {
        List<String> knowledge = properties.getKnowledge();
        List<String> selected = new ArrayList<>();
        for (String snippet : knowledge) {
            String lower = snippet.toLowerCase(Locale.ROOT);
            boolean relevant = signals.mentionedServices().stream().anyMatch(s -> lower.contains(s.toLowerCase(Locale.ROOT)))
                    || signals.mentionedProviders().stream().anyMatch(p -> lower.contains(p.toLowerCase(Locale.ROOT)))
                    || signals.indicators().stream().anyMatch(i -> lower.contains(i.toLowerCase(Locale.ROOT)));
            if (relevant) {
                selected.add(snippet);
            }
        }
        return List.copyOf(selected);
    }
}
