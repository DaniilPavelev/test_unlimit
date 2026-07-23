package daniil.pavelev.service;

import org.springframework.stereotype.Service;

@Service
public class InputNormalizerService {

    public String normalize(String description) {
        if (description == null) {
            return "";
        }
        return description.trim().replaceAll("\\s+", " ");
    }
}
