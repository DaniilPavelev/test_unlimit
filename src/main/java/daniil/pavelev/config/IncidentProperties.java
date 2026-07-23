package daniil.pavelev.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "incident")
@Getter
public class IncidentProperties {

    private final History history = new History();
    private final Memory memory = new Memory();
    private final Prompts prompts = new Prompts();
    private List<String> knowledge = new ArrayList<>();

    public void setKnowledge(List<String> knowledge) {
        this.knowledge = knowledge != null ? knowledge : new ArrayList<>();
    }

    @Getter
    @Setter
    public static class History {
        private int maxItems = 100;
    }

    @Getter
    @Setter
    public static class Memory {
        private int maxSelectedItems = 3;
        private int maxContextCharacters = 4000;
    }

    @Getter
    @Setter
    public static class Prompts {
        private String analysisSystem;
        private String repairSystem;
        private String repairUser;
        private String knowledgeHeader;
        private String historyHeader;
        private String signalsSection;
        private String incidentSection;
        private String noneItem;
        private String historyItem;
    }
}
