package daniil.pavelev.incident.service;

import daniil.pavelev.config.IncidentHistoryProperties;
import daniil.pavelev.incident.api.IncidentAnalysisMapper;
import daniil.pavelev.incident.api.dto.IncidentAnalysisDetailResponse;
import daniil.pavelev.incident.api.dto.IncidentAnalysisListItemResponse;
import daniil.pavelev.incident.api.dto.PageResponse;
import daniil.pavelev.incident.domain.AnalysisStatus;
import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.domain.IncidentSeverity;
import daniil.pavelev.incident.persistence.IncidentAnalysisRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class IncidentHistoryService {

    private final IncidentAnalysisRepository repository;
    private final IncidentHistoryProperties historyProperties;
    private final IncidentAnalysisMapper mapper;

    public IncidentHistoryService(
            IncidentAnalysisRepository repository,
            IncidentHistoryProperties historyProperties,
            IncidentAnalysisMapper mapper
    ) {
        this.repository = repository;
        this.historyProperties = historyProperties;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<IncidentAnalysisListItemResponse> findHistory(
            Integer page,
            Integer size,
            IncidentCategory category,
            IncidentSeverity severity,
            String service,
            String provider,
            AnalysisStatus status,
            Instant createdFrom,
            Instant createdTo
    ) {
        int pageNumber = page == null || page < 0 ? 0 : page;
        int pageSize = size == null || size <= 0
                ? historyProperties.getDefaultPageSize()
                : Math.min(size, historyProperties.getMaxPageSize());

        var result = repository.findFiltered(
                category,
                severity,
                status,
                blankToNull(service),
                blankToNull(provider),
                createdFrom,
                createdTo,
                PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<IncidentAnalysisListItemResponse> items = result.getContent().stream()
                .map(mapper::toListItem)
                .toList();

        return new PageResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public IncidentAnalysisDetailResponse getById(UUID analysisId) {
        return repository.findById(analysisId)
                .map(entity -> mapper.toDetail(entity, List.of(), List.of()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
