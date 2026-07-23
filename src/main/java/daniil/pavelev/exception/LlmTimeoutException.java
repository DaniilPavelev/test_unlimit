package daniil.pavelev.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
public class LlmTimeoutException extends RuntimeException {

    public LlmTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
