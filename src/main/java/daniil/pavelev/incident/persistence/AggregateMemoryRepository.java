package daniil.pavelev.incident.persistence;

import daniil.pavelev.incident.domain.AggregateMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AggregateMemoryRepository extends JpaRepository<AggregateMemoryEntity, UUID> {

    @Query("""
            SELECT a FROM AggregateMemoryEntity a
            ORDER BY a.createdAt DESC
            """)
    List<AggregateMemoryEntity> findAllNewestFirst();
}
