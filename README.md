# Incident analysis with persistent history and bounded LLM memory

Spring Boot application that analyzes incidents with an LLM while storing the **complete** analysis history in PostgreSQL. The LLM never receives the full history: only a small, relevance-ranked, budget-limited memory slice is included in each prompt.

## Stack

- Java 25
- Spring Boot 4.1.0
- Spring Data JPA
- PostgreSQL
- Flyway
- Testcontainers (repository / integration tests)

## What is stored

Each analysis is persisted as `IncidentAnalysisEntity` with:

- identifiers and timestamps (`id`, `createdAt`, `completedAt`)
- `originalDescription` / `normalizedDescription` (credentials redacted during normalization)
- `category`, `summary`, `severity`, `status`
- LLM metadata: `model`, `llmAttempts`, token counts, `processingDurationMs`, `errorCode`
- ordered hypotheses and next steps
- extracted signals: mentioned services, provider names, HTTP status codes, keywords, affected functionality

**Not stored:** API keys, authorization headers, or raw provider credentials. Matching credential-like patterns are redacted to `[REDACTED]` before persistence and prompting.

Original analysis rows are retained indefinitely by the application (no automatic deletion). Aggregate compaction **never** deletes or replaces them.

## Retention behavior

- Full incident analyses remain in `incident_analyses` (and related collections) as the system of record.
- Compaction marks source rows as `compacted=true` and writes a separate `aggregate_memories` row.
- Compaction is additive long-term memory, not archival deletion.
- Operators remain responsible for legal/privacy retention policies outside this service.

## History endpoints

### `POST /api/v1/incident-analyses`

Creates a new analysis: normalize → extract signals → retrieve bounded memory → call LLM → persist complete result.

### `GET /api/v1/incident-analyses`

Paginated history. Query parameters:

| Parameter | Description |
|-----------|-------------|
| `page` | Zero-based page index |
| `size` | Page size (capped by `incident.history.max-page-size`, default max 100) |
| `category` | Filter |
| `severity` | Filter |
| `service` | Filter by mentioned service |
| `provider` | Filter by provider name |
| `status` | Filter |
| `createdFrom` / `createdTo` | ISO-8601 timestamps |

Response shape:

```json
{
  "items": [ /* list items without full hypotheses */ ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

List items intentionally omit full hypotheses; use the detail endpoint for the complete analysis.

### `GET /api/v1/incident-analyses/{analysisId}`

Returns the complete analysis, including hypotheses, next steps, and signals.

### `GET /api/v1/incident-statistics`

Returns **deterministic** database-calculated statistics (no LLM):

- total / completed analyses
- counts by category and severity
- most frequently mentioned services and providers
- analyses over time (daily buckets)

## Relevance algorithm

`IncidentMemoryRetriever` implements `MemoryRetrievalStrategy` (swap-friendly for future embeddings / pgvector).

For each new incident:

1. Normalize input and redact secrets
2. Extract deterministic signals
3. Load completed analyses in the lookback window
4. Score candidates with explainable weights:
   - same mentioned service: **+5**
   - same provider: **+4**
   - same category: **+3**
   - same HTTP status: **+2**
   - shared keyword: **+1**
   - recent incident bonus: configurable (`memory.retrieval.recent-bonus`)
5. Keep at most `memory.retrieval.max-incidents` (default **5**)
6. Select at most `memory.retrieval.max-aggregate-memories` (default **2**) aggregate memories
7. Enforce `memory.retrieval.max-context-characters` (default **6000**)
8. Pass only compact entries to the LLM

A compact memory entry contains only: category, summary, severity, matching services/providers, and useful diagnostic steps.

## Context budget

When the estimated context size exceeds the budget, reduction is applied in order:

1. Drop lowest-ranked historical incidents
2. Drop lower-ranked aggregate memories
3. Shorten summaries
4. Remove lower-ranked diagnostic steps

Size is estimated deterministically from serialized JSON length (no tokenizer required). Entries are rewritten as whole objects so JSON is never truncated into invalid fragments.

## Why full history is not sent to the LLM

- Cost and latency grow with prompt size
- Noise from unrelated incidents degrades analysis quality
- Privacy: minimize data exposure to the model
- Controllability: retrieval scores and budgets are inspectable

The final LLM context may include only:

- the current incident
- small static system knowledge
- up to five similar historical analyses
- up to two relevant aggregate memories

## Compaction

Configuration:

```properties
memory.compaction.enabled=true
memory.compaction.batch-size=50
memory.compaction.minimum-pending=10
memory.compaction.cron=0 0 2 * * *
memory.compaction.max-source-items-per-run=200
```

A run starts only when at least `minimum-pending` uncompacted completed analyses exist after the durable checkpoint.

- Numeric aggregates (category/severity/service/provider counts) are computed in Java
- The LLM only summarizes textual patterns (recurring causes / diagnostics)
- Source analyses are never deleted
- Failed runs roll back and leave sources unmarked for retry
- Scheduled compaction does not block user analysis requests
- A DB row lock prevents concurrent compaction on **one** application instance

### Multi-instance limitation

The current lock is adequate for a single instance. Multiple instances require a **distributed lock** (e.g. ShedLock, Redis, or PostgreSQL advisory locks coordinated across nodes).

## Deterministic statistics vs LLM text

| Concern | Owner |
|---------|--------|
| Counts, rankings, time series | SQL / Java only |
| Incident summary / hypotheses / next steps | LLM |
| Aggregate recurring textual patterns | LLM compaction (text only) |
| Aggregate numeric counts | Java only |

## Vector search

This release uses weighted lexical/signal matching only. There is **no** vector database.

A future `MemoryRetrievalStrategy` implementation can add pgvector embeddings while keeping the same compact-entry contract and context budget.

## Privacy and data retention

- Redact secrets before store/prompt
- Prefer summaries over raw customer payloads when possible
- Full history remains in PostgreSQL; apply org retention/deletion policies separately
- Limit LLM exposure via retrieval caps and context budgets

## Configuration (retrieval)

```properties
memory.retrieval.max-incidents=5
memory.retrieval.max-aggregate-memories=2
memory.retrieval.max-context-characters=6000
memory.retrieval.lookback-days=180
memory.retrieval.recent-bonus=1.0
memory.retrieval.recent-days=14
```

## Failure behavior

| Failure | Behavior |
|---------|----------|
| Persist completed analysis fails | HTTP 500; do not pretend storage succeeded |
| Memory retrieval fails | Log and continue without historical memory |
| Compaction fails | Preserve sources; retry later |
| After compaction | Sources remain; only `compacted` flag + aggregate row added |

## Running tests

Integration tests use Testcontainers PostgreSQL. Docker must be available.

```bash
./gradlew test
```

Unit tests (scoring / budget) do not call a real LLM. The default `llm.mode=stub` client is deterministic and offline.
