package daniil.pavelev.incident.persistence;

import daniil.pavelev.incident.domain.AnalysisStatus;
import daniil.pavelev.incident.domain.IncidentAnalysisEntity;
import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.domain.IncidentSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IncidentAnalysisRepository extends JpaRepository<IncidentAnalysisEntity, UUID> {

    @Query("""
            SELECT DISTINCT a FROM IncidentAnalysisEntity a
            LEFT JOIN a.mentionedServices s
            LEFT JOIN a.providerNames p
            WHERE (:category IS NULL OR a.category = :category)
              AND (:severity IS NULL OR a.severity = :severity)
              AND (:status IS NULL OR a.status = :status)
              AND (:service IS NULL OR LOWER(s) = LOWER(:service))
              AND (:provider IS NULL OR LOWER(p) = LOWER(:provider))
              AND (:createdFrom IS NULL OR a.createdAt >= :createdFrom)
              AND (:createdTo IS NULL OR a.createdAt <= :createdTo)
            """)
    Page<IncidentAnalysisEntity> findFiltered(
            @Param("category") IncidentCategory category,
            @Param("severity") IncidentSeverity severity,
            @Param("status") AnalysisStatus status,
            @Param("service") String service,
            @Param("provider") String provider,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo") Instant createdTo,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM IncidentAnalysisEntity a
            WHERE a.status = daniil.pavelev.incident.domain.AnalysisStatus.COMPLETED
              AND a.createdAt >= :from
              AND a.createdAt < :to
            ORDER BY a.createdAt DESC
            """)
    List<IncidentAnalysisEntity> findCompletedBetween(
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT COUNT(a) FROM IncidentAnalysisEntity a
            WHERE a.status = daniil.pavelev.incident.domain.AnalysisStatus.COMPLETED
              AND a.compacted = false
              AND (:after IS NULL OR a.createdAt > :after)
            """)
    long countUncompactedCompleted(@Param("after") Instant after);

    @Query("""
            SELECT a FROM IncidentAnalysisEntity a
            WHERE a.status = daniil.pavelev.incident.domain.AnalysisStatus.COMPLETED
              AND a.compacted = false
              AND (:after IS NULL OR a.createdAt > :after)
            ORDER BY a.createdAt ASC
            """)
    List<IncidentAnalysisEntity> findUncompactedCompleted(
            @Param("after") Instant after,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE IncidentAnalysisEntity a
            SET a.compacted = true
            WHERE a.id IN :ids
            """)
    int markCompacted(@Param("ids") List<UUID> ids);

    long countByStatus(AnalysisStatus status);

    @Query("""
            SELECT a.category, COUNT(a) FROM IncidentAnalysisEntity a
            WHERE a.status = daniil.pavelev.incident.domain.AnalysisStatus.COMPLETED
              AND a.category IS NOT NULL
            GROUP BY a.category
            """)
    List<Object[]> countByCategory();

    @Query("""
            SELECT a.severity, COUNT(a) FROM IncidentAnalysisEntity a
            WHERE a.status = daniil.pavelev.incident.domain.AnalysisStatus.COMPLETED
              AND a.severity IS NOT NULL
            GROUP BY a.severity
            """)
    List<Object[]> countBySeverity();

    @Query(value = """
            SELECT service_name, COUNT(*) AS cnt
            FROM incident_mentioned_services s
            JOIN incident_analyses a ON a.id = s.analysis_id
            WHERE a.status = 'COMPLETED'
            GROUP BY service_name
            ORDER BY cnt DESC, service_name ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> mostFrequentServices(@Param("limit") int limit);

    @Query(value = """
            SELECT provider_name, COUNT(*) AS cnt
            FROM incident_provider_names p
            JOIN incident_analyses a ON a.id = p.analysis_id
            WHERE a.status = 'COMPLETED'
            GROUP BY provider_name
            ORDER BY cnt DESC, provider_name ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> mostFrequentProviders(@Param("limit") int limit);

    @Query(value = """
            SELECT CAST(DATE_TRUNC('day', created_at) AS DATE) AS day, COUNT(*) AS cnt
            FROM incident_analyses
            WHERE status = 'COMPLETED'
            GROUP BY day
            ORDER BY day ASC
            """, nativeQuery = true)
    List<Object[]> analysesOverTimeByDay();
}
