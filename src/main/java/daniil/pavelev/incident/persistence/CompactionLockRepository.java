package daniil.pavelev.incident.persistence;

import daniil.pavelev.incident.domain.CompactionLockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface CompactionLockRepository extends JpaRepository<CompactionLockEntity, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE compaction_locks
            SET locked_until = :until, locked_by = :owner
            WHERE lock_name = :name
              AND (locked_until IS NULL OR locked_until < :now)
            """, nativeQuery = true)
    int tryAcquire(
            @Param("name") String name,
            @Param("owner") String owner,
            @Param("until") Instant until,
            @Param("now") Instant now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE compaction_locks
            SET locked_until = NULL, locked_by = NULL
            WHERE lock_name = :name AND locked_by = :owner
            """, nativeQuery = true)
    int release(@Param("name") String name, @Param("owner") String owner);
}
