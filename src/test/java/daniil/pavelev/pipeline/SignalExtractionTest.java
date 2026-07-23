package daniil.pavelev.pipeline;

import daniil.pavelev.domain.IncidentSignals;
import daniil.pavelev.service.SignalExtractorService;
import daniil.pavelev.support.StubLlmClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(StubLlmClientConfig.class)
class SignalExtractionTest {

    @Autowired
    private SignalExtractorService signalExtractor;

    @Test
    void extractsPayGateTimeoutSignals() {
        IncidentSignals signals = signalExtractor.extract(
                "Customers cannot pay by card. payment-service logs show many timeouts while calling PayGate."
        );
        assertThat(signals.mentionedServices()).contains("payment-service");
        assertThat(signals.mentionedProviders()).contains("paygate");
        assertThat(signals.indicators()).contains("timeout", "payment");
    }

    @Test
    void extractsAuthenticationSignals() {
        IncidentSignals signals = signalExtractor.extract(
                "Some customers cannot log in. auth-service returns HTTP 401. Logs contain invalid token signature errors."
        );
        assertThat(signals.mentionedServices()).contains("auth-service");
        assertThat(signals.httpStatusCodes()).contains(401);
        assertThat(signals.indicators()).contains("authentication", "token");
    }
}
