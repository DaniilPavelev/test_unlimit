package daniil.pavelev.incident.memory;

import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.signal.ExtractedSignals;

/**
 * Retrieval strategy abstraction. The default implementation uses explainable weighted matching.
 * Future implementations may use embeddings / pgvector without changing callers.
 */
public interface MemoryRetrievalStrategy {

    MemoryRetrievalResult retrieve(ExtractedSignals signals, IncidentCategory inferredCategory);
}
