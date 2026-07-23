package daniil.pavelev.incident.persistence;

import daniil.pavelev.incident.domain.CompactionCheckpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompactionCheckpointRepository extends JpaRepository<CompactionCheckpointEntity, Integer> {
}
