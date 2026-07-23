package daniil.pavelev.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "memory.retrieval")
public class MemoryRetrievalProperties {

    private int maxIncidents = 5;
    private int maxAggregateMemories = 2;
    private int maxContextCharacters = 6000;
    private int lookbackDays = 180;
    private double recentBonus = 1.0;
    private int recentDays = 14;

    public int getMaxIncidents() {
        return maxIncidents;
    }

    public void setMaxIncidents(int maxIncidents) {
        this.maxIncidents = maxIncidents;
    }

    public int getMaxAggregateMemories() {
        return maxAggregateMemories;
    }

    public void setMaxAggregateMemories(int maxAggregateMemories) {
        this.maxAggregateMemories = maxAggregateMemories;
    }

    public int getMaxContextCharacters() {
        return maxContextCharacters;
    }

    public void setMaxContextCharacters(int maxContextCharacters) {
        this.maxContextCharacters = maxContextCharacters;
    }

    public int getLookbackDays() {
        return lookbackDays;
    }

    public void setLookbackDays(int lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    public double getRecentBonus() {
        return recentBonus;
    }

    public void setRecentBonus(double recentBonus) {
        this.recentBonus = recentBonus;
    }

    public int getRecentDays() {
        return recentDays;
    }

    public void setRecentDays(int recentDays) {
        this.recentDays = recentDays;
    }
}
