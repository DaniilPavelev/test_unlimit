package daniil.pavelev.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AnalysisNotFoundException extends RuntimeException {

    public AnalysisNotFoundException(String message) {
        super(message);
    }
}
