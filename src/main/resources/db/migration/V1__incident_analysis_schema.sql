CREATE TABLE incident_analyses (
    id                      UUID PRIMARY KEY,
    original_description    TEXT NOT NULL,
    normalized_description  TEXT NOT NULL,
    category                VARCHAR(64),
    summary                 TEXT,
    severity                VARCHAR(32),
    status                  VARCHAR(32) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL,
    completed_at            TIMESTAMPTZ,
    model                   VARCHAR(128),
    llm_attempts            INTEGER,
    prompt_tokens           INTEGER,
    completion_tokens       INTEGER,
    total_tokens            INTEGER,
    processing_duration_ms  BIGINT,
    error_code              VARCHAR(64),
    compacted               BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_incident_analyses_created_at ON incident_analyses (created_at);
CREATE INDEX idx_incident_analyses_category ON incident_analyses (category);
CREATE INDEX idx_incident_analyses_severity ON incident_analyses (severity);
CREATE INDEX idx_incident_analyses_status ON incident_analyses (status);
CREATE INDEX idx_incident_analyses_compacted_status ON incident_analyses (compacted, status, created_at);

CREATE TABLE incident_hypotheses (
    analysis_id UUID NOT NULL REFERENCES incident_analyses (id) ON DELETE CASCADE,
    position    INTEGER NOT NULL,
    text        TEXT NOT NULL,
    PRIMARY KEY (analysis_id, position)
);

CREATE TABLE incident_next_steps (
    analysis_id UUID NOT NULL REFERENCES incident_analyses (id) ON DELETE CASCADE,
    position    INTEGER NOT NULL,
    text        TEXT NOT NULL,
    PRIMARY KEY (analysis_id, position)
);

CREATE TABLE incident_mentioned_services (
    analysis_id  UUID NOT NULL REFERENCES incident_analyses (id) ON DELETE CASCADE,
    service_name VARCHAR(256) NOT NULL,
    PRIMARY KEY (analysis_id, service_name)
);

CREATE INDEX idx_incident_mentioned_services_name ON incident_mentioned_services (service_name);

CREATE TABLE incident_provider_names (
    analysis_id   UUID NOT NULL REFERENCES incident_analyses (id) ON DELETE CASCADE,
    provider_name VARCHAR(256) NOT NULL,
    PRIMARY KEY (analysis_id, provider_name)
);

CREATE INDEX idx_incident_provider_names_name ON incident_provider_names (provider_name);

CREATE TABLE incident_http_status_codes (
    analysis_id UUID NOT NULL REFERENCES incident_analyses (id) ON DELETE CASCADE,
    status_code INTEGER NOT NULL,
    PRIMARY KEY (analysis_id, status_code)
);

CREATE TABLE incident_keywords (
    analysis_id UUID NOT NULL REFERENCES incident_analyses (id) ON DELETE CASCADE,
    keyword     VARCHAR(256) NOT NULL,
    PRIMARY KEY (analysis_id, keyword)
);

CREATE TABLE incident_affected_functionality (
    analysis_id   UUID NOT NULL REFERENCES incident_analyses (id) ON DELETE CASCADE,
    functionality VARCHAR(512) NOT NULL,
    PRIMARY KEY (analysis_id, functionality)
);

CREATE TABLE aggregate_memories (
    id                            UUID PRIMARY KEY,
    period_start                  TIMESTAMPTZ NOT NULL,
    period_end                    TIMESTAMPTZ NOT NULL,
    source_analysis_count         INTEGER NOT NULL,
    category_counts_json          TEXT NOT NULL,
    severity_counts_json          TEXT NOT NULL,
    frequent_services_json        TEXT NOT NULL,
    frequent_providers_json       TEXT NOT NULL,
    recurring_patterns            TEXT,
    effective_diagnostic_patterns TEXT,
    created_at                    TIMESTAMPTZ NOT NULL,
    compaction_model              VARCHAR(128),
    source_checkpoint             BIGINT NOT NULL
);

CREATE INDEX idx_aggregate_memories_period ON aggregate_memories (period_start, period_end);
CREATE INDEX idx_aggregate_memories_created_at ON aggregate_memories (created_at);

CREATE TABLE compaction_checkpoints (
    id                         INTEGER PRIMARY KEY,
    last_compacted_created_at  TIMESTAMPTZ,
    last_run_at                TIMESTAMPTZ,
    last_source_count          INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT compaction_checkpoints_singleton CHECK (id = 1)
);

INSERT INTO compaction_checkpoints (id, last_source_count) VALUES (1, 0);

CREATE TABLE compaction_locks (
    lock_name   VARCHAR(64) PRIMARY KEY,
    locked_until TIMESTAMPTZ,
    locked_by   VARCHAR(128)
);

INSERT INTO compaction_locks (lock_name, locked_until, locked_by) VALUES ('memory-compaction', NULL, NULL);
