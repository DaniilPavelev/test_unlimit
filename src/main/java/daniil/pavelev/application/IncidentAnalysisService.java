package daniil.pavelev.application;

import daniil.pavelev.api.dto.IncidentAnalysisListResponse;
import daniil.pavelev.api.dto.IncidentAnalysisResponse;
import daniil.pavelev.exception.AnalysisNotFoundException;
import daniil.pavelev.mapper.IncidentAnalysisMapper;
import daniil.pavelev.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentAnalysisService {

    private final IncidentAnalysisOrchestrator orchestrator;
    private final IncidentRepository historyStore;
    private final IncidentAnalysisMapper mapper;

    public IncidentAnalysisResponse create(String description) {
        return mapper.toResponse(orchestrator.analyze(description));
    }

    public IncidentAnalysisResponse get(UUID id) {
        return historyStore.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new AnalysisNotFoundException("Analysis not found"));
    }

    public IncidentAnalysisListResponse list() {
        return mapper.toList(historyStore.findAll());
    }
}
