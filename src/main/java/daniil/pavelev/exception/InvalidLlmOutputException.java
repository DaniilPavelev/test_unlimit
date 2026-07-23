package daniil.pavelev.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Getter
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidLlmOutputException extends RuntimeException {

    private final List<String> validationErrors;

    public InvalidLlmOutputException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = List.copyOf(validationErrors);
    }
}
