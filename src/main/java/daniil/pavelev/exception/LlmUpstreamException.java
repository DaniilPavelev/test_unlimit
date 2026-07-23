package daniil.pavelev.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class LlmUpstreamException extends RuntimeException {

    public LlmUpstreamException(String message) {
        super(message);
    }

    public LlmUpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
