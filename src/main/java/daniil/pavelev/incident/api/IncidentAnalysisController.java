package daniil.pavelev.incident.api;

import daniil.pavelev.incident.api.dto.CreateIncidentAnalysisRequest;
import daniil.pavelev.incident.api.dto.IncidentAnalysisDetailResponse;
import daniil.pavelev.incident.api.dto.IncidentAnalysisListItemResponse;
import daniil.pavelev.incident.api.dto.PageResponse;
import daniil.pavelev.incident.domain.AnalysisStatus;
import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.domain.IncidentSeverity;
import daniil.pavelev.incident.service.IncidentAnalysisService;
import daniil.pavelev.incident.service.IncidentHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incident-analyses")
@RequiredArgsConstructor
public class IncidentAnalysisController {

    private final IncidentAnalysisService analysisService;
    private final IncidentHistoryService historyService;
    private final IncidentAnalysisMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentAnalysisDetailResponse create(@Valid @RequestBody CreateIncidentAnalysisRequest request) {
        IncidentAnalysisService.AnalysisOutcome outcome = analysisService.analyze(request.description());
        return mapper.toDetail(
                outcome.entity(),
                outcome.memory().selectedAnalysisIds(),
                outcome.memory().selectedAggregateIds()
        );
    }

    @GetMapping
    public PageResponse<IncidentAnalysisListItemResponse> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) IncidentCategory category,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) AnalysisStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo
    ) {
        return historyService.findHistory(
                page, size, category, severity, service, provider, status, createdFrom, createdTo
        );
    }

    @GetMapping("/{analysisId}")
    public IncidentAnalysisDetailResponse get(@PathVariable UUID analysisId) {
        return historyService.getById(analysisId);
    }
}
