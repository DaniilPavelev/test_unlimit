package daniil.pavelev.repository;

import daniil.pavelev.config.IncidentProperties;
import daniil.pavelev.domain.IncidentAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Thread-safe bounded in-memory history. Lost on restart by design.
 * LinkedHashMap is used only to evict the oldest inserted entry when the limit is exceeded.
 */
@Component
@RequiredArgsConstructor
public class IncidentRepository {

    private final IncidentProperties properties;
    private volatile Map<UUID, IncidentAnalysis> analyses;

    public void save(IncidentAnalysis analysis) {
        store().put(analysis.id(), analysis);
    }

    public Optional<IncidentAnalysis> findById(UUID id) {
        return Optional.ofNullable(store().get(id));
    }

    public List<IncidentAnalysis> findAll() {
        return List.copyOf(store().values());
    }

    public int size() {
        return store().size();
    }

    private Map<UUID, IncidentAnalysis> store() {
        Map<UUID, IncidentAnalysis> local = analyses;
        if (local == null) {
            synchronized (this) {
                local = analyses;
                if (local == null) {
                    int maxItems = properties.getHistory().getMaxItems();
                    local = Collections.synchronizedMap(new LinkedHashMap<>() {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<UUID, IncidentAnalysis> eldest) {
                            return size() > maxItems;
                        }
                    });
                    analyses = local;
                }
            }
        }
        return local;
    }
}
