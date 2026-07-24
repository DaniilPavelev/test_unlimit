## How to run

Requirements: **Java 25**, an OpenAI-compatible API key.

```bash
LLM_API_KEY=sk-... ./gradlew bootRun
```

Optional:

```bash
LLM_BASE_URL=https://api.openai.com   # default
LLM_MODEL=gpt-4o-mini                 # default
```

Then open http://localhost:8080/ (UI) or call:

```bash
curl -s -X POST http://localhost:8080/api/v1/incident-analyses \
  -H 'Content-Type: application/json' \
  -d '{"description":"Customers cannot pay by card. payment-service logs show PayGate timeouts."}'
```

No database setup is required (history is in-memory).

## How the agent is structured

The HTTP layer only accepts the description and returns a DTO. 
Orchestrator pipeline:

1. Normalize the text and extract deterministic signals (services, providers, HTTP statuses, symptom indicators).
2. Select relevant static system knowledge and a few similar previous analyses from the bounded in-memory history.
3. Build a prompt that separates trusted context from the untrusted incident text.
4. Call the LLM (Spring AI `ChatClient`), parse JSON, and validate schema/business rules.
5. If the output is invalid, retry with a repair prompt up to `incident.llm.max-attempts` total calls (default `2` = one analysis + one repair); then persist the result and return it.

Controllers never build prompts or talk to the model directly.

## How it was tested

Automated tests (`./gradlew test`) cover API validation, signal extraction, JSON parse/validate, and repair-after-invalid-LLM using an offline stub client (no real model calls).

Manual checks against a real LLM were run with **qwen3/6-27b** (reasoning disabled).

Manual / expected behavior for sample incidents:

| # | Incident input (summary) | Expected kind of output |
|---|--------------------------|-------------------------|
| 1 | Card payments fail; `payment-service` logs show **PayGate timeouts** | Category about external payment provider; severity HIGH/MEDIUM; hypotheses about PayGate / timeout config; next steps like check provider status and payment-service timeouts |
| 2 | Mobile login fails; `auth-service` returns **HTTP 401** with invalid token signature | Category about authentication; severity MEDIUM; hypotheses about signing-key mismatch / auth deploy; next steps around keys and recent auth releases |
| 3 | Top-up emails missing; balances OK; `notification-service` **SMTP** connection errors | Category about notification delivery; severity usually MEDIUM; hypotheses about SMTP provider; next steps around SMTP health and notification-service logs |
| 4 | `/payments/create` slow; high DB CPU from `reporting-service`; some clients get **HTTP 504** from `api-gateway` | Category about DB / reporting load; hypotheses tying latency/504 to reporting queries; next steps around DB load and reporting jobs |
| 5 | Vague text with almost no service/provider signals | Lower confidence / generic category; summary should admit limited evidence; hypotheses should stay cautious and ask for more data |

## Trade-offs

### What was simplified
- History is in-memory only (no PostgreSQL / migrations).
- Similar-case retrieval uses deterministic signals and weighted scoring, not embeddings.
- System knowledge is a static list in `application.yml`.
- Past-incident context comes from accumulated history after analyses run (no separate seed dataset); cold start has empty history by design.
- LLM repair is a small configurable retry loop (`incident.llm.max-attempts`), not a full eval / prompt-versioning setup.
- Minimal UI and API: no auth, pagination, metrics, or live ELK / monitoring integrations.
- Context budget is approximate (character-based), not exact token counting.

### What I would do differently with more time
- Persist history and improve retrieval (e.g. PostgreSQL + embeddings / pgvector).
- Bootstrap with the sample past incidents from the brief, then blend them with live history.
- API pagination/filters, auth, and a clearer error contract for clients.
- An evaluation set of incidents and prompt/model comparison.
- Exact token budget for context packing.
- Observability for latency, attempt counts, and validation failures.