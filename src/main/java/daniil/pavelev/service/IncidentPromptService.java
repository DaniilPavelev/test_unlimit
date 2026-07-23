package daniil.pavelev.service;

import daniil.pavelev.config.IncidentProperties;
import daniil.pavelev.domain.IncidentAnalysis;
import daniil.pavelev.domain.IncidentSignals;
import daniil.pavelev.service.SimilarIncidentService.ScoredHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IncidentPromptService {

    private final IncidentProperties properties;

    public PromptBundle buildAnalysisPrompt(
            String originalDescription,
            String normalizedDescription,
            IncidentSignals signals,
            List<String> knowledge,
            List<ScoredHistory> history
    ) {
        IncidentProperties.Prompts prompts = properties.getPrompts();
        List<CompactHistory> budgeted = enforceBudget(compact(history), knowledge);

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append(prompts.getKnowledgeHeader()).append('\n');
        knowledge.forEach(item -> userPrompt.append("- ").append(item).append('\n'));

        userPrompt.append('\n').append(prompts.getHistoryHeader()).append('\n');
        if (budgeted.isEmpty()) {
            userPrompt.append(prompts.getNoneItem()).append('\n');
        } else {
            for (CompactHistory item : budgeted) {
                userPrompt.append(fill(prompts.getHistoryItem(), Map.of(
                        "id", item.id(),
                        "category", item.category(),
                        "severity", item.severity(),
                        "summary", item.summary(),
                        "services", String.valueOf(item.services()),
                        "providers", String.valueOf(item.providers()),
                        "steps", String.valueOf(item.steps())
                ))).append('\n');
            }
        }

        userPrompt.append('\n').append(fill(prompts.getSignalsSection(), Map.of(
                "services", String.valueOf(signals.mentionedServices()),
                "providers", String.valueOf(signals.mentionedProviders()),
                "httpStatuses", String.valueOf(signals.httpStatusCodes()),
                "indicators", String.valueOf(signals.indicators())
        )));

        userPrompt.append('\n').append(fill(prompts.getIncidentSection(), Map.of(
                "originalDescription", originalDescription,
                "normalizedDescription", normalizedDescription
        )));

        return new PromptBundle(
                prompts.getAnalysisSystem(),
                userPrompt.toString(),
                budgeted.stream().map(CompactHistory::id).toList()
        );
    }

    public PromptBundle buildRepairPrompt(String invalidOutput, List<String> errors) {
        var prompts = properties.getPrompts();
        String user = fill(prompts.getRepairUser(), Map.of(
                "errors", String.join("; ", errors),
                "invalidOutput", invalidOutput
        ));
        return new PromptBundle(prompts.getRepairSystem(), user, List.of());
    }

    private List<CompactHistory> compact(List<ScoredHistory> history) {
        List<CompactHistory> result = new ArrayList<>();
        for (ScoredHistory item : history) {
            IncidentAnalysis analysis = item.analysis();
            List<String> steps = analysis.hypotheses().stream()
                    .flatMap(h -> h.nextSteps().stream())
                    .limit(2)
                    .toList();
            result.add(new CompactHistory(
                    analysis.id().toString(),
                    analysis.category(),
                    truncate(analysis.summary(), 220),
                    analysis.severity().name(),
                    analysis.metadata().mentionedServices(),
                    analysis.metadata().mentionedProviders(),
                    steps,
                    item.score()
            ));
        }
        return result;
    }

    private List<CompactHistory> enforceBudget(List<CompactHistory> history, List<String> knowledge) {
        List<CompactHistory> working = new ArrayList<>(history);
        int max = properties.getMemory().getMaxContextCharacters();
        while (estimate(working, knowledge) > max && !working.isEmpty()) {
            int lowestIndex = 0;
            double lowest = working.getFirst().score();
            for (int i = 1; i < working.size(); i++) {
                if (working.get(i).score() < lowest) {
                    lowest = working.get(i).score();
                    lowestIndex = i;
                }
            }
            working.remove(lowestIndex);
        }
        return List.copyOf(working);
    }

    private int estimate(List<CompactHistory> history, List<String> knowledge) {
        int size = knowledge.stream().mapToInt(String::length).sum();
        for (CompactHistory item : history) {
            size += item.summary().length()
                    + item.category().length()
                    + item.services().toString().length()
                    + item.providers().toString().length()
                    + item.steps().toString().length();
        }
        return size;
    }

    private static String fill(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max).trim() + "…";
    }

    public record PromptBundle(String systemPrompt, String userPrompt, List<String> selectedHistoricalIds) {
    }

    public record CompactHistory(
            String id,
            String category,
            String summary,
            String severity,
            List<String> services,
            List<String> providers,
            List<String> steps,
            double score
    ) {
    }
}
