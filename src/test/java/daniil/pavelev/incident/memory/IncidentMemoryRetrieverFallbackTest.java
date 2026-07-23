package daniil.pavelev.incident.memory;

import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.signal.ExtractedSignals;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IncidentMemoryRetrieverFallbackTest {

    @Test
    void retrieveSafelyReturnsEmptyWhenStrategyFails() {
        IncidentMemoryRetriever retriever = mock(IncidentMemoryRetriever.class);
        when(retriever.retrieve(any(), any())).thenThrow(new RuntimeException("db down"));
        when(retriever.retrieveSafely(any(), any())).thenCallRealMethod();

        MemoryRetrievalResult result = retriever.retrieveSafely(
                ExtractedSignals.empty(),
                IncidentCategory.UNKNOWN
        );

        assertThat(result.selectedIncidents()).isEmpty();
        assertThat(result.selectedAnalysisIds()).isEmpty();
        assertThat(result.estimatedContextCharacters()).isZero();
    }

    @Test
    void emptySignalsProduceEmptyMemoryContract() {
        assertThat(ExtractedSignals.empty().mentionedServices()).isEmpty();
        assertThat(new ExtractedSignals(Set.of(), Set.of(), Set.of(), Set.of(), Set.of())
                .keywords()).isEmpty();
    }
}
