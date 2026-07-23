package daniil.pavelev.api;

import daniil.pavelev.api.dto.CreateIncidentAnalysisRequest;
import daniil.pavelev.api.dto.IncidentAnalysisListResponse;
import daniil.pavelev.api.dto.IncidentAnalysisResponse;
import daniil.pavelev.application.IncidentAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incident-analyses")
@RequiredArgsConstructor
public class IncidentAnalysisController {

    private final IncidentAnalysisService service;

    @PostMapping
    public ResponseEntity<IncidentAnalysisResponse> create(@Valid @RequestBody CreateIncidentAnalysisRequest request) {
        IncidentAnalysisResponse body = service.create(request.description());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/incident-analyses/" + body.id()))
                .body(body);
    }

    @GetMapping("/{analysisId}")
    public IncidentAnalysisResponse get(@PathVariable UUID analysisId) {
        return service.get(analysisId);
    }

    @GetMapping
    public IncidentAnalysisListResponse list() {
        return service.list();
    }
}
