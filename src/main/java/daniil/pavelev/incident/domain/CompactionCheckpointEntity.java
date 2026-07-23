package daniil.pavelev.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "compaction_checkpoints")
public class CompactionCheckpointEntity {

    @Id
    private Integer id = 1;

    @Column(name = "last_compacted_created_at")
    private Instant lastCompactedCreatedAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "last_source_count", nullable = false)
    private int lastSourceCount;

    protected CompactionCheckpointEntity() {
    }

    public CompactionCheckpointEntity(Integer id) {
        this.id = id;
        this.lastSourceCount = 0;
    }

    public Integer getId() {
        return id;
    }

    public Instant getLastCompactedCreatedAt() {
        return lastCompactedCreatedAt;
    }

    public void setLastCompactedCreatedAt(Instant lastCompactedCreatedAt) {
        this.lastCompactedCreatedAt = lastCompactedCreatedAt;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public int getLastSourceCount() {
        return lastSourceCount;
    }

    public void setLastSourceCount(int lastSourceCount) {
        this.lastSourceCount = lastSourceCount;
    }

    public void reset() {
        this.lastCompactedCreatedAt = null;
        this.lastRunAt = null;
        this.lastSourceCount = 0;
    }
}
