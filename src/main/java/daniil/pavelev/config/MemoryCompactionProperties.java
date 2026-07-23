package daniil.pavelev.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "memory.compaction")
public class MemoryCompactionProperties {

    private boolean enabled = true;
    private int batchSize = 50;
    private int minimumPending = 10;
    private String cron = "0 0 2 * * *";
    private int maxSourceItemsPerRun = 200;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMinimumPending() {
        return minimumPending;
    }

    public void setMinimumPending(int minimumPending) {
        this.minimumPending = minimumPending;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public int getMaxSourceItemsPerRun() {
        return maxSourceItemsPerRun;
    }

    public void setMaxSourceItemsPerRun(int maxSourceItemsPerRun) {
        this.maxSourceItemsPerRun = maxSourceItemsPerRun;
    }
}
